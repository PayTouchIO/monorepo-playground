package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch._
import io.paytouch.core.data._
import io.paytouch.core.entities._
import io.paytouch.core.services.UtilService

trait LocationPrintReceiptConversions extends EntityConversion[model.LocationPrintReceiptRecord, LocationPrintReceipt] {
  def defaultLocationPrintReceipt(locationId: UUID)(implicit user: UserContext) =
    model.LocationPrintReceiptUpdate.defaultUpdate(user.merchantId, locationId)

  def fromRecordToEntity(record: model.LocationPrintReceiptRecord)(implicit user: UserContext) =
    fromRecordToEntity(record, None)

  def fromRecordsToEntities(
      records: Seq[model.LocationPrintReceiptRecord],
      imageUploads: Map[UUID, Seq[ImageUrls]],
    ): Map[UUID, LocationPrintReceipt] =
    records.map { locationReceipt =>
      (
        locationReceipt.locationId,
        fromRecordToEntity(
          locationReceipt,
          imageUploads.get(locationReceipt.locationId).flatMap(_.headOption),
        ),
      )
    }.toMap

  def fromRecordToEntity(record: model.LocationPrintReceiptRecord, imageUrls: Option[ImageUrls]) =
    LocationPrintReceipt(
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
      imageUrls = imageUrls,
    )

  def fromUpsertionToUpdate(
      locationId: UUID,
      update: LocationPrintReceiptUpdate,
    )(implicit
      user: UserContext,
    ): model.LocationPrintReceiptUpdate =
    model.LocationPrintReceiptUpdate(
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
    )
}
