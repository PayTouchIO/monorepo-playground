package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class CustomerLocationRecord(
    id: UUID,
    merchantId: UUID,
    customerId: UUID,
    locationId: UUID,
    totalVisits: Int,
    totalSpendAmount: BigDecimal,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord
       with SlickItemLocationRecord {
  def itemId = customerId
}

case class CustomerLocationUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    customerId: Option[UUID],
    locationId: Option[UUID],
    totalVisits: Option[Int],
    totalSpendAmount: Option[BigDecimal],
  ) extends SlickMerchantUpdate[CustomerLocationRecord] {

  def toRecord: CustomerLocationRecord = {
    require(merchantId.isDefined, s"Impossible to convert CustomerLocationUpdate without a merchant id. [$this]")
    require(customerId.isDefined, s"Impossible to convert CustomerLocationUpdate without a customer id. [$this]")
    require(locationId.isDefined, s"Impossible to convert CustomerLocationUpdate without a location id. [$this]")
    CustomerLocationRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      customerId = customerId.get,
      locationId = locationId.get,
      totalVisits = totalVisits.getOrElse(0),
      totalSpendAmount = totalSpendAmount.getOrElse(0),
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: CustomerLocationRecord): CustomerLocationRecord =
    CustomerLocationRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      customerId = customerId.getOrElse(record.customerId),
      locationId = locationId.getOrElse(record.locationId),
      totalVisits = totalVisits.getOrElse(record.totalVisits),
      totalSpendAmount = totalSpendAmount.getOrElse(record.totalSpendAmount),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
