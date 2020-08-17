package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch._
import io.paytouch.core.data._
import io.paytouch.core.entities._
import io.paytouch.core.services.UtilService

trait LocationEmailReceiptConversions extends EntityConversion[model.LocationEmailReceiptRecord, LocationEmailReceipt] {
  def defaultLocationEmailReceipt(locationId: UUID)(implicit user: UserContext) =
    model.LocationEmailReceiptUpdate.defaultUpdate(user.merchantId, locationId)

  override def fromRecordToEntity(record: model.LocationEmailReceiptRecord)(implicit user: UserContext) =
    fromRecordToEntity(record, None)

  def fromRecordsToEntities(
      records: Seq[model.LocationEmailReceiptRecord],
      imageUploads: Map[UUID, Seq[ImageUrls]],
    ): Map[UUID, LocationEmailReceipt] =
    records.map { locationReceipt =>
      (
        locationReceipt.locationId,
        fromRecordToEntity(
          locationReceipt,
          imageUploads.get(locationReceipt.locationId).flatMap(_.headOption),
        ),
      )
    }.toMap

  def fromRecordToEntity(record: model.LocationEmailReceiptRecord, imageUrls: Option[ImageUrls]): LocationEmailReceipt =
    LocationEmailReceipt(
      locationId = record.locationId,
      headerColor = record.headerColor,
      locationName = record.locationName,
      address = Address(
        line1 = record.locationAddressLine1,
        line2 = record.locationAddressLine2,
        city = record.locationCity,
        state = record.locationState,
        country = record.locationCountry,
        stateData = UtilService
          .Geo
          .addressState(
            record.locationCountryCode.map(CountryCode),
            record.locationStateCode.map(StateCode),
            record.locationCountry.map(CountryName),
            record.locationState.map(StateName),
          ),
        countryData = UtilService
          .Geo
          .country(
            record.locationCountryCode.map(CountryCode),
            record.locationCountry.map(CountryName),
          ),
        postalCode = record.locationPostalCode,
      ),
      includeItemDescription = record.includeItemDescription,
      websiteUrl = record.websiteUrl,
      facebookUrl = record.facebookUrl,
      twitterUrl = record.twitterUrl,
      imageUrls = imageUrls,
    )

  def fromUpsertionToUpdate(
      locationId: UUID,
      update: LocationEmailReceiptUpdate,
    )(implicit
      user: UserContext,
    ): model.LocationEmailReceiptUpdate =
    model.LocationEmailReceiptUpdate(
      id = None,
      merchantId = Some(user.merchantId),
      locationId = Some(locationId),
      headerColor = update.headerColor,
      locationName = update.locationName,
      locationAddressLine1 = update.address.line1,
      locationAddressLine2 = update.address.line2,
      locationCity = update.address.city,
      locationState = update.address.state,
      locationCountry = update.address.country,
      locationStateCode = update.address.stateCode,
      locationCountryCode = update.address.countryCode,
      locationPostalCode = update.address.postalCode,
      includeItemDescription = update.includeItemDescription,
      websiteUrl = update.websiteUrl,
      facebookUrl = update.facebookUrl,
      twitterUrl = update.twitterUrl,
    )
}
