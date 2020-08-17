package io.paytouch.ordering.clients.paytouch.core.entities

import java.time.ZoneId
import java.util.{ Currency, UUID }

import io.paytouch.ordering.clients.paytouch.core.entities.enums.Day

final case class Location(
    id: UUID,
    name: String,
    email: Option[String],
    phoneNumber: String,
    website: Option[String],
    active: Boolean,
    address: Address,
    timezone: ZoneId,
    currency: Currency,
    openingHours: Option[Map[Day, Seq[Availability]]],
    coordinates: Option[Coordinates],
    settings: Option[LocationSettings],
  )

final case class LocationSettings(onlineOrder: LocationOnlineOrderSettings)

final case class LocationOnlineOrderSettings(defaultEstimatedPrepTimeInMins: Option[Int])
