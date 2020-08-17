package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.{ Currency, UUID }

import io.paytouch.core.data.model.enums.DiscountType

final case class OrderDiscountRecord(
    id: UUID,
    merchantId: UUID,
    orderId: UUID,
    discountId: Option[UUID],
    title: Option[String],
    `type`: DiscountType,
    amount: BigDecimal,
    totalAmount: Option[BigDecimal],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class OrderDiscountUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    orderId: Option[UUID],
    discountId: Option[UUID],
    title: Option[String],
    `type`: Option[DiscountType],
    amount: Option[BigDecimal],
    totalAmount: Option[BigDecimal],
  ) extends SlickMerchantUpdate[OrderDiscountRecord] {

  def toRecord: OrderDiscountRecord = {
    require(merchantId.isDefined, s"Impossible to convert OrderDiscountUpdate without a merchant id. [$this]")
    require(orderId.isDefined, s"Impossible to convert OrderDiscountUpdate without a order item id. [$this]")
    require(`type`.isDefined, s"Impossible to convert OrderDiscountUpdate without a type. [$this]")
    require(amount.isDefined, s"Impossible to convert OrderDiscountUpdate without a price amount. [$this]")
    OrderDiscountRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      orderId = orderId.get,
      discountId = discountId,
      title = title,
      `type` = `type`.get,
      amount = amount.get,
      totalAmount = totalAmount,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: OrderDiscountRecord): OrderDiscountRecord =
    OrderDiscountRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      orderId = orderId.getOrElse(record.orderId),
      discountId = discountId.orElse(record.discountId),
      title = title.orElse(record.title),
      `type` = `type`.getOrElse(record.`type`),
      amount = amount.getOrElse(record.amount),
      totalAmount = totalAmount.orElse(record.totalAmount),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
