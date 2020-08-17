package io.paytouch.core.validators

import java.util.UUID

import scala.concurrent._

import io.paytouch.core.data.daos._
import io.paytouch.core.data.model.{ ArticleRecord, OrderItemRecord }
import io.paytouch.core.data.model.enums._
import io.paytouch.core.entities._
import io.paytouch.core.errors._
import io.paytouch.core.utils._
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.validators.features.DefaultRecoveryValidator

class OrderItemRecoveryValidator(
    implicit
    val ec: ExecutionContext,
    val daos: Daos,
    val logger: PaytouchLogger,
  ) extends DefaultRecoveryValidator[OrderItemRecord] {

  type Dao = OrderItemDao
  type Record = OrderItemRecord

  protected val dao = daos.orderItemDao
  val validationErrorF = InvalidOrderItemIds(_)
  val accessErrorF = NonAccessibleOrderItemIds(_)

  val itemDiscountValidator = new OrderItemDiscountRecoveryValidator
  val orderItemModifierOptionValidator = new OrderItemModifierOptionValidator
  val orderItemTaxRateValidator = new OrderItemTaxRateValidator
  val orderItemVariantOptionValidator = new OrderItemVariantOptionValidator
  val productValidatorIncludingDeleted = new ProductValidatorIncludingDeleted

  def validateUpsertions(
      orderId: UUID,
      upsertions: Seq[OrderItemUpsertion],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Seq[OrderItemUpsertion]]] = {
    val orderItemIds = upsertions.map(_.id)
    val productIds = upsertions.flatMap(_.productId)
    val discountsByOrderItemId = upsertions.map(upsertion => upsertion.id -> upsertion.discounts).toMap
    val modifierOptionsByOrderItemId = upsertions.map(upsertion => upsertion.id -> upsertion.modifierOptions).toMap
    val variantOptionsByOrderItemId = upsertions.map(upsertion => upsertion.id -> upsertion.variantOptions).toMap
    val taxRatesByOrderItemId = upsertions.map(upsertion => upsertion.id -> upsertion.taxRates).toMap
    for {
      availableOrderItemIds <- filterNonAlreadyTakenIds(orderItemIds)
      products <- productValidatorIncludingDeleted.filterValidByIds(productIds)
      validIds = validateIds(orderId, upsertions, availableOrderItemIds)
      validProductIds = validateProductIds(upsertions, products)
      validItemDiscounts <- itemDiscountValidator.validateUpsertions(discountsByOrderItemId)
      validModifierOptions <- orderItemModifierOptionValidator.validateUpsertions(modifierOptionsByOrderItemId)
      validVariantOptions <- orderItemVariantOptionValidator.validateUpsertions(variantOptionsByOrderItemId)
      validTaxRates <- orderItemTaxRateValidator.validateUpsertions(taxRatesByOrderItemId)
    } yield Multiple.combine(
      validIds,
      validProductIds,
      validItemDiscounts,
      validModifierOptions,
      validVariantOptions,
      validTaxRates,
    ) { case _ => upsertions }
  }

  def validateIds(
      orderId: UUID,
      upsertions: Seq[OrderItemUpsertion],
      availableOrderItemIds: Seq[UUID],
    ): ErrorsOr[Seq[UUID]] =
    Multiple.combineSeq(upsertions.map { orderItemUpsertion =>
      val orderItemId = orderItemUpsertion.id
      recoverOrderItemId(availableOrderItemIds, orderItemId, orderId)
    })

  def validateProductIds(
      upsertions: Seq[OrderItemUpsertion],
      products: Seq[ArticleRecord],
    ): ErrorsOr[Seq[Option[UUID]]] =
    Multiple.combineSeq(upsertions.map { orderItemUpsertion =>
      recoverProductId(products, orderItemUpsertion.productId)
    })

  def recoverUpsertions(
      orderId: UUID,
      upsertions: Seq[OrderItemUpsertion],
    )(implicit
      user: UserContext,
    ): Future[Seq[RecoveredOrderItemUpsertion]] = {
    val orderItemIds = upsertions.map(_.id)
    val productIds = upsertions.flatMap(_.productId)
    val discountsByOrderItemId = upsertions.map(upsertion => upsertion.id -> upsertion.discounts).toMap
    val modifierOptionsByOrderItemId = upsertions.map(upsertion => upsertion.id -> upsertion.modifierOptions).toMap
    val variantOptionsByOrderItemId = upsertions.map(upsertion => upsertion.id -> upsertion.variantOptions).toMap
    val taxRatesByOrderItemId = upsertions.map(upsertion => upsertion.id -> upsertion.taxRates).toMap
    for {
      availableOrderItemIds <- filterNonAlreadyTakenIds(orderItemIds)
      products <- productValidatorIncludingDeleted.filterValidByIds(productIds)
      recoveredDiscountsByOrderItemId <- itemDiscountValidator.recoverUpsertionsPerItem(discountsByOrderItemId)
      recoveredModifierOptionsByOrderItemId <- orderItemModifierOptionValidator.recoverUpsertions(
        modifierOptionsByOrderItemId,
      )
      recoveredVariantOptionsByOrderItemId <- orderItemVariantOptionValidator.recoverUpsertions(
        variantOptionsByOrderItemId,
      )
      recoveredTaxRatesByOrderItemId <- orderItemTaxRateValidator.recoverUpsertions(taxRatesByOrderItemId)
      recoveredWithGiftCardCreationByOrderItemId <- recoverWithGiftCardCreation(upsertions, products)
    } yield toRecoveredOrderItemUpsertions(
      orderId,
      upsertions,
      availableOrderItemIds,
      products,
      recoveredDiscountsByOrderItemId,
      recoveredModifierOptionsByOrderItemId,
      recoveredVariantOptionsByOrderItemId,
      recoveredTaxRatesByOrderItemId,
      recoveredWithGiftCardCreationByOrderItemId,
    )
  }

  private def toRecoveredOrderItemUpsertions(
      orderId: UUID,
      orderItemUpsertions: Seq[OrderItemUpsertion],
      availableOrderItemIds: Seq[UUID],
      products: Seq[ArticleRecord],
      discountsPerOrderItemId: Map[UUID, Seq[RecoveredItemDiscountUpsertion]],
      modifierOptionsPerOrderItemId: Map[UUID, Seq[RecoveredOrderItemModifierOptionUpsertion]],
      variantOptionsPerOrderItemId: Map[UUID, Seq[RecoveredOrderItemVariantOptionUpsertion]],
      taxRatesPerOrderItemId: Map[UUID, Seq[RecoveredOrderItemTaxRateUpsertion]],
      withGiftCardCreationPerOrderItemId: Map[UUID, Boolean],
    ): Seq[RecoveredOrderItemUpsertion] =
    orderItemUpsertions.map { orderItemUpsertion =>
      val orderItemId = orderItemUpsertion.id
      val contextDescription = s"While validating order item upsertion of order item $orderItemId of order $orderId"
      val recoveredId =
        logger.loggedSoftRecoverUUID(recoverOrderItemId(availableOrderItemIds, orderItemId, orderId))(
          contextDescription,
        )
      val recoveredProductId =
        logger.loggedRecover(recoverProductId(products, orderItemUpsertion.productId))(
          contextDescription,
          orderItemUpsertion,
        )
      val discounts = discountsPerOrderItemId.getOrElse(orderItemId, Seq.empty)
      val modifierOptions = modifierOptionsPerOrderItemId.getOrElse(orderItemId, Seq.empty)
      val variantOptions = variantOptionsPerOrderItemId.getOrElse(orderItemId, Seq.empty)
      val taxRates = taxRatesPerOrderItemId.getOrElse(orderItemId, Seq.empty)
      val withGiftCardCreation = withGiftCardCreationPerOrderItemId.getOrElse(orderItemId, false)

      val productType = orderItemUpsertion.productType match {
        case at @ Some(ArticleType.CustomProduct) =>
          at

        case _ =>
          products
            .find(p => orderItemUpsertion.productId.contains(p.id))
            .map(_.`type`)
      }

      toRecoveredOrderItemUpsertion(
        orderItemUpsertion,
        recoveredId,
        recoveredProductId,
        productType,
        discounts,
        modifierOptions,
        variantOptions,
        taxRates,
        withGiftCardCreation,
      )
    }

  private def recoverOrderItemId(
      availableOrderItemIds: Seq[UUID],
      orderItemId: UUID,
      orderId: UUID,
    ): ErrorsOr[UUID] =
    if (availableOrderItemIds.contains(orderItemId)) Multiple.success(orderItemId)
    else Multiple.failure(InvalidOrderItemOrderAssociation(orderItemId = orderItemId, orderId = orderId))

  private def recoverProductId(products: Seq[ArticleRecord], productId: Option[UUID]): ErrorsOr[Option[UUID]] =
    productId match {
      case Some(prodId) if products.exists(_.id == prodId) => Multiple.successOpt(prodId)
      case Some(prodId)                                    => Multiple.failure(productValidatorIncludingDeleted.validationErrorF(Seq(prodId)))
      case None                                            => Multiple.empty
    }

  private def recoverWithGiftCardCreation(
      upsertions: Seq[OrderItemUpsertion],
      products: Seq[ArticleRecord],
    )(implicit
      user: UserContext,
    ): Future[Map[UUID, Boolean]] = {
    val giftCardProductIds =
      products
        .filter(_.`type`.isGiftCard)
        .map(_.id)

    if (giftCardProductIds.isEmpty)
      Future.successful(Map.empty)
    else {
      val orderItemIds = upsertions.map(_.id)
      val merchantId = user.merchantId
      dao.findByIdsAndMerchantId(orderItemIds, merchantId).map { existingItems =>
        upsertions.map { upsertion =>
          val existingItem = existingItems.find(_.id == upsertion.id)

          val withGiftCardCreation = {
            val giftCardInvolved = giftCardProductIds.exists(upsertion.productId.contains)
            val paymentStatusChanges = existingItem.flatMap(_.paymentStatus) != upsertion.paymentStatus
            val isPaid = upsertion.paymentStatus.contains(PaymentStatus.Paid)
            giftCardInvolved && paymentStatusChanges && isPaid
          }

          upsertion.id -> withGiftCardCreation
        }.toMap
      }
    }
  }

  private def toRecoveredOrderItemUpsertion(
      orderItemUpsertion: OrderItemUpsertion,
      recoveredId: UUID,
      recoveredProductId: Option[UUID],
      productType: Option[ArticleType],
      discounts: Seq[RecoveredItemDiscountUpsertion],
      modifierOptions: Seq[RecoveredOrderItemModifierOptionUpsertion],
      variantOptions: Seq[RecoveredOrderItemVariantOptionUpsertion],
      taxRates: Seq[RecoveredOrderItemTaxRateUpsertion],
      withGiftCardCreation: Boolean,
    ): RecoveredOrderItemUpsertion =
    RecoveredOrderItemUpsertion(
      id = recoveredId,
      productId = recoveredProductId,
      productName = orderItemUpsertion.productName,
      productDescription = orderItemUpsertion.productDescription,
      productType = productType,
      quantity = orderItemUpsertion.quantity,
      unit = orderItemUpsertion.unit,
      paymentStatus = orderItemUpsertion.paymentStatus,
      priceAmount = orderItemUpsertion.priceAmount,
      costAmount = orderItemUpsertion.costAmount,
      discountAmount = orderItemUpsertion.discountAmount,
      taxAmount = orderItemUpsertion.taxAmount,
      basePriceAmount = orderItemUpsertion.basePriceAmount,
      calculatedPriceAmount = orderItemUpsertion.calculatedPriceAmount,
      totalPriceAmount = orderItemUpsertion.totalPriceAmount,
      notes = orderItemUpsertion.notes,
      discounts = discounts,
      modifierOptions = modifierOptions,
      variantOptions = variantOptions,
      taxRates = taxRates,
      withGiftCardCreation = withGiftCardCreation,
      giftCardPassRecipientEmail = orderItemUpsertion.giftCardPassRecipientEmail,
    )
}

final case class RecoveredOrderItemUpsertion(
    id: UUID,
    productId: Option[UUID],
    productName: Option[String],
    productDescription: Option[String],
    productType: Option[ArticleType],
    quantity: Option[BigDecimal],
    unit: Option[UnitType],
    paymentStatus: Option[PaymentStatus],
    priceAmount: Option[BigDecimal],
    costAmount: Option[BigDecimal],
    discountAmount: Option[BigDecimal],
    taxAmount: Option[BigDecimal],
    basePriceAmount: Option[BigDecimal],
    calculatedPriceAmount: Option[BigDecimal],
    totalPriceAmount: Option[BigDecimal],
    notes: Option[String],
    discounts: Seq[RecoveredItemDiscountUpsertion],
    modifierOptions: Seq[RecoveredOrderItemModifierOptionUpsertion],
    variantOptions: Seq[RecoveredOrderItemVariantOptionUpsertion],
    taxRates: Seq[RecoveredOrderItemTaxRateUpsertion],
    withGiftCardCreation: Boolean,
    giftCardPassRecipientEmail: Option[String],
  )
