package io.paytouch.core.conversions

import io.paytouch.core.calculations.OrderRoutingStatusCalculation
import io.paytouch.core.data.model._
import io.paytouch.core.entities.{ OrderItem => OrderItemEntity, _ }

trait OrderItemConversions extends OrderRoutingStatusCalculation {

  def fromRecordsAndOptionsToEntities(
      records: Seq[OrderItemRecord],
      discountsPerOrderItem: Map[OrderItemRecord, Seq[OrderItemDiscount]],
      modifierOptionsPerOrderItem: Map[OrderItemRecord, Seq[OrderItemModifierOption]],
      taxRatesPerOrderItem: Map[OrderItemRecord, Seq[OrderItemTaxRate]],
      variantOptionsPerOrderItem: Map[OrderItemRecord, Seq[OrderItemVariantOption]],
      ticketsPerOrderItem: Map[OrderItemRecord, Seq[TicketRecord]],
      giftCardPassesPerOrderItem: Option[Map[OrderItemRecord, GiftCardPassInfo]],
    )(implicit
      user: UserContext,
    ): Seq[OrderItemEntity] =
    records.map { record =>
      val discounts = discountsPerOrderItem.getOrElse(record, Seq.empty)
      val modifierOptions = modifierOptionsPerOrderItem.getOrElse(record, Seq.empty)
      val taxRates = taxRatesPerOrderItem.getOrElse(record, Seq.empty)
      val variantOptions = variantOptionsPerOrderItem.getOrElse(record, Seq.empty)
      val tickets = ticketsPerOrderItem.getOrElse(record, Seq.empty)
      val giftCardPasses = giftCardPassesPerOrderItem.flatMap(_.get(record))
      fromRecordAndOptionsToEntity(
        record,
        discounts,
        modifierOptions,
        taxRates,
        variantOptions,
        tickets,
        giftCardPasses,
      )
    }

  def fromRecordAndOptionsToEntity(
      record: OrderItemRecord,
      discounts: Seq[OrderItemDiscount],
      modifierOptions: Seq[OrderItemModifierOption],
      taxRates: Seq[OrderItemTaxRate],
      variantOptions: Seq[OrderItemVariantOption],
      tickets: Seq[TicketRecord],
      giftCardPasses: Option[GiftCardPassInfo],
    )(implicit
      user: UserContext,
    ): OrderItemEntity =
    OrderItemEntity(
      id = record.id,
      orderId = record.orderId,
      productId = record.productId,
      productName = record.productName,
      productDescription = record.productDescription,
      productType = record.productType,
      quantity = record.quantity,
      unit = record.unit,
      paymentStatus = record.paymentStatus,
      price = MonetaryAmount.extract(record.priceAmount),
      cost = MonetaryAmount.extract(record.costAmount),
      discount = MonetaryAmount.extract(record.discountAmount),
      tax = MonetaryAmount.extract(record.taxAmount),
      basePrice = MonetaryAmount.extract(record.basePriceAmount),
      calculatedPrice = MonetaryAmount.extract(record.calculatedPriceAmount),
      totalPrice = MonetaryAmount.extract(record.totalPriceAmount),
      notes = record.notes,
      variantOptions = variantOptions,
      modifierOptions = modifierOptions,
      discounts = discounts,
      orderRoutingStatus = inferOrderRoutingStatus(tickets),
      taxRates = taxRates,
      giftCardPass = giftCardPasses,
    )
}
