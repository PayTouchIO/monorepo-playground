package io.paytouch.core.entities

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.TimeOffType
import io.paytouch.core.entities.enums.ExposedName

final case class TimeOffCard(
    id: UUID,
    user: UserInfo,
    paid: Boolean,
    `type`: Option[TimeOffType],
    notes: Option[String],
    startAt: Option[ZonedDateTime],
    endAt: Option[ZonedDateTime],
  ) extends ExposedEntity {
  val classShortName = ExposedName.TimeOffCard
}

final case class TimeOffCardCreation(
    userId: UUID,
    paid: Boolean,
    `type`: Option[TimeOffType],
    notes: Option[String],
    startAt: Option[ZonedDateTime],
    endAt: Option[ZonedDateTime],
  ) extends CreationEntity[TimeOffCard, TimeOffCardUpdate] {

  def asUpdate =
    TimeOffCardUpdate(
      userId = Some(userId),
      paid = Some(paid),
      `type` = `type`,
      notes = notes,
      startAt = startAt,
      endAt = endAt,
    )
}

final case class TimeOffCardUpdate(
    userId: Option[UUID],
    paid: Option[Boolean],
    `type`: Option[TimeOffType],
    notes: Option[String],
    startAt: Option[ZonedDateTime],
    endAt: Option[ZonedDateTime],
  ) extends UpdateEntity[TimeOffCard]
