package io.paytouch.core.resources.shifts

import java.util.UUID
import java.time.LocalTime

import org.specs2.matcher.{ Expectable, Matcher }

import io.paytouch.core.data.model.{ LocationRecord, ShiftRecord }
import io.paytouch.core.entities.enums.MerchantSetupSteps
import io.paytouch.core.entities.{ Shift => ShiftEntity, _ }
import io.paytouch.core.utils._

abstract class ShiftsFSpec extends FSpec {

  abstract class ShiftResourceFSpecContext
      extends FSpecContext
         with MultipleLocationFixtures
         with SetupStepsAssertions
         with LocalDateAssertions {
    val shiftDao = daos.shiftDao
    val userDao = daos.userDao

    def assertResponseById(recordId: UUID, entity: ShiftEntity) = {
      val record = shiftDao.findById(recordId).await.get
      assertResponse(record, entity)
    }

    def assertResponse(
        record: ShiftRecord,
        entity: ShiftEntity,
        location: Option[LocationRecord] = None,
      ) = {
      record.id ==== entity.id
      assertUserResponse(record.userId, entity.user)
      record.startDate ==== entity.startDate
      record.endDate ==== entity.endDate
      record.startTime ==== entity.startTime
      record.endTime ==== entity.endTime
      record.unpaidBreakMins ==== entity.unpaidBreakMins
      record.repeat ==== entity.repeat
      record.frequencyInterval ==== entity.frequencyInterval
      record.frequencyCount ==== entity.frequencyCount
      record.sunday ==== entity.sunday
      record.monday ==== entity.monday
      record.tuesday ==== entity.tuesday
      record.wednesday ==== entity.wednesday
      record.thursday ==== entity.thursday
      record.friday ==== entity.friday
      record.saturday ==== entity.saturday
      record.status ==== entity.status
      record.bgColor ==== entity.bgColor
      record.sendShiftStartNotification ==== entity.sendShiftStartNotification
      record.notes ==== entity.notes
      location.map(_.id) ==== entity.location.map(_.id)
    }

    def assertUserResponse(userId: UUID, userEntity: UserInfo) = {
      val userRecord = userDao.findById(userId).await.get
      userId ==== userEntity.id
      userRecord.firstName ==== userEntity.firstName
      userRecord.lastName ==== userEntity.lastName
      userRecord.email ==== userEntity.email
    }

    def assertCreation(recordId: UUID, creation: ShiftCreation) = {
      assertUpdate(recordId, creation.asUpdate)
      assertSetupStepCompleted(merchant, MerchantSetupSteps.ScheduleEmployees)
    }

    def assertUpdate(recordId: UUID, update: ShiftUpdate) = {
      val record = shiftDao.findById(recordId).await.get

      if (update.userId.isDefined) update.userId ==== Some(record.userId)
      if (update.locationId.isDefined) update.locationId ==== Some(record.locationId)
      if (update.startDate.isDefined) update.startDate ==== Some(record.startDate)
      if (update.endDate.isDefined) update.endDate ==== Some(record.endDate)
      if (update.startTime.isDefined) update.startTime.get must beApproxTheSame(record.startTime)
      if (update.endTime.isDefined) update.endTime.get must beApproxTheSame(record.endTime)
      if (update.unpaidBreakMins.isDefined) update.unpaidBreakMins ==== record.unpaidBreakMins
      if (update.repeat.isDefined) update.repeat ==== Some(record.repeat)
      if (update.frequencyInterval.isDefined) update.frequencyInterval ==== record.frequencyInterval
      if (update.frequencyCount.isDefined) update.frequencyCount ==== record.frequencyCount
      if (update.sunday.isDefined) update.sunday ==== record.sunday
      if (update.monday.isDefined) update.monday ==== record.monday
      if (update.tuesday.isDefined) update.tuesday ==== record.tuesday
      if (update.wednesday.isDefined) update.wednesday ==== record.wednesday
      if (update.thursday.isDefined) update.thursday ==== record.thursday
      if (update.friday.isDefined) update.friday ==== record.friday
      if (update.saturday.isDefined) update.saturday ==== record.saturday
      if (update.status.isDefined) update.status ==== record.status
      if (update.bgColor.isDefined) update.bgColor ==== record.bgColor
      if (update.sendShiftStartNotification.isDefined)
        update.sendShiftStartNotification ==== Some(record.sendShiftStartNotification)
      if (update.notes.isDefined) update.notes ==== record.notes
    }
  }
}
