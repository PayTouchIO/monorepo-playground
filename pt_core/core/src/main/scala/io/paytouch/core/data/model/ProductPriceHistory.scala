package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.{ Currency, UUID }

import io.paytouch.core.data.model.enums.ChangeReason

final case class ProductPriceHistoryRecord(
    id: UUID,
    merchantId: UUID,
    productId: UUID,
    locationId: UUID,
    userId: UUID,
    date: ZonedDateTime,
    prevPriceAmount: BigDecimal,
    newPriceAmount: BigDecimal,
    reason: ChangeReason,
    notes: Option[String],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class ProductPriceHistoryUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    productId: Option[UUID],
    locationId: Option[UUID],
    userId: Option[UUID],
    date: Option[ZonedDateTime],
    prevPriceAmount: Option[BigDecimal],
    newPriceAmount: Option[BigDecimal],
    reason: Option[ChangeReason],
    notes: Option[String],
  ) extends SlickMerchantUpdate[ProductPriceHistoryRecord] {

  def updateRecord(record: ProductPriceHistoryRecord): ProductPriceHistoryRecord =
    ProductPriceHistoryRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      productId = productId.getOrElse(record.productId),
      locationId = locationId.getOrElse(record.locationId),
      userId = userId.getOrElse(record.userId),
      date = date.getOrElse(record.date),
      prevPriceAmount = prevPriceAmount.getOrElse(record.prevPriceAmount),
      newPriceAmount = newPriceAmount.getOrElse(record.newPriceAmount),
      reason = reason.getOrElse(record.reason),
      notes = notes.orElse(record.notes),
      createdAt = record.createdAt,
      updatedAt = now,
    )

  def toRecord: ProductPriceHistoryRecord = {
    require(merchantId.isDefined, s"Impossible to convert ProductPriceHistoryUpdate without a merchant id. [$this]")
    require(productId.isDefined, s"Impossible to convert ProductPriceHistoryUpdate without a product id. [$this]")
    require(locationId.isDefined, s"Impossible to convert ProductPriceHistoryUpdate without a location id. [$this]")
    require(userId.isDefined, s"Impossible to convert ProductPriceHistoryUpdate without a user id. [$this]")
    require(date.isDefined, s"Impossible to convert ProductPriceHistoryUpdate without a date [$this]")
    require(
      prevPriceAmount.isDefined,
      s"Impossible to convert ProductPriceHistoryUpdate without a previous price amount [$this]",
    )
    require(
      newPriceAmount.isDefined,
      s"Impossible to convert ProductPriceHistoryUpdate without a new price amount [$this]",
    )
    require(reason.isDefined, s"Impossible to convert ProductPriceHistoryUpdate without a reason [$this]")
    ProductPriceHistoryRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      productId = productId.get,
      locationId = locationId.get,
      userId = userId.get,
      date = date.get,
      prevPriceAmount = prevPriceAmount.get,
      newPriceAmount = newPriceAmount.get,
      reason = reason.get,
      notes = notes,
      createdAt = now,
      updatedAt = now,
    )
  }
}
