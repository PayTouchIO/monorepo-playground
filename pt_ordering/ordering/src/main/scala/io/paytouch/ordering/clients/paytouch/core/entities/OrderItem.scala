package io.paytouch.ordering.clients.paytouch.core.entities

import java.util.UUID

import io.paytouch.ordering.clients.paytouch.core.entities.enums.PaymentStatus
import io.paytouch.ordering.entities.MonetaryAmount
import io.paytouch.ordering.entities.enums.UnitType

final case class OrderItem(
    id: UUID,
    productId: Option[UUID],
    productName: Option[String],
    quantity: Option[BigDecimal],
    paymentStatus: Option[PaymentStatus],
    calculatedPrice: Option[MonetaryAmount],
    totalPrice: Option[MonetaryAmount],
    tax: Option[MonetaryAmount],
    taxRates: Seq[OrderItemTaxRate],
    variantOptions: Seq[OrderItemVariantOption],
    modifierOptions: Seq[OrderItemModifierOption],
  )

final case class OrderItemUpsertion(
    id: UUID,
    productId: Option[UUID],
    productName: Option[String],
    productDescription: Option[String],
    quantity: Option[BigDecimal],
    unit: Option[UnitType],
    paymentStatus: Option[PaymentStatus],
    priceAmount: Option[BigDecimal],
    costAmount: Option[BigDecimal],
    discountAmount: Option[BigDecimal],
    taxAmount: Option[BigDecimal],
    calculatedPriceAmount: Option[BigDecimal],
    totalPriceAmount: Option[BigDecimal],
    modifierOptions: Seq[OrderItemModifierOptionUpsertion],
    variantOptions: Seq[OrderItemVariantOptionUpsertion],
    notes: Option[String],
    taxRates: Seq[OrderItemTaxRateUpsertion],
    giftCardPassRecipientEmail: Option[String],
  )
