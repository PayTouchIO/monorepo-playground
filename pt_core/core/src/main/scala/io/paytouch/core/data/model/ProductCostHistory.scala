package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.ChangeReason

final case class ProductCostHistoryRecord(
    id: UUID,
    merchantId: UUID,
    productId: UUID,
    locationId: UUID,
    userId: UUID,
    date: ZonedDateTime,
    prevCostAmount: BigDecimal,
    newCostAmount: BigDecimal,
    reason: ChangeReason,
    notes: Option[String],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class ProductCostHistoryUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    productId: Option[UUID],
    locationId: Option[UUID],
    userId: Option[UUID],
    date: Option[ZonedDateTime],
    prevCostAmount: Option[BigDecimal],
    newCostAmount: Option[BigDecimal],
    reason: Option[ChangeReason],
    notes: Option[String],
  ) extends SlickMerchantUpdate[ProductCostHistoryRecord] {

  def updateRecord(record: ProductCostHistoryRecord): ProductCostHistoryRecord =
    ProductCostHistoryRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      productId = productId.getOrElse(record.productId),
      locationId = locationId.getOrElse(record.locationId),
      userId = userId.getOrElse(record.userId),
      date = date.getOrElse(record.date),
      prevCostAmount = prevCostAmount.getOrElse(record.prevCostAmount),
      newCostAmount = newCostAmount.getOrElse(record.newCostAmount),
      reason = reason.getOrElse(record.reason),
      notes = notes.orElse(record.notes),
      createdAt = record.createdAt,
      updatedAt = now,
    )

  def toRecord: ProductCostHistoryRecord = {
    require(merchantId.isDefined, s"Impossible to convert ProductCostHistoryUpdate without a merchant id. [$this]")
    require(productId.isDefined, s"Impossible to convert ProductCostHistoryUpdate without a product id. [$this]")
    require(locationId.isDefined, s"Impossible to convert ProductCostHistoryUpdate without a location id. [$this]")
    require(userId.isDefined, s"Impossible to convert ProductCostHistoryUpdate without a user id. [$this]")
    require(date.isDefined, s"Impossible to convert ProductCostHistoryUpdate without a date [$this]")
    require(
      prevCostAmount.isDefined,
      s"Impossible to convert ProductCostHistoryUpdate without a previous cost amount [$this]",
    )
    require(
      newCostAmount.isDefined,
      s"Impossible to convert ProductCostHistoryUpdate without a new cost amount [$this]",
    )
    require(reason.isDefined, s"Impossible to convert ProductCostHistoryUpdate without a reason [$this]")
    ProductCostHistoryRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      productId = productId.get,
      locationId = locationId.get,
      userId = userId.get,
      date = date.get,
      prevCostAmount = prevCostAmount.get,
      newCostAmount = newCostAmount.get,
      reason = reason.get,
      notes = notes,
      createdAt = now,
      updatedAt = now,
    )
  }
}
