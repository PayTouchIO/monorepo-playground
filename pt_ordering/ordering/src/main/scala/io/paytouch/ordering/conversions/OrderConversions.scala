package io.paytouch.ordering.conversions

import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.clients.paytouch.core.entities.enums._
import io.paytouch.ordering.entities.{ StoreContext, Cart => CartEntity }
import io.paytouch.ordering.entities.enums.{ CartStatus, PaymentMethodType }
import io.paytouch.ordering.utils.UtcTime

trait OrderConversions
    extends OrderDeliveryAddressConversions
       with OrderTaxRateConversions
       with OrderItemConversions
       with OnlineOrderAttributeConversions
       with OrderBundleSyncConversions {

  protected def toOrderUpsertion(
      cart: CartEntity,
      existingOrder: Option[Order],
    )(implicit
      store: StoreContext,
    ): OrderUpsertion = {
    val (orderBundles, orderItemBundleUpsertions) =
      toOrderBundles(cart, existingOrder)

    val upsertion = OrderUpsertion(
      locationId = store.locationId,
      `type` = cart.orderType.coreOrderType,
      paymentType = paymentType(cart),
      totalAmount = cart.total.amount,
      subtotalAmount = cart.subtotal.amount,
      taxAmount = cart.tax.amount,
      tipAmount = Some(cart.tip.amount),
      deliveryFeeAmount = cart.deliveryFee.map(_.amount),
      // Note that the payment status for successful payments is overwritten in
      // the payment processor enhance conversion functions. As the order is
      // not synced again after being paid, we can keep it as pending here and
      // in order items.
      paymentStatus = PaymentStatus.Pending,
      source = OrderSource.Storefront,
      status = OrderStatus.Received,
      isInvoice = false,
      paymentTransactions = Seq.empty,
      items = toOrderItemUpsertions(cart) ++ orderItemBundleUpsertions,
      deliveryAddress = toDeliveryAddressUpsertion(cart),
      onlineOrderAttribute = toOnlineOrderAttributeUpsertion(cart),
      receivedAt = UtcTime.now,
      completedAt = None,
      taxRates = toOrderTaxRateUpsertions(cart),
      bundles = orderBundles,
    )

    if (cart.isGiftCardOnly)
      upsertion.copy(completedAt = Some(UtcTime.now), status = OrderStatus.Completed)
    else
      upsertion
  }

  protected def paymentType(cart: CartEntity): Option[OrderPaymentType] =
    cart.paymentMethodType match {
      case Some(PaymentMethodType.Ekashu) | Some(PaymentMethodType.Jetdirect) | Some(PaymentMethodType.Worldpay) | Some(
            PaymentMethodType.Stripe,
          ) =>
        Some(OrderPaymentType.CreditCard)
      case Some(PaymentMethodType.Cash) => Some(OrderPaymentType.Cash)
      case None                         => None
    }
}
