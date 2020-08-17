package io.paytouch.core.entities

import java.time.ZoneId
import java.util.{ Currency, UUID }

import io.paytouch.core.Availabilities
import io.paytouch.core.entities.enums.ExposedName

final case class Location(
    id: UUID,
    name: String,
    email: Option[String],
    phoneNumber: String,
    website: Option[String],
    active: Boolean,
    dummyData: Boolean,
    address: Address,
    timezone: ZoneId,
    currency: Currency,
    settings: Option[LocationSettings],
    taxRates: Option[Seq[TaxRate]],
    openingHours: Option[Availabilities],
    coordinates: Option[Coordinates],
  ) extends ExposedEntity {
  val classShortName = ExposedName.Location
}

final case class LocationCreation(
    name: String,
    email: Option[String],
    phoneNumber: Option[String],
    website: Option[String],
    address: AddressUpsertion = AddressUpsertion.empty,
    timezone: ZoneId,
    openingHours: Availabilities,
    initialOrderNumber: Int = 1,
    coordinates: Option[Coordinates],
    dummyData: Option[Boolean] = None,
  ) extends CreationEntity[Location, LocationUpdate] {
  def asUpdate =
    LocationUpdate(
      name = Some(name),
      email = email,
      phoneNumber = Some(phoneNumber),
      website = Some(website),
      address = address,
      timezone = Some(timezone),
      openingHours = Some(openingHours),
      initialOrderNumber = Some(initialOrderNumber),
      coordinates = coordinates,
      dummyData = dummyData,
    )
}

final case class LocationUpdate(
    name: Option[String],
    email: Option[String],
    phoneNumber: ResettableString,
    website: ResettableString,
    address: AddressUpsertion = AddressUpsertion.empty,
    timezone: Option[ZoneId],
    openingHours: Option[Availabilities],
    initialOrderNumber: Option[Int],
    coordinates: Option[Coordinates],
    dummyData: Option[Boolean] = None,
  ) extends UpdateEntity[Location]

final case class Coordinates(lat: BigDecimal, lng: BigDecimal)

object Coordinates {
  def extract(lat: Option[BigDecimal], lng: Option[BigDecimal]) =
    for {
      lt <- lat
      lg <- lng
    } yield Coordinates(lt, lg)
}
