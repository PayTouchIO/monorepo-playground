package io.paytouch.core.resources.timecards

import java.time.DayOfWeek._
import java.time._
import java.util.UUID

import io.paytouch.core.data.model.{ ShiftRecord, TimeCardRecord }
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.TimeCardStatus
import io.paytouch.core.utils.{ MultipleLocationFixtures, FixtureDaoFactory => Factory, _ }

abstract class TimeCardsFSpec extends FSpec {

  abstract class TimeCardResourceFSpecContext extends FSpecContext with MultipleLocationFixtures {
    val locationDao = daos.locationDao
    val timeCardDao = daos.timeCardDao
    val userDao = daos.userDao

    def assertResponseById(recordId: UUID, entity: TimeCard) = {
      val record = timeCardDao.findById(recordId).await.get
      assertResponse(entity, record)
    }

    def assertResponse(
        entity: TimeCard,
        record: TimeCardRecord,
        shift: Option[ShiftRecord] = None,
      ) = {
      entity.id ==== record.id
      entity.deltaMins ==== record.deltaMins
      entity.totalMins ==== record.totalMins
      entity.regularMins ==== record.regularMins
      entity.overtimeMins ==== record.overtimeMins
      entity.unpaidBreakMins ==== record.unpaidBreakMins
      entity.notes ==== record.notes
      entity.startAt ==== record.startAt
      entity.endAt ==== record.endAt
      entity.createdAt ==== record.createdAt
      entity.updatedAt ==== record.updatedAt
      entity.status ==== {
        if (record.startAt.isDefined && record.endAt.isDefined) Some(TimeCardStatus.Closed)
        else if (record.endAt.isEmpty) Some(TimeCardStatus.Open)
        else None
      }

      assertUserResponse(entity.user)
      assertLocationResponse(entity.location)
      if (shift.isDefined) entity.shift.map(_.id) ==== record.shiftId
      else entity.shift.isEmpty
    }

    private def assertUserResponse(entity: UserInfo) = {
      val record = userDao.findDeletedById(entity.id).await.get
      entity.id ==== record.id
      entity.firstName ==== record.firstName
      entity.lastName ==== record.lastName
    }

    private def assertLocationResponse(entity: Location) = {
      val record = locationDao.findById(entity.id).await.get
      entity.id ==== record.id
      entity.name ==== record.name
    }

    def assertCreation(
        recordId: UUID,
        creation: TimeCardCreation,
        deltaMins: Int,
        totalMins: Option[Int],
        regularMins: Option[Int] = None,
        overtimeMins: Option[Int] = None,
      ) =
      assertUpdate(recordId, creation.asUpdate, deltaMins, totalMins, regularMins, overtimeMins)

    def assertUpdate(
        recordId: UUID,
        update: TimeCardUpdate,
        deltaMins: Int,
        totalMins: Option[Int],
        regularMins: Option[Int] = None,
        overtimeMins: Option[Int] = None,
      ) = {
      val record = timeCardDao.findById(recordId).await.get

      if (update.userId.isDefined) update.userId ==== Some(record.userId)
      if (update.locationId.isDefined) update.locationId ==== Some(record.locationId)
      if (update.shiftId.isDefined) update.shiftId ==== record.shiftId
      if (update.unpaidBreakMins.isDefined) update.unpaidBreakMins ==== record.unpaidBreakMins
      if (update.notes.isDefined) update.notes ==== record.notes
      if (update.startAt.isDefined) update.startAt.map(_.toUtc) ==== record.startAt.map(_.toUtc)
      if (update.endAt.isDefined) update.endAt.map(_.toUtc) ==== record.endAt.map(_.toUtc)

      if (regularMins.isDefined) regularMins ==== record.regularMins
      if (overtimeMins.isDefined) overtimeMins ==== record.overtimeMins

      deltaMins ==== record.deltaMins
      totalMins ==== record.totalMins
    }
  }

  trait OverlappingShiftFixtures extends CommonShiftFixtures { self: MultipleLocationFixtures =>
    lazy val shiftStartTime = LocalTime.of(20, 0)
    lazy val shiftEndTime = LocalTime.of(5, 0)
    lazy val shiftDurationMinutes = Duration.ofHours(9).toMinutes.toInt // 540
  }

  trait RegularShiftFixtures extends CommonShiftFixtures { self: MultipleLocationFixtures =>
    lazy val shiftStartTime = LocalTime.of(9, 0)
    lazy val shiftEndTime = LocalTime.of(18, 0)
    lazy val shiftDurationMinutes = Duration.ofHours(9).toMinutes.toInt // 540
  }

  trait CommonShiftFixtures { self: MultipleLocationFixtures =>
    def shiftStartTime: LocalTime
    def shiftEndTime: LocalTime
    def shiftDurationMinutes: Int

    val shiftStartDate = LocalDate.of(2017, 1, 1)
    val shiftEndDate = LocalDate.of(2018, 1, 1)
    val shiftDays = Seq(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)
    val shift = Factory
      .shift(
        user,
        rome,
        startDate = Some(shiftStartDate),
        endDate = Some(shiftEndDate),
        startTime = Some(shiftStartTime),
        endTime = Some(shiftEndTime),
        days = shiftDays,
        repeat = Some(true),
      )
      .create

    val tuesdayWithinRange = LocalDate.of(2017, 5, 2)
    val tuesdayOutsideRange = LocalDate.of(2018, 1, 9)
  }
}
