package io.paytouch.core.filters

import java.time.ZonedDateTime

import io.paytouch.core.entities.enums.{ ExposedName, TrackableAction }

final case class EventFilters(
    updatedSince: ZonedDateTime,
    action: Option[TrackableAction],
    `object`: Option[ExposedName],
  ) extends BaseFilters
