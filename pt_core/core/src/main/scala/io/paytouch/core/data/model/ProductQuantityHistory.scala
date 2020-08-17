package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.{ Currency, UUID }

import io.paytouch.core.data.model.enums.QuantityChangeReason

final case class ProductQuantityHistoryRecord(
    id: UUID,
    merchantId: UUID,
    productId: UUID,
    locationId: UUID,
    userId: Option[UUID],
    orderId: Option[UUID],
    date: ZonedDateTime,
    prevQuantityAmount: BigDecimal,
    newQuantityAmount: BigDecimal,
    newStockValueAmount: BigDecimal,
    reason: QuantityChangeReason,
    notes: Option[String],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class ProductQuantityHistoryUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    productId: Option[UUID],
    locationId: Option[UUID],
    userId: Option[UUID],
    orderId: Option[UUID],
    date: Option[ZonedDateTime],
    prevQuantityAmount: Option[BigDecimal],
    newQuantityAmount: Option[BigDecimal],
    newStockValueAmount: Option[BigDecimal],
    reason: Option[QuantityChangeReason],
    notes: Option[String],
  ) extends SlickMerchantUpdate[ProductQuantityHistoryRecord] {

  def updateRecord(record: ProductQuantityHistoryRecord): ProductQuantityHistoryRecord =
    ProductQuantityHistoryRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      productId = productId.getOrElse(record.productId),
      locationId = locationId.getOrElse(record.locationId),
      userId = userId.orElse(record.userId),
      orderId = orderId.orElse(record.orderId),
      date = date.getOrElse(record.date),
      prevQuantityAmount = prevQuantityAmount.getOrElse(record.prevQuantityAmount),
      newQuantityAmount = newQuantityAmount.getOrElse(record.newQuantityAmount),
      newStockValueAmount = newStockValueAmount.getOrElse(record.newStockValueAmount),
      reason = reason.getOrElse(record.reason),
      notes = notes.orElse(record.notes),
      createdAt = record.createdAt,
      updatedAt = now,
    )

  def toRecord: ProductQuantityHistoryRecord = {
    require(merchantId.isDefined, s"Impossible to convert ProductQuantityHistoryUpdate without a merchant id. [$this]")
    require(productId.isDefined, s"Impossible to convert ProductQuantityHistoryUpdate without a product id. [$this]")
    require(locationId.isDefined, s"Impossible to convert ProductQuantityHistoryUpdate without a location id. [$this]")
    require(date.isDefined, s"Impossible to convert ProductQuantityHistoryUpdate without a date [$this]")
    require(
      prevQuantityAmount.isDefined,
      s"Impossible to convert ProductQuantityHistoryUpdate without a previous quantity amount [$this]",
    )
    require(
      newQuantityAmount.isDefined,
      s"Impossible to convert ProductQuantityHistoryUpdate without a new quantity amount [$this]",
    )
    require(
      newStockValueAmount.isDefined,
      s"Impossible to convert ProductQuantityHistoryUpdate without a newStockValueAmount [$this]",
    )
    require(reason.isDefined, s"Impossible to convert ProductQuantityHistoryUpdate without a reason [$this]")
    ProductQuantityHistoryRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      productId = productId.get,
      locationId = locationId.get,
      userId = userId,
      orderId = orderId,
      date = date.get,
      prevQuantityAmount = prevQuantityAmount.get,
      newQuantityAmount = newQuantityAmount.get,
      newStockValueAmount = newStockValueAmount.get,
      reason = reason.get,
      notes = notes,
      createdAt = now,
      updatedAt = now,
    )
  }
}
