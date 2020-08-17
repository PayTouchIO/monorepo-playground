package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.{ Currency, UUID }

import cats.implicits._

import io.paytouch.core.data.model.enums._

final case class OrderItemRecord(
    id: UUID,
    merchantId: UUID,
    orderId: UUID,
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
    notes: Option[String],
    giftCardPassRecipientEmail: Option[String],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord {
  def deriveUpdateFromPreviousState(other: OrderItemRecord): OrderItemUpdate =
    OrderItemUpdate(
      id = id.some,
      merchantId = if (merchantId != other.merchantId) merchantId.some else None,
      orderId = if (orderId != other.orderId) orderId.some else None,
      productId = if (productId != other.productId) productId else None,
      productName = if (productName != other.productName) productName else None,
      productDescription = if (productDescription != other.productDescription) productDescription else None,
      productType = if (productType != other.productType) productType else None,
      quantity = if (quantity != other.quantity) quantity else None,
      unit = if (unit != other.unit) unit else None,
      paymentStatus = if (paymentStatus != other.paymentStatus) paymentStatus else None,
      priceAmount = if (priceAmount != other.priceAmount) priceAmount else None,
      costAmount = if (costAmount != other.costAmount) costAmount else None,
      discountAmount = if (discountAmount != other.discountAmount) discountAmount else None,
      taxAmount = if (taxAmount != other.taxAmount) taxAmount else None,
      basePriceAmount = if (basePriceAmount != other.basePriceAmount) basePriceAmount else None,
      calculatedPriceAmount = if (calculatedPriceAmount != other.calculatedPriceAmount) calculatedPriceAmount else None,
      totalPriceAmount = if (totalPriceAmount != other.totalPriceAmount) totalPriceAmount else None,
      notes = if (notes != other.notes) notes else None,
      giftCardPassRecipientEmail =
        if (giftCardPassRecipientEmail != other.giftCardPassRecipientEmail) giftCardPassRecipientEmail else None,
    )

}

case class OrderItemUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    orderId: Option[UUID],
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
    notes: Option[String],
    giftCardPassRecipientEmail: Option[String],
  ) extends SlickMerchantUpdate[OrderItemRecord] {
  def toRecord: OrderItemRecord = {
    require(merchantId.isDefined, s"Impossible to convert OrderItemUpdate without a merchant id. [$this]")
    require(orderId.isDefined, s"Impossible to convert OrderItemUpdate without a order id. [$this]")

    OrderItemRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      orderId = orderId.get,
      productId = productId,
      productName = productName,
      productDescription = productDescription,
      productType = productType,
      quantity = quantity,
      unit = unit,
      paymentStatus = paymentStatus,
      priceAmount = priceAmount,
      costAmount = costAmount,
      discountAmount = discountAmount,
      taxAmount = taxAmount,
      basePriceAmount = basePriceAmount,
      calculatedPriceAmount = calculatedPriceAmount,
      totalPriceAmount = totalPriceAmount,
      notes = notes,
      giftCardPassRecipientEmail = giftCardPassRecipientEmail,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: OrderItemRecord): OrderItemRecord =
    OrderItemRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      orderId = orderId.getOrElse(record.orderId),
      productId = productId.orElse(record.productId),
      productName = productName.orElse(record.productName),
      productDescription = productDescription.orElse(record.productDescription),
      productType = productType.orElse(record.productType),
      quantity = quantity.orElse(record.quantity),
      unit = unit.orElse(record.unit),
      paymentStatus = paymentStatus.orElse(record.paymentStatus),
      priceAmount = priceAmount.orElse(record.priceAmount),
      costAmount = costAmount.orElse(record.costAmount),
      discountAmount = discountAmount.orElse(record.discountAmount),
      taxAmount = taxAmount.orElse(record.taxAmount),
      basePriceAmount = basePriceAmount.orElse(record.basePriceAmount),
      calculatedPriceAmount = calculatedPriceAmount.orElse(record.calculatedPriceAmount),
      totalPriceAmount = totalPriceAmount.orElse(record.totalPriceAmount),
      giftCardPassRecipientEmail = giftCardPassRecipientEmail.orElse(record.giftCardPassRecipientEmail),
      notes = notes.orElse(record.notes),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}

object OrderItemUpdate {
  def empty(id: UUID): OrderItemUpdate =
    OrderItemUpdate(
      id.some,
      merchantId = none,
      orderId = none,
      productId = none,
      productName = none,
      productDescription = none,
      productType = none,
      quantity = none,
      unit = none,
      paymentStatus = none,
      priceAmount = none,
      costAmount = none,
      discountAmount = none,
      taxAmount = none,
      basePriceAmount = none,
      calculatedPriceAmount = none,
      totalPriceAmount = none,
      notes = none,
      giftCardPassRecipientEmail = none,
    )
}
