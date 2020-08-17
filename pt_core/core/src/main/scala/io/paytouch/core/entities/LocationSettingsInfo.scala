package io.paytouch.core.entities

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.entities.enums._

final case class LocationSettingsInfo(
    locationId: UUID,
  ) extends ExposedEntity {
  val classShortName = ExposedName.LocationSettingsInfo
}

object LocationSettingsInfo {
  def apply(locationSettings: LocationSettings): LocationSettingsInfo =
    LocationSettingsInfo(
      locationId = locationSettings.locationId,
    )
}
