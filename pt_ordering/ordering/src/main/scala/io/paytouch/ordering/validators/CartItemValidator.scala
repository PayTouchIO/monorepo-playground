package io.paytouch.ordering.validators

import java.util.UUID

import scala.concurrent._

import cats.data._
import cats.implicits._

import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.clients.paytouch.core.PtCoreClient
import io.paytouch.ordering.data.daos.{ CartItemDao, Daos }
import io.paytouch.ordering.data.model.CartItemRecord
import io.paytouch.ordering.entities._
import io.paytouch.ordering.errors._
import io.paytouch.ordering.utils.validation.{ ValidatedData, ValidatedOptData }
import io.paytouch.ordering.utils.validation.ValidatedData._
import io.paytouch.ordering.utils.validation.ValidatedOptData.ValidatedOptData
import io.paytouch.ordering.validators.features.{ DefaultStoreValidator, PtCoreValidator }

class CartItemValidator(val ptCoreClient: PtCoreClient)(implicit val ec: ExecutionContext, val daos: Daos)
    extends DefaultStoreValidator
       with PtCoreValidator {
  type Dao = CartItemDao
  type Entity = CartItem
  type Record = CartItemRecord
  type Upsertion = CartItemUpsertion

  protected val dao = daos.cartItemDao

  val validationErrorF = InvalidCartItemIds(_)
  val accessErrorF = NonAccessibleCartItemIds(_)

  def validateCartItemId(cart: Cart, cartItemId: UUID)(implicit store: StoreContext): Future[ValidatedData[CartItem]] =
    Future.successful {
      cart.items.find(_.id === cartItemId) match {
        case Some(ci) => ValidatedData.success(ci)
        case _        => ValidatedData.failure(InvalidCartItemIds(Seq(cartItemId)))
      }
    }

  def validateUpsertion(
      targetProductId: UUID,
      upsertion: CartItemUpsertion,
    )(implicit
      store: StoreContext,
    ): Future[ValidatedData[ValidCartItemUpsertion]] =
    if (upsertion.productId.isDefined || upsertion.modifierOptions.isDefined || upsertion.bundleSets.isDefined) {
      val productId = upsertion.productId.getOrElse(targetProductId)

      validateProductAndModifiers(
        productId,
        upsertion,
        upsertion.modifierOptions,
        upsertion.bundleSets,
      )
    }
    else
      ValidatedData.success(ValidCartItemUpsertion(upsertion)).pure[Future]

  private def validateProductAndModifiers(
      productId: UUID,
      upsertion: CartItemUpsertion,
      modifierOptions: Option[Seq[CartItemModifierOptionUpsertion]],
      bundleSets: Option[Seq[CartItemBundleSetUpsertion]],
    )(implicit
      store: StoreContext,
    ): Future[ValidatedData[ValidCartItemUpsertion]] =
    validateProduct(productId).flatMap {
      case Validated.Valid(product) =>
        prefetchCoreBundledProducts(product, bundleSets).map {
          case Validated.Valid(coreBundledProducts) =>
            val validModifierOptions =
              modifierOptions match {
                case Some(mo) =>
                  validateModifierOptions(mo, product).map(Some.apply)

                case _ =>
                  ValidatedData.success(None)
              }

            val validBundleSets =
              validateBundleSets(bundleSets, product, coreBundledProducts)

            val validStock =
              validateStock(product, upsertion.quantity)

            ValidatedData.combine(validBundleSets, validModifierOptions, validStock) {
              case (vBundleSets, vModifierOptions, _) =>
                ValidCartItemUpsertion(
                  upsertion = upsertion,
                  coreData = Some(product),
                  modifierOptions = vModifierOptions,
                  bundleSets = Some(vBundleSets),
                )
            }

          case i @ Validated.Invalid(_) =>
            i
        }

      case i @ Validated.Invalid(_) =>
        Future.successful(i)
    }

  private def prefetchCoreBundledProducts(
      product: Product,
      bundleSets: Option[Seq[CartItemBundleSetUpsertion]],
    )(implicit
      store: StoreContext,
    ): Future[ValidatedData[Seq[Product]]] =
    validateProducts {
      val upsertedOptionIds: Seq[UUID] =
        bundleSets
          .map(_.flatMap(_.bundleOptions.map(_.bundleOptionId)))
          .getOrElse(Seq.empty)
          .distinct

      val bundleOptionIdToProductIdMap: Map[UUID, UUID] =
        product
          .bundleSets
          .flatMap(_.options.map(o => o.id -> o.article.id))
          .toMap

      upsertedOptionIds.flatMap(bundleOptionIdToProductIdMap.get)
    }

  private def validateBundleSets(
      maybeBundleSets: Option[Seq[CartItemBundleSetUpsertion]],
      product: Product,
      coreBundledProducts: Seq[Product],
    )(implicit
      store: StoreContext,
    ): ValidatedData[Seq[ValidCartItemBundleSetUpsertion]] = {
    val isBundle = product.isCombo && product.bundleSets.nonEmpty
    val upsertedBundleSets = maybeBundleSets.getOrElse(Seq.empty)
    val bundleMetadataUpserted = maybeBundleSets.isDefined && upsertedBundleSets.nonEmpty
    val upsertedBundleSetIds = upsertedBundleSets.map(_.bundleSetId)

    (isBundle, bundleMetadataUpserted) match {
      case (false, false) =>
        ValidatedData.success(Seq.empty)

      case (false, true) =>
        ValidatedData.failure(BundleMetadataForNonBundleProduct(product.id))

      case (true, false) =>
        ValidatedData.failure(MissingBundleMetadataForBundleProduct(product.id))

      case (true, true) if upsertedBundleSetIds.length != upsertedBundleSetIds.distinct.length =>
        ValidatedData.failure(DuplicatedBundleMetadataForBundleProduct(product.id))

      case (true, true) =>
        ValidatedData.sequence(product.bundleSets.zipWithIndex.map {
          case (set, index) =>
            validateBundleSet(upsertedBundleSets, set, coreBundledProducts, index)
        })
    }
  }

  private def validateBundleSet(
      upsertedBundleSets: Seq[CartItemBundleSetUpsertion],
      coreBundleSet: BundleSet,
      coreBundledProducts: Seq[Product],
      setIndex: Int,
    )(implicit
      store: StoreContext,
    ): ValidatedData[ValidCartItemBundleSetUpsertion] = {
    val upsertedSet = upsertedBundleSets.find(_.bundleSetId == coreBundleSet.id)
    val upsertedOptions = upsertedSet.map(_.bundleOptions).getOrElse(Seq.empty)
    val upsertedOptionsQuantity = upsertedOptions.map(_.quantity).sum

    val respectsMinQuantity =
      (upsertedOptionsQuantity >= coreBundleSet.minQuantity).toValidated(NotEnoughOptionsForBundleSet(coreBundleSet.id))
    val respectsMaxQuantity =
      (upsertedOptionsQuantity <= coreBundleSet.maxQuantity).toValidated(TooManyOptionsForBundleSet(coreBundleSet.id))

    val allValidatedOptions =
      upsertedOptions.map { bundleOption =>
        val maybeCoreOption = coreBundleSet.options.find(_.id == bundleOption.bundleOptionId)
        val validationOptionFailure = ValidatedData.failure(
          InvalidBundleOptionIdForBundleSet(
            coreBundleSet.id,
            bundleOption.bundleOptionId,
            coreBundleSet.options.map(_.id),
          ),
        )
        val validatedCoreOption =
          maybeCoreOption.fold[ValidatedData[(CartItemBundleOptionUpsertion, BundleOption)]](validationOptionFailure)(
            validCoreOption => ValidatedData.success((bundleOption, validCoreOption)),
          )

        validatedCoreOption match {
          case Validated.Valid((bundleOption, validCoreOption)) =>
            val maybeCoreProduct = coreBundledProducts.find(_.id == validCoreOption.article.id)
            val validationProductFailure =
              ValidatedData.failure(
                MissingProductForBundleOption(validCoreOption.article.id, bundleOption.bundleOptionId),
              )
            maybeCoreProduct
              .fold[ValidatedData[
                (CartItemBundleOptionUpsertion, BundleOption, Product, Seq[ValidCartItemModifierOptionUpsertion]),
              ]](validationProductFailure) { validCoreProduct =>
                val validModifierOptions = validateModifierOptions(bundleOption.modifierOptions, validCoreProduct)
                val validStock = validateStock(validCoreProduct, Some(bundleOption.quantity))

                ValidatedData.combine(validModifierOptions, validStock) {
                  case (vModifierOptions, _) =>
                    (bundleOption, validCoreOption, validCoreProduct, vModifierOptions)
                }
              }
          case i @ Validated.Invalid(_) => i
        }
      }

    val hasAllValidOptions = ValidatedData.sequence(allValidatedOptions)

    val validatedUpsertedSet = upsertedSet
      .fold[ValidatedData[CartItemBundleSetUpsertion]](
        ValidatedData.failure(MissingBundleMetadataForBundleSet(coreBundleSet.id)),
      )(ValidatedData.success)

    (
      validatedUpsertedSet,
      respectsMinQuantity,
      respectsMaxQuantity,
      hasAllValidOptions,
    ).mapN { (validUpsertedSet, _, _, validatedOptions) =>
      ValidCartItemBundleSetUpsertion(
        upsertion = validUpsertedSet,
        coreData = coreBundleSet,
        position = setIndex + 1,
        bundleOptions = validatedOptions.zipWithIndex.map {
          case ((validUpsertion, coreOption, coreProduct, validModifierOptions), optionIndex) =>
            ValidCartItemBundleOptionUpsertion(
              upsertion = validUpsertion,
              coreData = (coreBundleSet, coreOption, coreProduct),
              modifierOptions = validModifierOptions,
              variantOptions = coreProduct.options.map(v => ValidCartItemVariantOptionUpsertion(v)),
              position = optionIndex + 1,
            )
        },
      )
    }
  }

  private def validateStock(
      product: Product,
      quantity: Option[BigDecimal],
    )(implicit
      store: StoreContext,
    ): ValidatedData[Unit] = {
    val data = for {
      productLocation <- product.locationOverrides.get(store.locationId)
      stock <- productLocation.stock
      qty <- quantity
    } yield (stock, qty)

    data match {
      case Some((stock, qty)) if product.trackInventory && !stock.sellOutOfStock && stock.quantity < qty =>
        ValidatedData.failure(ProductOutOfStock(product.id))

      case _ =>
        ValidatedData.success(())
    }
  }

  private def validateModifierOptions(
      modifierOptions: Seq[CartItemModifierOptionUpsertion],
      product: Product,
    )(implicit
      store: StoreContext,
    ): ValidatedData[Seq[ValidCartItemModifierOptionUpsertion]] =
    ValidatedData.sequence {
      val optionIdToSetAndOption = product
        .modifiers
        .getOrElse(Seq.empty)
        .flatMap(modifier => modifier.options.map(option => option.id -> (modifier, option)))
        .toMap

      modifierOptions
        .map(validateModifierOption(_, product, optionIdToSetAndOption))
    }

  private def validateModifierOption(
      upsertion: CartItemModifierOptionUpsertion,
      product: Product,
      optionIdToSetAndOption: Map[UUID, (ModifierSet, ModifierOption)],
    )(implicit
      store: StoreContext,
    ): ValidatedData[ValidCartItemModifierOptionUpsertion] =
    optionIdToSetAndOption.get(upsertion.modifierOptionId) match {
      case Some((coreSet, _)) if !coreSet.locationOverrides.get(store.locationId).exists(_.active) =>
        ValidatedData.failure(DisabledSetModifierOptionIds(Seq(coreSet.id)))

      case Some((_, coreOption)) if !coreOption.active =>
        ValidatedData.failure(DisabledModifierOptionIds(Seq(coreOption.id)))

      case None =>
        ValidatedData.failure(InvalidModifierOptionIds(Seq(upsertion.modifierOptionId)))

      case Some(coreData) =>
        ValidatedData.success(
          ValidCartItemModifierOptionUpsertion(upsertion = upsertion, coreData = coreData),
        )
    }
}
