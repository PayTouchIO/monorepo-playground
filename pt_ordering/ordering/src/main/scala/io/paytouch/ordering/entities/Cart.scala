package io.paytouch.ordering.entities

import java.time.{ LocalTime, ZonedDateTime }
import java.util.UUID

import io.paytouch.ordering.clients.paytouch.core.entities.enums.CartItemType
import io.paytouch.ordering.entities.enums._

final case class Cart(
    id: UUID,
    storeId: UUID,
    orderId: Option[UUID],
    orderNumber: Option[String],
    total: MonetaryAmount,
    subtotal: MonetaryAmount,
    tax: MonetaryAmount,
    tip: MonetaryAmount,
    deliveryFee: Option[MonetaryAmount],
    phoneNumber: Option[String],
    email: Option[String],
    deliveryAddress: DeliveryAddress,
    orderType: OrderType,
    prepareBy: Option[LocalTime],
    drivingDistanceInMeters: Option[BigDecimal],
    estimatedDrivingTimeInMins: Option[Int],
    taxRates: Seq[CartTaxRate],
    items: Seq[CartItem],
    paymentProcessorData: Option[PaymentProcessorData],
    paymentMethodType: Option[PaymentMethodType],
    status: CartStatus,
    appliedGiftCardPasses: Seq[GiftCardPassApplied],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends ExposedEntity {
  val classShortName = ExposedName.Cart

  val isGiftCardOnly: Boolean = items.forall(_.isGiftCard)
  def isNotGiftCardOnly: Boolean = !isGiftCardOnly
}

final case class CartCreation(
    storeId: UUID,
    email: Option[String],
    phoneNumber: Option[String],
    prepareBy: ResettableLocalTime,
    orderType: OrderType,
    deliveryAddress: DeliveryAddressUpsertion,
  ) extends CreationEntity[CartUpsertion] {

  def asUpsert =
    CartUpsertion(
      storeId = Some(storeId),
      email = email,
      phoneNumber = phoneNumber,
      prepareBy = prepareBy,
      orderType = Some(orderType),
      deliveryAddress = deliveryAddress,
      totalAmountWithoutGiftCards = Some(0),
      totalAmount = Some(0),
      subtotalAmount = Some(0),
      taxAmount = Some(0),
      tipAmount = Some(0),
      deliveryFeeAmount = None,
      paymentMethodType = None,
    )
}

final case class CartUpdate(
    email: ResettableString,
    phoneNumber: ResettableString,
    prepareBy: ResettableLocalTime,
    orderType: Option[OrderType],
    tipAmount: Option[BigDecimal],
    deliveryAddress: DeliveryAddressUpsertion,
    paymentMethodType: Option[PaymentMethodType],
    checkoutSuccessReturnUrl: Option[String],
    checkoutFailureReturnUrl: Option[String],
  ) extends UpdateEntity[CartUpsertion] {
  def asUpsert =
    CartUpsertion(
      storeId = None,
      email = email,
      phoneNumber = phoneNumber,
      prepareBy = prepareBy,
      orderType = orderType,
      deliveryAddress = deliveryAddress,
      totalAmount = None,
      totalAmountWithoutGiftCards = None,
      subtotalAmount = None,
      taxAmount = None,
      tipAmount = tipAmount,
      deliveryFeeAmount = None,
      paymentMethodType = paymentMethodType,
    )
}

final case class CartUpsertion(
    storeId: Option[UUID],
    email: ResettableString,
    phoneNumber: ResettableString,
    prepareBy: ResettableLocalTime,
    orderType: Option[OrderType],
    deliveryAddress: DeliveryAddressUpsertion,
    totalAmount: Option[BigDecimal],
    totalAmountWithoutGiftCards: Option[BigDecimal],
    subtotalAmount: Option[BigDecimal],
    taxAmount: Option[BigDecimal],
    tipAmount: Option[BigDecimal],
    deliveryFeeAmount: ResettableBigDecimal,
    paymentMethodType: Option[PaymentMethodType],
  )
