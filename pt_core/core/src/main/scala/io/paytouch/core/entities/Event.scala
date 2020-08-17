package io.paytouch.core.entities

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.entities.enums.{ ExposedName, TrackableAction }
import io.paytouch.core.json.JsonSupport.JValue

final case class Event(
    id: UUID,
    action: TrackableAction,
    `object`: ExposedName,
    data: Option[JValue],
    receivedAt: ZonedDateTime,
  ) extends ExposedEntity {
  val classShortName = ExposedName.Event
}
