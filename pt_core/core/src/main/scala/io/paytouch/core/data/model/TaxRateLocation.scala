package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class TaxRateLocationRecord(
    id: UUID,
    merchantId: UUID,
    taxRateId: UUID,
    locationId: UUID,
    active: Boolean,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickRecord
       with SlickToggleableRecord
       with SlickItemLocationRecord {
  def itemId = taxRateId
}

case class TaxRateLocationUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    taxRateId: Option[UUID],
    locationId: Option[UUID],
    active: Option[Boolean],
  ) extends SlickMerchantUpdate[TaxRateLocationRecord] {
  def toRecord: TaxRateLocationRecord = {
    require(merchantId.isDefined, s"Impossible to convert TaxRateLocationUpdate without a merchant id. [$this]")
    require(taxRateId.isDefined, s"Impossible to convert TaxRateLocationUpdate without a tax rate id. [$this]")
    require(locationId.isDefined, s"Impossible to convert TaxRateLocationUpdate without a location id. [$this]")
    TaxRateLocationRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      taxRateId = taxRateId.get,
      locationId = locationId.get,
      active = active.getOrElse(true),
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: TaxRateLocationRecord): TaxRateLocationRecord =
    TaxRateLocationRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      taxRateId = taxRateId.getOrElse(record.taxRateId),
      locationId = locationId.getOrElse(record.locationId),
      active = active.getOrElse(record.active),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
