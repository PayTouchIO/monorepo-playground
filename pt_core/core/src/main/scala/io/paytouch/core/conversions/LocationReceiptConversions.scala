package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch._
import io.paytouch.core.data._
import io.paytouch.core.entities._
import io.paytouch.core.services.UtilService

trait LocationReceiptConversions {
  protected def toDefaultLocationReceipt(merchantId: UUID, locationId: UUID): model.LocationReceiptUpdate =
    model.LocationReceiptUpdate.defaultUpdate(merchantId, locationId)

  protected def prepopulateLocationReceipt(
      merchantId: UUID,
      locationId: UUID,
      update: LocationUpdate,
    ): model.LocationReceiptUpdate =
    toDefaultLocationReceipt(merchantId = merchantId, locationId = locationId).copy(
      locationName = update.name,
      addressLine1 = update.address.line1,
      addressLine2 = update.address.line2,
      city = update.address.city,
      state = update.address.state,
      country = update.address.country,
      postalCode = update.address.postalCode,
      phoneNumber = update.phoneNumber,
      websiteUrl = update.website,
    )

  def fromRecordsAndOptionsToEntities(
      records: Seq[model.LocationReceiptRecord],
      locationPerRecord: Map[model.LocationReceiptRecord, Location],
      emailImageUrlsPerRecord: Map[model.LocationReceiptRecord, Seq[ImageUrls]],
      printImageUrlsPerRecord: Map[model.LocationReceiptRecord, Seq[ImageUrls]],
    ): Seq[LocationReceipt] =
    records.flatMap { record =>
      locationPerRecord.get(record).map { location =>
        fromRecordAndOptionsToEntity(
          record,
          location,
          emailImageUrls = emailImageUrlsPerRecord.getOrElse(record, Seq.empty),
          printImageUrls = printImageUrlsPerRecord.getOrElse(record, Seq.empty),
        )
      }
    }

  private def fromRecordAndOptionsToEntity(
      record: model.LocationReceiptRecord,
      location: Location,
      emailImageUrls: Seq[ImageUrls],
      printImageUrls: Seq[ImageUrls],
    ): LocationReceipt =
    LocationReceipt(
      locationId = record.locationId,
      locationName = record.locationName.getOrElse(location.name),
      headerColor = record.headerColor,
      address = Address(
        line1 = record.addressLine1.orElse(location.address.line1),
        line2 = record.addressLine2.orElse(location.address.line2),
        city = record.city.orElse(location.address.city),
        state = record.state.orElse(location.address.state),
        country = record.country.orElse(location.address.country),
        stateData = UtilService
          .Geo
          .addressState(
            record.countryCode.map(CountryCode),
            record.stateCode.map(StateCode),
            record.country.map(CountryName),
            record.state.map(StateName),
          ),
        countryData = UtilService
          .Geo
          .country(
            record.countryCode.map(CountryCode),
            record.country.map(CountryName),
          ),
        postalCode = record.postalCode.orElse(location.address.postalCode),
      ),
      // TODO - ignoring location.phoneNumber defaulted to "" -- to be removed in PP-1189
      phoneNumber = record.phoneNumber.orElse(Option(location.phoneNumber).filterNot(_.isEmpty)),
      websiteUrl = record.websiteUrl.orElse(location.website),
      facebookUrl = record.facebookUrl,
      twitterUrl = record.twitterUrl,
      showCustomText = record.showCustomText,
      customText = record.customText,
      showReturnPolicy = record.showReturnPolicy,
      returnPolicyText = record.returnPolicyText,
      emailImageUrls = emailImageUrls,
      printImageUrls = printImageUrls,
      createdAt = record.createdAt,
      updatedAt = record.updatedAt,
    )

  def fromUpsertionToUpdate(
      locationId: UUID,
      update: LocationReceiptUpdate,
    )(implicit
      user: UserContext,
    ): model.LocationReceiptUpdate =
    model.LocationReceiptUpdate(
      id = None,
      merchantId = Some(user.merchantId),
      locationId = Some(locationId),
      locationName = update.locationName,
      headerColor = update.headerColor,
      addressLine1 = update.address.line1,
      addressLine2 = update.address.line2,
      city = update.address.city,
      state = update.address.state,
      country = update.address.country,
      codes = model.ResettableCodes(
        stateCode = update.address.stateCode,
        countryCode = update.address.countryCode,
      ),
      postalCode = update.address.postalCode,
      phoneNumber = update.phoneNumber,
      websiteUrl = update.websiteUrl,
      facebookUrl = update.facebookUrl,
      twitterUrl = update.twitterUrl,
      showCustomText = update.showCustomText,
      customText = update.customText,
      showReturnPolicy = update.showReturnPolicy,
      returnPolicyText = update.returnPolicyText,
    )
}
