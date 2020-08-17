package io.paytouch.core.entities

import java.time.{ LocalDate, LocalTime }
import java.util.UUID

import io.paytouch.core.data.model.enums.{ FrequencyInterval, ShiftStatus }
import io.paytouch.core.entities.enums.ExposedName

final case class Shift(
    id: UUID,
    user: UserInfo,
    startDate: LocalDate,
    endDate: LocalDate,
    startTime: LocalTime,
    endTime: LocalTime,
    unpaidBreakMins: Option[Int],
    repeat: Boolean,
    frequencyInterval: Option[FrequencyInterval],
    frequencyCount: Option[Int],
    sunday: Option[Boolean],
    monday: Option[Boolean],
    tuesday: Option[Boolean],
    wednesday: Option[Boolean],
    thursday: Option[Boolean],
    friday: Option[Boolean],
    saturday: Option[Boolean],
    status: Option[ShiftStatus],
    bgColor: Option[String],
    sendShiftStartNotification: Boolean,
    notes: Option[String],
    location: Option[Location],
  ) extends ExposedEntity {
  val classShortName = ExposedName.Shift
}

final case class ShiftCreation(
    userId: UUID,
    locationId: UUID,
    startDate: LocalDate,
    endDate: LocalDate,
    startTime: LocalTime,
    endTime: LocalTime,
    unpaidBreakMins: Option[Int],
    repeat: Option[Boolean],
    frequencyInterval: Option[FrequencyInterval],
    frequencyCount: Option[Int],
    sunday: Option[Boolean],
    monday: Option[Boolean],
    tuesday: Option[Boolean],
    wednesday: Option[Boolean],
    thursday: Option[Boolean],
    friday: Option[Boolean],
    saturday: Option[Boolean],
    status: Option[ShiftStatus],
    bgColor: Option[String],
    sendShiftStartNotification: Option[Boolean],
    notes: Option[String],
  ) extends CreationEntity[Shift, ShiftUpdate] {

  def asUpdate =
    ShiftUpdate(
      userId = Some(userId),
      locationId = Some(locationId),
      startDate = Some(startDate),
      endDate = Some(endDate),
      startTime = Some(startTime),
      endTime = Some(endTime),
      unpaidBreakMins = unpaidBreakMins,
      repeat = repeat,
      frequencyInterval = frequencyInterval,
      frequencyCount = frequencyCount,
      sunday = sunday,
      monday = monday,
      tuesday = tuesday,
      wednesday = wednesday,
      thursday = thursday,
      friday = friday,
      saturday = saturday,
      status = status,
      bgColor = bgColor,
      sendShiftStartNotification = sendShiftStartNotification,
      notes = notes,
    )
}

final case class ShiftUpdate(
    userId: Option[UUID],
    locationId: Option[UUID],
    startDate: Option[LocalDate],
    endDate: Option[LocalDate],
    startTime: Option[LocalTime],
    endTime: Option[LocalTime],
    unpaidBreakMins: Option[Int],
    repeat: Option[Boolean],
    frequencyInterval: Option[FrequencyInterval],
    frequencyCount: Option[Int],
    sunday: Option[Boolean],
    monday: Option[Boolean],
    tuesday: Option[Boolean],
    wednesday: Option[Boolean],
    thursday: Option[Boolean],
    friday: Option[Boolean],
    saturday: Option[Boolean],
    status: Option[ShiftStatus],
    bgColor: Option[String],
    sendShiftStartNotification: Option[Boolean],
    notes: Option[String],
  ) extends UpdateEntity[Shift]
