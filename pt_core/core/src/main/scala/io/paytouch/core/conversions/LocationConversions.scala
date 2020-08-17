package io.paytouch.core.conversions

import java.util.UUID

import cats.implicits._

import io.paytouch._
import io.paytouch.implicits._

import io.paytouch.core.Availabilities
import io.paytouch.core.data._
import io.paytouch.core.entities._
import io.paytouch.core.services.UtilService

trait LocationConversions extends EntityConversionMerchantContext[model.LocationRecord, Location] {
  def fromRecordToEntity(record: model.LocationRecord)(implicit merchant: MerchantContext): Location =
    fromRecordAndOptionToEntity(record, None, None, None)

  def fromRecordsAndOptionsToEntities(
      records: Seq[model.LocationRecord],
      settings: Option[Map[model.LocationRecord, LocationSettings]],
      taxRates: Option[Map[model.LocationRecord, Seq[TaxRate]]],
      openingHours: Option[Map[model.LocationRecord, Availabilities]],
    )(implicit
      merchant: MerchantContext,
    ) =
    records.map { record =>
      fromRecordAndOptionToEntity(
        record,
        settings.flatMap(_.get(record)),
        taxRates.flatMap(_.get(record)),
        openingHours.map(_.getOrElse(record, Map.empty)),
      )
    }

  def fromRecordAndOptionToEntity(
      record: model.LocationRecord,
      settings: Option[LocationSettings],
      taxRates: Option[Seq[TaxRate]],
      openingHours: Option[Availabilities],
    )(implicit
      merchant: MerchantContext,
    ): Location =
    Location(
      id = record.id,
      name = record.name,
      email = record.email,
      phoneNumber = record.phoneNumber.getOrElse(""),
      website = record.website,
      address = Address(
        line1 = record.addressLine1,
        line2 = record.addressLine2,
        city = record.city,
        state = record.state,
        country = record.country,
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
        postalCode = record.postalCode,
      ),
      timezone = record.timezone,
      currency = merchant.currency,
      active = record.active,
      dummyData = record.dummyData,
      settings = settings,
      taxRates = taxRates,
      openingHours = openingHours,
      coordinates = Coordinates.extract(record.latitude, record.longitude),
    )

  def groupLocationsPerItemId(
      items: Seq[model.SlickItemLocationRecord],
      locations: Seq[Location],
    ): Map[UUID, Seq[Location]] =
    items.groupBy(_.itemId).transform { (_, itemLocations) =>
      itemLocations
        .flatMap(itemLocation => locations.find(_.id == itemLocation.locationId))
    }

  def groupLocationSettingsPerLocation(
      locationSettings: Seq[LocationSettings],
      locations: Seq[model.LocationRecord],
    ): Map[model.LocationRecord, LocationSettings] =
    locations.flatMap { location =>
      locationSettings.find(_.locationId == location.id).map(locationSetting => (location, locationSetting))
    }.toMap

  def fromUpsertionToUpdate(id: UUID, update: LocationUpdate)(implicit user: UserContext): model.LocationUpdate =
    model.LocationUpdate(
      id = Some(id),
      merchantId = Some(user.merchantId),
      name = update.name,
      email = update.email,
      phoneNumber = update.phoneNumber,
      website = update.website,
      addressLine1 = update.address.line1,
      addressLine2 = update.address.line2,
      city = update.address.city,
      state = update.address.state,
      country = update.address.country,
      stateCode = update.address.stateCode,
      countryCode = update.address.countryCode,
      postalCode = update.address.postalCode,
      timezone = update.timezone,
      active = None,
      dummyData = update.dummyData,
      latitude = update.coordinates.map(_.lat),
      longitude = update.coordinates.map(_.lng),
      deletedAt = None,
    )

  def convertToDefaultLocationCreation(
      merchant: model.MerchantRecord,
      userOwner: model.UserRecord,
      merchantCreation: MerchantCreation,
    ): LocationCreation =
    LocationCreation(
      name = merchant.businessName,
      email = Some(userOwner.email),
      address = merchantCreation.address,
      phoneNumber = None,
      website = None,
      timezone = merchant.defaultZoneId,
      openingHours = Availabilities.TwentyFourSeven,
      coordinates = None,
      dummyData = Some(true),
    )
}
