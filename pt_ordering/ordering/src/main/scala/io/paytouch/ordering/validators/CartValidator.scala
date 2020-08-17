package io.paytouch.ordering.validators

import scala.concurrent._

import java.util.UUID

import cats.data._

import io.paytouch.implicits._

import io.paytouch.ordering.clients.paytouch.core.PtCoreClient
import io.paytouch.ordering.data.daos.{ CartDao, Daos }
import io.paytouch.ordering.data.model.CartRecord
import io.paytouch.ordering.entities.{ AddressUpsertion, CartUpsertion, StoreContext }
import io.paytouch.ordering.entities.enums.{ CartStatus, OrderType, PaymentMethodType }
import io.paytouch.ordering.errors._
import io.paytouch.ordering.utils.validation.{ ValidatedData, ValidatedOptData }
import io.paytouch.ordering.utils.validation.ValidatedData.ValidatedData
import io.paytouch.ordering.utils.validation.ValidatedOptData.ValidatedOptData
import io.paytouch.ordering.validators.features._

class CartValidator(val ptCoreClient: PtCoreClient)(implicit val ec: ExecutionContext, val daos: Daos)
    extends DefaultStoreValidator
       with UpsertionValidator
       with PtCoreValidator {
  type Dao = CartDao
  type Record = CartRecord
  type Upsertion = CartUpsertion

  protected val dao = daos.cartDao

  val validationErrorF = InvalidCartIds(_)
  val accessErrorF = NonAccessibleCartIds(_)

  def validateUpsertion(
      id: UUID,
      upsertion: Upsertion,
      existing: Option[Record],
    )(implicit
      store: StoreContext,
    ): Future[ValidatedData[Upsertion]] =
    Future.successful {
      val validStatus = validateStatus(id, existing)
      val validAddress = validateAddressForOrderType(id, upsertion, existing)
      val validTip = validateTip(id, upsertion)
      val validPaymentMethodType = validatePaymentMethodType(id, upsertion)
      ValidatedData.combine(validStatus, validAddress, validTip, validPaymentMethodType) { case _ => upsertion }
    }

  def validateSync(id: UUID)(implicit store: StoreContext): Future[ValidatedData[Record]] =
    accessOneById(id).onValid { cart =>
      validateStatus(id, Some(cart)) match {
        case Validated.Valid(_)       => Future.successful(ValidatedData.success(cart))
        case i @ Validated.Invalid(_) => Future.successful(i)
      }
    }

  def validateCheckout(id: UUID)(implicit store: StoreContext): Future[ValidatedData[Record]] =
    accessOneById(id).flatMap {
      case Validated.Valid(cart) =>
        for {
          validDeliveryAmount <- validateDeliveryAmount(cart)
          validPaymentMethodType <- validatePaymentMethodTypePresent(cart)
        } yield ValidatedData.combine(validDeliveryAmount, validPaymentMethodType) { case _ => cart }
      case i @ Validated.Invalid(_) => Future.successful(i)
    }

  private def validateStatus(
      id: UUID,
      existing: Option[Record],
    )(implicit
      store: StoreContext,
    ): ValidatedOptData[Record] =
    existing.map(_.status) match {
      case Some(CartStatus.Paid) =>
        ValidatedOptData.failure(ImmutableCart(id))
      case _ => ValidatedOptData.success(existing)
    }

  private def validateTip(id: UUID, upsertion: Upsertion)(implicit store: StoreContext): ValidatedData[Upsertion] =
    upsertion.tipAmount match {
      case Some(tip) if tip < 0 => ValidatedData.failure(NegativeTip(tip))
      case _                    => ValidatedData.success(upsertion)
    }

  private def validateAddressForOrderType(
      id: UUID,
      upsertion: Upsertion,
      existing: Option[Record],
    )(implicit
      store: StoreContext,
    ): ValidatedData[Upsertion] =
    mergeOrderType(upsertion, existing) match {
      case Some(OrderType.Delivery) =>
        val mergedAddress = mergeAddress(upsertion.deliveryAddress.address, existing)
        validateAddressNonEmpty(id, mergedAddress).map(_ => upsertion)
      case _ => ValidatedData.success(upsertion)
    }

  private def validateAddressNonEmpty(id: UUID, address: AddressUpsertion): ValidatedData[AddressUpsertion] =
    if (address.isEmpty) ValidatedData.failure(AddressRequiredForDelivery(id))
    else ValidatedData.success(address)

  private def validatePaymentMethodType(
      id: UUID,
      upsertion: Upsertion,
    )(implicit
      store: StoreContext,
    ): ValidatedOptData[Option[PaymentMethodType]] =
    upsertion.paymentMethodType match {
      case Some(t) =>
        store.paymentMethods.find(_.`type` == t).map(_.active) match {
          case Some(true) => ValidatedOptData.successOpt(upsertion.paymentMethodType)
          case _          => ValidatedOptData.failure(UnsupportedPaymentMethodType(t))
        }
      case None => ValidatedOptData.empty
    }

  private def validateDeliveryAmount(cart: Record)(implicit store: StoreContext): Future[ValidatedData[BigDecimal]] =
    Future.successful {
      val minAmount: BigDecimal = store.deliveryMinAmount.getOrElse(0)
      val maxAmount: BigDecimal = store.deliveryMaxAmount.getOrElse(Int.MaxValue)
      val isNotWithinRange: Boolean =
        Boolean.or(
          cart.totalAmountWithoutGiftCards < minAmount,
          cart.totalAmountWithoutGiftCards > maxAmount,
        )

      if (cart.orderType == OrderType.Delivery && isNotWithinRange)
        ValidatedData.failure(CartTotalOutOfBounds(cart.totalAmountWithoutGiftCards, minAmount, maxAmount))
      else
        ValidatedData.success(cart.totalAmountWithoutGiftCards)
    }

  private def validatePaymentMethodTypePresent(cart: Record): Future[ValidatedData[PaymentMethodType]] =
    Future.successful {
      cart.paymentMethodType match {
        case Some(t) => ValidatedData.success(t)
        case None    => ValidatedData.failure(MissingPaymentMethodType())
      }
    }

  private def mergeOrderType(upsertion: Upsertion, existingRecord: Option[Record]): Option[OrderType] =
    upsertion.orderType.orElse(existingRecord.map(_.orderType))

  private def mergeAddress(upsertion: AddressUpsertion, existingRecord: Option[Record]): AddressUpsertion = {
    val line1 = existingRecord.flatMap(_.deliveryAddressLine1)
    val line2 = existingRecord.flatMap(_.deliveryAddressLine2)
    val city = existingRecord.flatMap(_.deliveryCity)
    val state = existingRecord.flatMap(_.deliveryState)
    val country = existingRecord.flatMap(_.deliveryCountry)
    val postalCode = existingRecord.flatMap(_.deliveryPostalCode)

    AddressUpsertion(
      line1 = upsertion.line1.getOrElse(line1),
      line2 = upsertion.line2.getOrElse(line2),
      city = upsertion.city.getOrElse(city),
      state = upsertion.state.getOrElse(state),
      country = upsertion.country.getOrElse(country),
      postalCode = upsertion.postalCode.getOrElse(postalCode),
    )
  }

  override def validateDeletion(ids: Seq[UUID])(implicit context: StoreContext): Future[ValidatedData[Seq[UUID]]] =
    dao.findByIdsAndStoreId(ids, context.id).map { records =>
      val submittedIds = records.filter(_.orderId.isDefined).map(_.id)
      if (submittedIds.nonEmpty) ValidatedData.failure(ImmutableCart(submittedIds))
      else ValidatedData.success(ids)
    }

  def exists(id: UUID): Future[ValidatedData[Record]] =
    dao.findById(id).map {
      case Some(cart) => ValidatedData.success(cart)
      case _          => ValidatedData.failure(InvalidCartIds(Seq(id)))
    }
}
