package io.paytouch.core.conversions

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.{ ShiftRecord, TimeCardRecord, TimeCardUpdate => TimeCardUpdateModel }
import io.paytouch.core.entities.enums.TimeCardStatus
import io.paytouch.core.entities.{
  Location,
  Shift,
  UserContext,
  UserInfo,
  TimeCard => TimeCardEntity,
  TimeCardUpdate => TimeCardUpdateEntity,
}
import io.paytouch.core.utils.UtcTime

trait TimeCardConversions {

  def fromRecordsToEntities(
      records: Seq[TimeCardRecord],
      users: Seq[UserInfo],
      locations: Seq[Location],
      shifts: Option[Seq[Shift]],
    ): Seq[TimeCardEntity] =
    records.flatMap { record =>
      val shift = shifts.getOrElse(Seq.empty).find(s => record.shiftId.contains(s.id))
      for {
        user <- users.find(_.id == record.userId)
        location <- locations.find(_.id == record.locationId)
      } yield fromRecordToEntity(record, user, location, shift)
    }

  def fromRecordToEntity(
      record: TimeCardRecord,
      user: UserInfo,
      location: Location,
      shift: Option[Shift],
    ): TimeCardEntity =
    TimeCardEntity(
      id = record.id,
      user = user,
      location = location,
      shift = shift,
      deltaMins = record.deltaMins,
      totalMins = record.totalMins,
      regularMins = record.regularMins,
      overtimeMins = record.overtimeMins,
      unpaidBreakMins = record.unpaidBreakMins,
      notes = record.notes,
      status = inferTimeCardStatus(record.startAt, record.endAt),
      startAt = record.startAt,
      endAt = record.endAt,
      createdAt = record.createdAt,
      updatedAt = record.updatedAt,
    )

  private def inferTimeCardStatus(
      startAt: Option[ZonedDateTime],
      endAt: Option[ZonedDateTime],
    ): Option[TimeCardStatus] =
    (startAt, endAt) match {
      case (Some(_), Some(_)) => Some(TimeCardStatus.Closed)
      case (_, None)          => Some(TimeCardStatus.Open)
      case _                  => None
    }

  def fromUpsertionToUpdate(
      id: UUID,
      update: TimeCardUpdateEntity,
      shift: Option[ShiftRecord],
    )(implicit
      user: UserContext,
    ): TimeCardUpdateModel = {

    val totalMinsInTimeCard = update.totalMinutes
    val minsCalculations = TimeCardEntity.calculateResultsInMins(totalMinsInTimeCard, shift)

    TimeCardUpdateModel(
      id = Some(id),
      merchantId = Some(user.merchantId),
      userId = update.userId,
      locationId = update.locationId,
      shiftId = shift.map(_.id),
      deltaMins = minsCalculations.map(_.deltaMins),
      totalMins = totalMinsInTimeCard,
      regularMins = minsCalculations.map(_.regularMins),
      overtimeMins = minsCalculations.map(_.overtimeMins),
      unpaidBreakMins = update.unpaidBreakMins,
      notes = update.notes,
      startAt = update.startAt,
      endAt = update.endAt,
    )
  }

  def toOpenTimeCardUpdate(userId: UUID, locationId: UUID)(implicit user: UserContext): TimeCardUpdateEntity =
    TimeCardUpdateEntity
      .empty
      .copy(
        userId = Some(userId),
        locationId = Some(locationId),
        startAt = Some(UtcTime.now),
      )

  def toCloseTimeCardUpdate(timeCard: TimeCardRecord)(implicit user: UserContext): TimeCardUpdateEntity =
    TimeCardUpdateEntity
      .empty
      .copy(
        startAt = timeCard.startAt,
        endAt = Some(UtcTime.now),
      )
}
