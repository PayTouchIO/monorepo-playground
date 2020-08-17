package io.paytouch.core.entities

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.entities.enums.ExposedName

final case class Seating(
    id: UUID,
    tableId: UUID,
    tableName: String,
    tableShortName: String,
    tableFloorPlanName: String,
    tableSectionName: String,
    reservationId: Option[UUID],
    guestName: Option[String],
    guestsCount: Int,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends ExposedEntity {
  val classShortName = ExposedName.Seating
}
