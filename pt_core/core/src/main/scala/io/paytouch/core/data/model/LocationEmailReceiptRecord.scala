package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.entities.ResettableString

final case class LocationEmailReceiptRecord(
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
    websiteUrl: Option[String],
    facebookUrl: Option[String],
    twitterUrl: Option[String],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord
       with SlickOneToOneWithLocationRecord

case class LocationEmailReceiptUpdate(
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
    websiteUrl: Option[String],
    facebookUrl: Option[String],
    twitterUrl: Option[String],
  ) extends SlickMerchantUpdate[LocationEmailReceiptRecord] {
  def toRecord: LocationEmailReceiptRecord = {
    require(merchantId.isDefined, s"Impossible to convert LocationEmailReceiptUpdate without a merchant id. [$this]")
    require(locationId.isDefined, s"Impossible to convert LocationEmailReceiptUpdate without a location id. [$this]")

    LocationEmailReceiptRecord(
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
      websiteUrl = websiteUrl,
      facebookUrl = facebookUrl,
      twitterUrl = twitterUrl,
      createdAt = now,
      updatedAt = now,
    )
  }

  final override def updateRecord(record: LocationEmailReceiptRecord): LocationEmailReceiptRecord =
    LocationEmailReceiptRecord(
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
      locationCountryCode = locationCountry.orElse(record.locationCountryCode),
      locationPostalCode = locationPostalCode.orElse(record.locationPostalCode),
      includeItemDescription = includeItemDescription.getOrElse(record.includeItemDescription),
      websiteUrl = websiteUrl.orElse(record.websiteUrl),
      facebookUrl = facebookUrl.orElse(record.facebookUrl),
      twitterUrl = twitterUrl.orElse(record.twitterUrl),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}

object LocationEmailReceiptUpdate {
  def defaultUpdate(merchantId: UUID, locationId: UUID) =
    LocationEmailReceiptUpdate(
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
      websiteUrl = None,
      facebookUrl = None,
      twitterUrl = None,
    )
}
