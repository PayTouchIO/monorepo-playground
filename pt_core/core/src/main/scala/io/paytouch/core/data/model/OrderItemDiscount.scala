package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.{ Currency, UUID }

import io.paytouch.core.data.model.enums.DiscountType

final case class OrderItemDiscountRecord(
    id: UUID,
    merchantId: UUID,
    orderItemId: UUID,
    discountId: Option[UUID],
    title: Option[String],
    `type`: DiscountType,
    amount: BigDecimal,
    totalAmount: Option[BigDecimal],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord
       with SlickOrderItemRelationRecord

case class OrderItemDiscountUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    orderItemId: Option[UUID],
    discountId: Option[UUID],
    title: Option[String],
    `type`: Option[DiscountType],
    amount: Option[BigDecimal],
    totalAmount: Option[BigDecimal],
  ) extends SlickMerchantUpdate[OrderItemDiscountRecord] {

  def toRecord: OrderItemDiscountRecord = {
    require(merchantId.isDefined, s"Impossible to convert OrderItemDiscountUpdate without a merchant id. [$this]")
    require(orderItemId.isDefined, s"Impossible to convert OrderItemDiscountUpdate without a order item id. [$this]")
    require(`type`.isDefined, s"Impossible to convert OrderItemDiscountUpdate without a type. [$this]")
    require(amount.isDefined, s"Impossible to convert OrderItemDiscountUpdate without a price amount. [$this]")
    OrderItemDiscountRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      orderItemId = orderItemId.get,
      discountId = discountId,
      title = title,
      `type` = `type`.get,
      amount = amount.get,
      totalAmount = totalAmount,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: OrderItemDiscountRecord): OrderItemDiscountRecord =
    OrderItemDiscountRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      orderItemId = orderItemId.getOrElse(record.orderItemId),
      discountId = discountId.orElse(record.discountId),
      title = title.orElse(record.title),
      `type` = `type`.getOrElse(record.`type`),
      amount = amount.getOrElse(record.amount),
      totalAmount = totalAmount.orElse(record.totalAmount),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
