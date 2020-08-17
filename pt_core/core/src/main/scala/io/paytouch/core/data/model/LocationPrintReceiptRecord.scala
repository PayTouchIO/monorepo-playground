package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.entities.ResettableString

final case class LocationPrintReceiptRecord(
    id: UUID,
    merchantId: UUID,
    locationId: UUID,
    headerColor: Option[String],
    locationName: Option[String],
    locationAddressLine1: Option[String],
    locationAddressLine2: Option[String],
    locationCity: Option[String],
    locationState: Option[String],
    locationCountry: Option[String],
    locationStateCode: Option[String],
    locationCountryCode: Option[String],
    locationPostalCode: Option[String],
    includeItemDescription: Boolean,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord
       with SlickOneToOneWithLocationRecord

case class LocationPrintReceiptUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    locationId: Option[UUID],
    headerColor: Option[String],
    locationName: Option[String],
    locationAddressLine1: Option[String],
    locationAddressLine2: ResettableString,
    locationCity: Option[String],
    locationState: Option[String],
    locationCountry: Option[String],
    locationStateCode: Option[String],
    locationCountryCode: Option[String],
    locationPostalCode: Option[String],
    includeItemDescription: Option[Boolean],
  ) extends SlickMerchantUpdate[LocationPrintReceiptRecord] {
  def toRecord: LocationPrintReceiptRecord = {
    require(merchantId.isDefined, s"Impossible to convert LocationPrintReceiptUpdate without a merchant id. [$this]")
    require(locationId.isDefined, s"Impossible to convert LocationPrintReceiptUpdate without a location id. [$this]")

    LocationPrintReceiptRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      locationId = locationId.get,
      headerColor = headerColor,
      locationName = locationName,
      locationAddressLine1 = locationAddressLine1,
      locationAddressLine2 = locationAddressLine2,
      locationCity = locationCity,
      locationState = locationState,
      locationCountry = locationCountry,
      locationStateCode = locationStateCode,
      locationCountryCode = locationCountryCode,
      locationPostalCode = locationPostalCode,
      includeItemDescription = includeItemDescription.getOrElse(false),
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: LocationPrintReceiptRecord): LocationPrintReceiptRecord =
    LocationPrintReceiptRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      locationId = locationId.getOrElse(record.locationId),
      headerColor = headerColor.orElse(record.headerColor),
      locationName = locationName.orElse(record.locationName),
      locationAddressLine1 = locationAddressLine1.orElse(record.locationAddressLine1),
      locationAddressLine2 = locationAddressLine2.getOrElse(record.locationAddressLine2),
      locationCity = locationCity.orElse(record.locationCity),
      locationState = locationState.orElse(record.locationState),
      locationCountry = locationCountry.orElse(record.locationCountry),
      locationStateCode = locationStateCode.orElse(record.locationStateCode),
      locationCountryCode = locationCountryCode.orElse(record.locationCountryCode),
      locationPostalCode = locationPostalCode.orElse(record.locationPostalCode),
      includeItemDescription = includeItemDescription.getOrElse(record.includeItemDescription),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}

object LocationPrintReceiptUpdate {
  def defaultUpdate(merchantId: UUID, locationId: UUID) =
    LocationPrintReceiptUpdate(
      id = None,
      merchantId = Some(merchantId),
      locationId = Some(locationId),
      headerColor = None,
      locationName = None,
      locationAddressLine1 = None,
      locationAddressLine2 = None,
      locationCity = None,
      locationState = None,
      locationCountry = None,
      locationStateCode = None,
      locationCountryCode = None,
      locationPostalCode = None,
      includeItemDescription = None,
    )
}
