package io.paytouch.ordering.conversions

import java.util.UUID

import cats.implicits._

import io.paytouch.implicits._

import io.paytouch.ordering.data.model.upsertions.{ CartUpsertion => CartUpsertionModel }
import io.paytouch.ordering.data.model.{ CartRecord, CartUpdate => CartUpdateModel }
import io.paytouch.ordering.entities.enums.{ CartStatus, OrderType }
import io.paytouch.ordering.entities.{
  Address,
  AppContext,
  CartUpsertion,
  DrivingInfo,
  Merchant,
  MonetaryAmount,
  ResettableBigDecimal,
  StoreContext,
  Cart => CartEntity,
}
import io.paytouch.ordering.entities.GiftCardPassApplied

trait CartConversions extends CartDeliveryAddressConversions {
  protected def fromRecordToEntity(record: CartRecord)(implicit app: AppContext): CartEntity =
    CartEntity(
      id = record.id,
      storeId = record.storeId,
      orderId = record.orderId,
      orderNumber = record.orderNumber,
      total = MonetaryAmount(record.totalAmount),
      subtotal = MonetaryAmount(record.subtotalAmount),
      tax = MonetaryAmount(record.taxAmount),
      tip = MonetaryAmount(record.tipAmount),
      deliveryFee = MonetaryAmount.extract(record.deliveryFeeAmount),
      phoneNumber = record.phoneNumber,
      email = record.email,
      deliveryAddress = toDeliveryAddress(record),
      orderType = record.orderType,
      prepareBy = record.prepareBy,
      drivingDistanceInMeters = record.drivingDistanceInMeters,
      estimatedDrivingTimeInMins = record.estimatedDrivingTimeInMins,
      taxRates = Seq.empty,
      items = Seq.empty,
      paymentProcessorData = None,
      paymentMethodType = record.paymentMethodType,
      status = record.status,
      appliedGiftCardPasses = record.appliedGiftCardPasses.sortBy(_.addedAt),
      createdAt = record.createdAt,
      updatedAt = record.updatedAt,
    )

  protected def toUpsertionModel(
      id: UUID,
      upsertion: CartUpsertion,
      merchant: Option[Merchant],
      storeAddress: Option[Address],
      drivingInfo: Option[DrivingInfo],
      appliedGiftCardPasses: Option[Seq[GiftCardPassApplied]],
    )(implicit
      store: StoreContext,
    ): CartUpsertionModel =
    CartUpsertionModel(
      cart = CartUpdateModel(
        id = id,
        merchantId = store.merchantId.some,
        storeId = upsertion.storeId,
        orderId = None,
        orderNumber = None,
        paymentProcessor = merchant.map(_.paymentProcessor),
        paymentMethodType = upsertion.paymentMethodType,
        currency = store.currency.some,
        subtotalAmount = upsertion.subtotalAmount,
        taxAmount = upsertion.taxAmount,
        tipAmount = upsertion.tipAmount,
        totalAmountWithoutGiftCards = upsertion.totalAmountWithoutGiftCards,
        totalAmount = upsertion.totalAmount,
        deliveryFeeAmount = upsertion.orderType match {
          case Some(OrderType.Delivery) => store.deliveryFeeAmount
          case Some(OrderType.TakeOut)  => ResettableBigDecimal.reset
          case _                        => ResettableBigDecimal.ignore
        },
        phoneNumber = upsertion.phoneNumber,
        email = upsertion.email,
        firstName = upsertion.deliveryAddress.firstName,
        lastName = upsertion.deliveryAddress.lastName,
        deliveryAddressLine1 = upsertion.deliveryAddress.address.line1,
        deliveryAddressLine2 = upsertion.deliveryAddress.address.line2,
        deliveryCity = upsertion.deliveryAddress.address.city,
        deliveryState = upsertion.deliveryAddress.address.state,
        deliveryCountry = upsertion.deliveryAddress.address.country,
        deliveryPostalCode = upsertion.deliveryAddress.address.postalCode,
        orderType = upsertion.orderType,
        prepareBy = upsertion.prepareBy,
        drivingDistanceInMeters = drivingInfo.map(_.distanceInMeters),
        estimatedDrivingTimeInMins = drivingInfo.map(_.durationInMins),
        storeAddress = storeAddress,
        status = None,
        appliedGiftCardPasses = appliedGiftCardPasses,
      ),
      cartTaxRates = Seq.empty,
      cartItems = Seq.empty,
    )
}
