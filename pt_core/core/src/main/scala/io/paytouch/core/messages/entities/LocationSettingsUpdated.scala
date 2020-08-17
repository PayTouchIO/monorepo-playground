package io.paytouch.core.messages.entities

import java.util.UUID

import cats.implicits._

import io.paytouch.core.entities._

final case class LocationSettingsUpdated(eventName: String, payload: EntityPayload[LocationSettingsInfo])
    extends PtNotifierMsg[LocationSettingsInfo]

object LocationSettingsUpdated {
  val eventName = "location_settings_updated"

  def apply(locationId: UUID)(implicit user: UserContext): LocationSettingsUpdated =
    LocationSettingsUpdated(
      eventName,
      EntityPayload(LocationSettingsInfo(locationId), locationId.some),
    )

  def apply(locationSettings: LocationSettings)(implicit user: UserContext): LocationSettingsUpdated =
    LocationSettingsUpdated(
      eventName,
      EntityPayload(LocationSettingsInfo(locationSettings), locationSettings.locationId.some),
    )
}
