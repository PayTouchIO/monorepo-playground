package io.paytouch.core.data.model.upsertions

import io.paytouch.core.data.model._
import io.paytouch.core.validators.RecoveredOrderUpsertion

final case class OrderUpsertion(
    order: OrderUpdate,
    orderItems: Seq[OrderItemUpsertion],
    creatorOrderUsers: Option[OrderUserUpdate],
    assignedOrderUsers: Option[Seq[OrderUserUpdate]],
    paymentTransactions: Seq[PaymentTransactionUpdate],
    paymentTransactionFees: Seq[PaymentTransactionFeeUpdate],
    paymentTransactionOrderItems: Seq[PaymentTransactionOrderItemUpdate],
    customerLocation: Option[CustomerLocationUpdate],
    orderTaxRates: Option[Seq[OrderTaxRateUpdate]],
    orderDiscounts: Option[Seq[OrderDiscountUpdate]],
    giftCardPasses: Option[Seq[GiftCardPassUpdate]],
    rewardRedemptions: Option[Seq[RewardRedemptionUpdate]],
    canDeleteOrderItems: Boolean,
    events: List[RecoveredOrderUpsertion.Event],
    deliveryAddress: Option[OrderDeliveryAddressUpdate],
    onlineOrderAttribute: Option[OnlineOrderAttributeUpdate],
    orderBundles: Option[Seq[OrderBundleUpdate]],
  )

object OrderUpsertion {
  def empty(orderUpdate: OrderUpdate): OrderUpsertion =
    OrderUpsertion(
      orderUpdate,
      orderItems = Seq.empty,
      creatorOrderUsers = None,
      assignedOrderUsers = None,
      paymentTransactions = Seq.empty,
      paymentTransactionFees = Seq.empty,
      paymentTransactionOrderItems = Seq.empty,
      customerLocation = None,
      orderTaxRates = None,
      orderDiscounts = None,
      giftCardPasses = None,
      rewardRedemptions = None,
      canDeleteOrderItems = false,
      events = List.empty,
      deliveryAddress = None,
      onlineOrderAttribute = None,
      orderBundles = None,
    )
}
