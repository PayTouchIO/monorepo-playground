package io.paytouch.ordering.services

import java.util.UUID
import java.time.ZonedDateTime

import scala.concurrent._

import cats.data._
import cats.implicits._

import io.paytouch.implicits._

import io.paytouch.ordering._
import io.paytouch.ordering.calculations.CartCalculations
import io.paytouch.ordering.clients.paytouch.core.entities.enums.{ AcceptanceStatus, OrderPaymentType, PaymentStatus }
import io.paytouch.ordering.clients.paytouch.core.entities.GiftCardPass
import io.paytouch.ordering.clients.paytouch.core.entities.GiftCardPassCharge
import io.paytouch.ordering.clients.paytouch.core.entities.OrderUpsertion
import io.paytouch.ordering.clients.paytouch.core.PtCoreClient
import io.paytouch.ordering.conversions.CartConversions
import io.paytouch.ordering.data.daos.{ CartDao, Daos }
import io.paytouch.ordering.data.model.{ CartRecord, CartUpdate, EkashuConfig, JetdirectConfig }
import io.paytouch.ordering.data.model.upsertions.{ CartUpsertion => CartUpsertionModel }
import io.paytouch.ordering.entities.{ Cart => CartEntity, CartUpdate => CartUpdateEntity, _ }
import io.paytouch.ordering.errors._
import io.paytouch.ordering.expansions.NoExpansions
import io.paytouch.ordering.filters.NoFilters
import io.paytouch.ordering.services.features._
import io.paytouch.ordering.UpsertionResult
import io.paytouch.ordering.utils.ResultType
import io.paytouch.ordering.utils.UtcTime
import io.paytouch.ordering.utils.validation.ValidatedData
import io.paytouch.ordering.utils.validation.ValidatedData.ValidatedData
import io.paytouch.ordering.validators.CartValidator

class CartService(
    val ptCoreClient: PtCoreClient,
    val cartItemService: CartItemService,
    cartSyncService: => CartSyncService,
    val cartTaxRateService: CartTaxRateService,
    ekashuService: => EkashuService,
    jetdirectService: => JetdirectService,
    val locationService: LocationService,
    val merchantService: MerchantService,
    val gMapsService: GMapsService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends CartConversions
       with CartCalculations
       with FindByIdFeature
       with CreateFeature
       with UpdateFeature {
  type Context = StoreContext
  type Creation = CartCreation
  type Dao = CartDao
  type Entity = CartEntity
  type Expansions = NoExpansions
  type Filters = NoFilters
  type Model = CartUpsertionModel
  type Record = CartRecord
  type Update = CartUpdateEntity
  type Upsertion = CartUpsertion
  type Validator = CartValidator

  protected val dao = daos.cartDao
  protected val validator = new CartValidator(ptCoreClient)
  protected val storeDao = daos.storeDao

  protected val defaultFilters = NoFilters()
  protected val defaultExpansions = NoExpansions()

  protected val expanders = {
    val cartTaxRateExpander = MandatoryExpander[this.type, Seq[CartTaxRate]](
      retrieverF = implicit context => cartTaxRateService.findTaxRatesByCart,
      copierF = (entity, data) => entity.copy(taxRates = data),
      defaultDataValue = Seq.empty,
    )
    val cartItemExpander = MandatoryExpander[this.type, Seq[CartItem]](
      retrieverF = implicit context => cartItemService.findItemsByCart,
      copierF = (entity, data) => entity.copy(items = data),
      defaultDataValue = Seq.empty,
    )
    val paymentProcessorDataExpander = MandatoryExpander[this.type, PaymentProcessorData](
      retrieverF = implicit context => getPaymentProcessorData,
      copierF = (entity, data) => entity.copy(paymentProcessorData = Some(data)),
      defaultDataValue = PaymentProcessorData.empty,
    )

    Seq(
      cartTaxRateExpander,
      cartItemExpander,
      paymentProcessorDataExpander,
    )
  }

  override def create(
      id: UUID,
      creation: CartCreation,
    )(implicit
      context: StoreContext,
    ): Future[UpsertionResult[Entity]] =
    applyOperationToCart(id)(super.create(id, creation))

  override def update(
      id: UUID,
      update: CartUpdateEntity,
    )(implicit
      context: StoreContext,
    ): Future[UpsertionResult[Entity]] =
    applyOperationToCart(id)(super.update(id, update))

  protected def convertToUpsertionModel(
      id: UUID,
      upsertion: Upsertion,
      existing: Option[Record],
    )(implicit
      store: StoreContext,
    ): Future[Model] =
    for {
      storeAddress <- getStoreAddressIfNeeded(id, upsertion, existing)
      drivingInfo <- getDrivingInfoIfNeeded(id, upsertion, existing, storeAddress)
      // assign paymentProcessor only on create
      merchant <- if (existing.isEmpty) merchantService.find else Future.successful(None)
    } yield toUpsertionModel(id, upsertion, merchant, storeAddress, drivingInfo, None)

  private def getStoreAddressIfNeeded(
      id: UUID,
      upsertion: Upsertion,
      existing: Option[Record],
    )(implicit
      store: StoreContext,
    ): Future[Option[Address]] = {
    val needsFetching = upsertion.storeId != existing.map(_.storeId)
    if (needsFetching) locationService.findById(store.locationId).map(_.map(_.address.toOrderingAddress))
    else Future.successful(None)
  }

  private def getDrivingInfoIfNeeded(
      id: UUID,
      upsertion: Upsertion,
      existing: Option[Record],
      storeAddress: Option[Address],
    )(implicit
      store: StoreContext,
    ): Future[Option[DrivingInfo]] = {
    val newOrigin = storeAddress.isDefined
    val previousDestination = existing.map(toDeliveryAddress(_).address)
    val currentDestination = upsertion.deliveryAddress.address.toAddress
    val newDestination = currentDestination.isDefined && previousDestination != currentDestination
    val needsFetching = newOrigin || newDestination

    if (needsFetching)
      gMapsService.getDrivingInfo(
        origin = storeAddress.orElse(existing.flatMap(_.storeAddress)),
        destination = currentDestination.orElse(previousDestination),
      )
    else
      Future.successful(None)
  }

  def applyGiftCard(
      cartId: UUID,
      onlineCode: io.paytouch.GiftCardPass.OnlineCode.Raw,
    )(implicit
      context: StoreContext,
    ): Future[UpsertionResult[Entity]] =
    validateAccessToCart(cartId) { cart =>
      applyOperationToCart(cartId) {
        validator
          .validateGiftCardPass(onlineCode)
          .flatMap {
            case Validated.Valid(giftCardPass) =>
              if (giftCardPass.balance.isZero)
                ValidatedData.failure(GiftCardPassIsUsedUp(onlineCode)).pure[Future]
              else
                addGiftCardPassToCart(cart, onlineCode)(giftCardPass).map(ValidatedData.success)

            case invalid @ Validated.Invalid(_) =>
              invalid.pure[Future]
          }
      }
    }

  def applyGiftCardFailures(
      cartRecord: CartRecord,
      bulkFailure: Seq[GiftCardPassCharge.Failure],
    )(implicit
      context: StoreContext,
    ): Future[Unit] =
    applyOperationToCart(cartRecord.id)(
      dao
        .upsert(giftCardFailureUpdate(cartRecord, bulkFailure, MonetaryAmount(_)))
        .map(ValidatedData.success),
    ).void

  private[this] def giftCardFailureUpdate(
      cartRecord: CartRecord,
      bulkFailure: Seq[GiftCardPassCharge.Failure],
      toMonetary: BigDecimal => MonetaryAmount,
    ): CartUpdate =
    CartUpdate
      .empty
      .copy(
        id = cartRecord.id.some,
        appliedGiftCardPasses = cartRecord
          .appliedGiftCardPasses
          .map { pass =>
            bulkFailure
              .find(_.giftCardPassId === pass.id)
              .map(_.actualBalance)
              .fold(pass)(amount => pass.copy(balance = toMonetary(amount)))
          }
          .some,
      )

  private[this] def addGiftCardPassToCart(
      cart: Entity,
      onlineCode: io.paytouch.GiftCardPass.OnlineCode.Raw,
    )(
      giftCardPass: GiftCardPass,
    ): Future[Result[Record]] =
    dao.upsert(
      newPassUpdate(
        cart = cart,
        newPass = toGiftCardPassApplied(
          giftCardPass = giftCardPass,
          onlineCode = onlineCode,
          addedAt = UtcTime.now,
        ),
      ),
    )

  private[this] def newPassUpdate(cart: Entity, newPass: GiftCardPassApplied): CartUpdate =
    CartUpdate
      .empty
      .copy(
        id = cart.id.some,
        appliedGiftCardPasses = addPassUnlessAlreadyExists(
          current = cart.appliedGiftCardPasses,
          newOne = newPass,
        ).some,
      )

  private[this] def addPassUnlessAlreadyExists(
      current: Seq[GiftCardPassApplied],
      newOne: GiftCardPassApplied,
    ): Seq[GiftCardPassApplied] =
    current
      .filterNot(_.id === newOne.id)
      .:+(newOne)
      .sortBy(_.addedAt)

  private[this] def toGiftCardPassApplied(
      giftCardPass: GiftCardPass,
      onlineCode: io.paytouch.GiftCardPass.OnlineCode.Raw,
      addedAt: ZonedDateTime,
    ): GiftCardPassApplied = {
    import io.scalaland.chimney.dsl._

    giftCardPass
      .into[GiftCardPassApplied]
      .withFieldConst(_.onlineCode, onlineCode)
      .withFieldConst(_.addedAt, addedAt)
      .transform
  }

  def unapplyGiftCard(
      cartId: UUID,
      onlineCode: io.paytouch.GiftCardPass.OnlineCode.Raw,
    )(implicit
      context: StoreContext,
    ): Future[UpsertionResult[Entity]] =
    validateAccessToCart(cartId) { cart =>
      applyOperationToCart(cartId) {
        findNonChargedGiftCardPassIn(cart, onlineCode)
          .bimap(_.pure[Future], removeGiftCardPassFrom(cart))
          .fold(_.map(ValidatedData.failure), _.map(ValidatedData.success))
      }
    }

  private[this] def findNonChargedGiftCardPassIn(
      cart: Entity,
      onlineCode: io.paytouch.GiftCardPass.OnlineCode.Raw,
    ): Either[DataError, GiftCardPassApplied] =
    cart
      .appliedGiftCardPasses
      .find(_.onlineCode === onlineCode)
      .toRight(GiftCardPassByOnlineCodeNotFound(onlineCode))
      .filterOrElse(_.paymentTransactionId.isEmpty, GiftCardPassAlreadyCharged(onlineCode))

  private[this] def removeGiftCardPassFrom(
      cart: Entity,
    )(
      pass: GiftCardPassApplied,
    ): Future[Result[Record]] =
    dao.upsert(
      CartUpdate
        .empty
        .copy(
          id = cart.id.some,
          appliedGiftCardPasses = cart
            .appliedGiftCardPasses
            .filterNot { p =>
              Boolean.and(
                p.onlineCode === pass.onlineCode,
                p.paymentTransactionId.isEmpty, // checking again just in case
              )
            }
            .some,
        ),
    )

  def addItem(creation: CartItemCreation)(implicit context: Context): Future[UpsertionResult[Entity]] =
    validateAccessToCart(creation.cartId) { cart =>
      applyOperationToCart(creation.cartId) {
        cartItemService.createOrMerge(cart, creation)
      }
    }

  def updateItem(
      cartId: UUID,
      cartItemId: UUID,
      cartItemUpdate: CartItemUpdate,
    )(implicit
      context: Context,
    ): Future[UpsertionResult[Entity]] =
    validateAccessToCart(cartId) { cart =>
      applyOperationToCart(cartId) {
        cartItemService.update(cart, cartItemId, cartItemUpdate)
      }
    }

  def removeItem(cartId: UUID, cartItemId: UUID)(implicit context: Context): Future[UpsertionResult[Entity]] =
    applyOperationToCart(cartId) {
      cartItemService.delete(cartItemId).mapValid(ResultType.Updated -> _)
    }

  private def refreshCartCalculations(cartId: UUID)(implicit context: Context): Future[UpsertionResult[Entity]] =
    validateAccessToCart(cartId) { cart =>
      for {
        (resultType, record) <- dao.upsert(calculationsUpdate(cart))
        entity <- enrich(record)
      } yield ValidatedData.success(resultType -> entity)
    }

  def validateAccessToCart[E](
      cartId: UUID,
    )(
      f: Entity => Future[UpsertionResult[E]],
    )(implicit
      context: Context,
    ): Future[UpsertionResult[E]] =
    validator
      .accessOneById(cartId)
      .flatMap {
        case Validated.Valid(record) =>
          for {
            entity <- enrich(record)
            result <- f(entity)
          } yield result

        case i @ Validated.Invalid(_) =>
          Future.successful(i)
      }

  private def applyOperationToCart[T](
      cartId: UUID,
    )(
      f: => Future[UpsertionResult[T]],
    )(implicit
      context: Context,
    ): Future[UpsertionResult[Entity]] =
    (for {
      resultTypeWithEntity <- EitherT(f.map(_.toEither))
      (resultType, _) = resultTypeWithEntity
      resultTypeWithUpdatedEntity <- EitherT(refreshCartCalculations(cartId).map(_.toEither))
      (_, updatedEntity) = resultTypeWithUpdatedEntity
    } yield resultType -> updatedEntity).value.map(_.toValidated)

  override protected def validateUpsertionModel(
      id: UUID,
      model: Model,
    )(implicit
      store: StoreContext,
    ): Future[ValidatedData[Model]] =
    model
      .cart
      .drivingDistanceInMeters
      .toOption
      .fold(Future.successful(ValidatedData.success(model))) { distance =>
        storeDao.findById(store.id).map {
          case Some(s) if s.deliveryMaxDistance.exists(_ < distance) =>
            ValidatedData.failure(AddressTooFarForDelivery(id, distance, s.deliveryMaxDistance.getOrElse(-1)))

          case _ =>
            ValidatedData.success(model)
        }
      }

  def updateAndValidate(
      id: UUID,
      update: CartUpdateEntity,
    )(implicit
      context: StoreContext,
    ): Future[ValidatedData[Record]] =
    for {
      update <- applyOperationToCart(id)(super.update(id, update))
      validate <- validator.validateCheckout(id)
    } yield ValidatedData.combine(update, validate) { case (_, cart) => cart }

  def sync(id: UUID)(implicit context: StoreContext): Future[UpsertionResult[Entity]] =
    validator.validateSync(id).onValid { cart =>
      cartSyncService.sync(cart, (_, upsertion) => upsertion).flatMapValid {
        case (result, record) => enrich(record).map((result, _))
      }
    }

  def getPaymentProcessorData(
      cartRecords: Seq[CartRecord],
    )(implicit
      context: AppContext,
    ): Future[Map[CartRecord, PaymentProcessorData]] =
    merchantService.getPaymentProcessorConfig(context).map {
      case Some(ekashuConfig: EkashuConfig) =>
        cartRecords.map(cart => cart -> ekashuService.computeEkashuHashCodeByCart(cart, ekashuConfig)).toMap

      case Some(jetdirectConfig: JetdirectConfig) =>
        cartRecords.map(cart => cart -> jetdirectService.computeJetdirectHashCodeByCart(cart, jetdirectConfig)).toMap

      case _ =>
        Map.empty
    }
}
