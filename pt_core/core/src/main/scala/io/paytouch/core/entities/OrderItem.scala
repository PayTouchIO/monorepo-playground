package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.data.model.enums._

final case class OrderItem(
    id: UUID,
    orderId: UUID,
    productId: Option[UUID],
    productName: Option[String],
    productDescription: Option[String],
    productType: Option[ArticleType],
    quantity: Option[BigDecimal],
    unit: Option[UnitType],
    paymentStatus: Option[PaymentStatus],
    price: Option[MonetaryAmount],
    cost: Option[MonetaryAmount],
    discount: Option[MonetaryAmount],
    tax: Option[MonetaryAmount],
    basePrice: Option[MonetaryAmount],
    calculatedPrice: Option[MonetaryAmount],
    totalPrice: Option[MonetaryAmount],
    variantOptions: Seq[OrderItemVariantOption],
    modifierOptions: Seq[OrderItemModifierOption],
    discounts: Seq[OrderItemDiscount],
    orderRoutingStatus: Option[OrderRoutingStatus],
    notes: Option[String],
    taxRates: Seq[OrderItemTaxRate],
    giftCardPass: Option[GiftCardPassInfo],
  )

final case class OrderItemUpsertion(
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
    discounts: Seq[ItemDiscountUpsertion],
    modifierOptions: Seq[OrderItemModifierOptionUpsertion],
    variantOptions: Seq[OrderItemVariantOptionUpsertion],
    notes: Option[String],
    taxRates: Seq[OrderItemTaxRateUpsertion],
    giftCardPassRecipientEmail: Option[String],
  )
