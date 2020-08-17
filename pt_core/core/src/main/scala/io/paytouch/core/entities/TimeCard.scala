package io.paytouch.core.entities

import java.time.{ Duration, ZonedDateTime }
import java.util.UUID
import scala.math.{ max, min }

import io.paytouch.core.entities.enums.{ ExposedName, TimeCardStatus }
import io.paytouch.core.data.model.ShiftRecord

final case class TimeCard(
    id: UUID,
    user: UserInfo,
    location: Location,
    shift: Option[Shift],
    deltaMins: Int,
    totalMins: Option[Int],
    regularMins: Option[Int],
    overtimeMins: Option[Int],
    unpaidBreakMins: Option[Int],
    notes: Option[String],
    status: Option[TimeCardStatus],
    startAt: Option[ZonedDateTime],
    endAt: Option[ZonedDateTime],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends ExposedEntity {
  val classShortName = ExposedName.TimeCard
}

object TimeCard {
  final case class Results(
      deltaMins: Int,
      regularMins: Int,
      overtimeMins: Int,
    )

  def calculateResultsInMinsFromShift(minsInTimeCard: Int, shift: ShiftRecord) = {
    val deltaMins = minsInTimeCard - shift.totalMins
    Results(
      deltaMins = deltaMins,
      regularMins = min(minsInTimeCard, shift.totalMins),
      overtimeMins = max(deltaMins, 0),
    )
  }

  def calculateResultsInMinsWithoutShift(minsInTimeCard: Int) =
    Results(
      deltaMins = 0,
      regularMins = minsInTimeCard,
      overtimeMins = 0,
    )

  def calculateResultsInMins(maybeMinsInTimeCard: Option[Int], maybeShift: Option[ShiftRecord]): Option[Results] =
    (maybeMinsInTimeCard, maybeShift) match {
      case (Some(minsInTimeCard), Some(shift)) =>
        Some(calculateResultsInMinsFromShift(minsInTimeCard, shift))
      case (Some(minsInTimeCard), None) =>
        Some(calculateResultsInMinsWithoutShift(minsInTimeCard))
      case _ => None
    }
}

final case class TimeCardClock(pin: String, locationId: UUID)

final case class TimeCardCreation(
    userId: UUID,
    locationId: UUID,
    shiftId: Option[UUID],
    unpaidBreakMins: Option[Int],
    notes: Option[String],
    startAt: Option[ZonedDateTime],
    endAt: Option[ZonedDateTime],
  ) extends CreationEntity[TimeCard, TimeCardUpdate] {

  def asUpdate: TimeCardUpdate =
    TimeCardUpdate(
      userId = Some(userId),
      locationId = Some(locationId),
      shiftId = shiftId,
      unpaidBreakMins = unpaidBreakMins,
      notes = notes,
      startAt = startAt,
      endAt = endAt,
    )
}

final case class TimeCardUpdate(
    userId: Option[UUID],
    locationId: Option[UUID],
    shiftId: Option[UUID],
    unpaidBreakMins: Option[Int],
    notes: Option[String],
    startAt: Option[ZonedDateTime],
    endAt: Option[ZonedDateTime],
  ) extends UpdateEntity[TimeCard] {

  val totalMinutes: Option[Int] =
    for {
      start <- startAt
      end <- endAt
    } yield Duration.between(start, end).toMinutes.toInt
}

object TimeCardUpdate {
  def empty =
    TimeCardUpdate(
      userId = None,
      locationId = None,
      shiftId = None,
      unpaidBreakMins = None,
      notes = None,
      startAt = None,
      endAt = None,
    )
}
