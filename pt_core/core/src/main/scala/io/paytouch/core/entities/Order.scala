package io.paytouch.core.entities

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.CustomerNote
import io.paytouch.core.data.model.enums._
import io.paytouch.core.entities.enums.ExposedName

final case class Order(
    id: UUID,
    location: Option[Location],
    deviceId: Option[UUID],
    creatorUserId: Option[UUID],
    creatorUser: Option[UserInfo],
    customer: Option[CustomerMerchant],
    number: Option[String],
    tag: Option[String],
    source: Option[Source],
    `type`: Option[OrderType],
    paymentType: Option[OrderPaymentType],
    total: Option[MonetaryAmount],
    subtotal: Option[MonetaryAmount],
    discount: Option[MonetaryAmount],
    tax: Option[MonetaryAmount],
    tip: Option[MonetaryAmount],
    ticketDiscount: Option[MonetaryAmount],
    deliveryFee: Option[MonetaryAmount],
    taxRates: Seq[OrderTaxRate],
    customerNotes: Seq[CustomerNote],
    merchantNotes: Seq[MerchantNote],
    paymentStatus: Option[PaymentStatus],
    status: Option[OrderStatus],
    fulfillmentStatus: Option[FulfillmentStatus],
    orderRoutingStatuses: OrderRoutingStatusesByType,
    orderRoutingKitchenStatuses: OrderRoutingStatusesByKitchen,
    statusTransitions: Seq[StatusTransition],
    isInvoice: Boolean,
    isFiscal: Boolean,
    paymentTransactions: Option[Seq[PaymentTransaction]],
    items: Option[Seq[OrderItem]],
    assignedUsers: Seq[UserInfo],
    discountDetails: Seq[OrderDiscount],
    rewards: Seq[RewardRedemption],
    loyaltyPoints: Option[LoyaltyPoints],
    deliveryAddress: Option[DeliveryAddress],
    onlineOrderAttribute: Option[OnlineOrderAttribute],
    bundles: Seq[OrderBundle],
    tickets: Option[Seq[TicketInfo]],
    tipsAssignments: Seq[TipsAssignment],
    version: Int,
    seating: Option[Seating],
    deliveryProvider: Option[DeliveryProvider],
    deliveryProviderId: Option[String],
    deliveryProviderNumber: Option[String],
    receivedAt: ZonedDateTime,
    completedAt: Option[ZonedDateTime],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends ExposedEntity {
  val classShortName = ExposedName.Order

  def isOpen: Boolean =
    onlineOrderAttribute.exists(_.acceptanceStatus == AcceptanceStatus.Open)
}

final case class OrdersMetadata(
    count: Int,
    salesSummary: Option[SalesSummary],
    typeSummary: Option[Seq[OrdersCountByType]],
  ) extends Metadata[SalesSummary, Seq[OrdersCountByType]]

final case class OrdersCountByType(`type`: OrderType, count: Int)

final case class OrderUpsertion(
    locationId: UUID,
    deviceId: Option[UUID],
    creatorUserId: Option[UUID],
    customerId: Option[UUID],
    tag: ResettableString,
    `type`: OrderType,
    paymentType: Option[OrderPaymentType],
    totalAmount: BigDecimal,
    subtotalAmount: BigDecimal,
    discountAmount: Option[BigDecimal],
    taxAmount: BigDecimal,
    tipAmount: Option[BigDecimal],
    ticketDiscountAmount: Option[BigDecimal],
    deliveryFeeAmount: Option[BigDecimal],
    merchantNotes: Seq[MerchantNoteUpsertion],
    paymentStatus: PaymentStatus,
    source: Option[Source] = None,
    status: OrderStatus,
    fulfillmentStatus: Option[FulfillmentStatus],
    isInvoice: Boolean,
    isFiscal: Option[Boolean],
    paymentTransactions: Seq[PaymentTransactionUpsertion],
    items: Seq[OrderItemUpsertion],
    discounts: Seq[ItemDiscountUpsertion],
    assignedUserIds: Option[Seq[UUID]],
    deliveryAddress: Option[DeliveryAddressUpsertion],
    onlineOrderAttribute: Option[OnlineOrderAttributeUpsertion],
    version: Int = 1,
    deliveryProvider: Option[DeliveryProvider],
    deliveryProviderId: Option[String],
    deliveryProviderNumber: Option[String],
    seating: ResettableSeating,
    receivedAt: ZonedDateTime,
    completedAt: Option[ZonedDateTime],
    rewards: Seq[RewardRedemptionSync],
    taxRates: Seq[OrderTaxRateUpsertion],
    bundles: Seq[OrderBundleUpsertion],
  )
