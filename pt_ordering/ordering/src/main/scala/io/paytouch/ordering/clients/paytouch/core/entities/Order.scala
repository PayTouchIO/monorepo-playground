package io.paytouch.ordering.clients.paytouch.core.entities

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.ordering.entities.MonetaryAmount
import io.paytouch.ordering.clients.paytouch.core.entities.enums._

final case class Order(
    id: UUID,
    location: Option[Location],
    number: Option[String],
    status: OrderStatus,
    paymentStatus: PaymentStatus,
    items: Seq[OrderItem],
    bundles: Seq[OrderBundle],
    taxRates: Seq[OrderTaxRate],
    paymentTransactions: Seq[PaymentTransaction],
    subtotal: Option[MonetaryAmount],
    tax: Option[MonetaryAmount],
    tip: Option[MonetaryAmount],
    total: Option[MonetaryAmount],
    onlineOrderAttribute: Option[OnlineOrderAttribute],
    completedAt: Option[ZonedDateTime],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  )

final case class OrderUpsertion(
    locationId: UUID,
    `type`: OrderType,
    paymentType: Option[OrderPaymentType],
    totalAmount: BigDecimal,
    subtotalAmount: BigDecimal,
    taxAmount: BigDecimal,
    tipAmount: Option[BigDecimal],
    deliveryFeeAmount: Option[BigDecimal],
    paymentStatus: PaymentStatus,
    source: OrderSource,
    status: OrderStatus,
    isInvoice: Boolean,
    paymentTransactions: Seq[PaymentTransactionUpsertion],
    items: Seq[OrderItemUpsertion],
    deliveryAddress: DeliveryAddressUpsertion,
    onlineOrderAttribute: OnlineOrderAttributeUpsertion,
    receivedAt: ZonedDateTime,
    completedAt: Option[ZonedDateTime],
    taxRates: Seq[OrderTaxRateUpsertion],
    bundles: Seq[OrderBundleUpsertion],
  )
