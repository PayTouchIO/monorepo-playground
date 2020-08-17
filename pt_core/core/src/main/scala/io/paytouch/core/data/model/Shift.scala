package io.paytouch.core.data.model

import java.time._
import java.util.UUID

import io.paytouch.core.RichLocalTime
import io.paytouch.core.data.model.enums.{ FrequencyInterval, ShiftStatus }

final case class ShiftRecord(
    id: UUID,
    merchantId: UUID,
    userId: UUID,
    locationId: UUID,
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
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord
       with SlickOneToOneWithLocationRecord {

  private val `1dayInMin` = Duration.ofDays(1).toMinutes.toInt

  def totalMins =
    if (startTime isAfter endTime) `1dayInMin` - Duration.between(endTime, startTime).toMinutes.toInt
    else Duration.between(startTime, endTime).toMinutes.toInt

  def covers(days: Set[DayOfWeek]): Boolean = days.forall(covers)

  private def covers(day: DayOfWeek): Boolean = {
    import DayOfWeek._
    day match {
      case SUNDAY    => sunday.exists(identity)
      case MONDAY    => monday.exists(identity)
      case TUESDAY   => tuesday.exists(identity)
      case WEDNESDAY => wednesday.exists(identity)
      case THURSDAY  => thursday.exists(identity)
      case FRIDAY    => friday.exists(identity)
      case SATURDAY  => saturday.exists(identity)
    }
  }

  def isContainedBy(start: LocalTime, end: LocalTime): Boolean =
    if (startTime.isAfter(endTime) && start.nonAfter(end))
      // shift overlapping
      startTime.nonAfter(start) && endTime.nonBefore(end)
    else if (startTime.nonAfter(endTime) && start.isAfter(end))
      false // tc overlapping cannot contain a shift non overlapping
    else startTime.nonBefore(start) && endTime.nonAfter(end)

}

case class ShiftUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
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
  ) extends SlickMerchantUpdate[ShiftRecord] {

  def toRecord: ShiftRecord = {
    require(merchantId.isDefined, s"Impossible to convert ShiftUpdate without a merchant id. [$this]")
    require(userId.isDefined, s"Impossible to convert ShiftUpdate without a user id. [$this]")
    require(locationId.isDefined, s"Impossible to convert ShiftUpdate without a location id. [$this]")
    require(startDate.isDefined, s"Impossible to convert ShiftUpdate without a start date. [$this]")
    require(endDate.isDefined, s"Impossible to convert ShiftUpdate without a end date. [$this]")
    require(startTime.isDefined, s"Impossible to convert ShiftUpdate without a start time. [$this]")
    require(endTime.isDefined, s"Impossible to convert ShiftUpdate without a end time. [$this]")

    ShiftRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      userId = userId.get,
      locationId = locationId.get,
      startDate = startDate.get,
      endDate = endDate.get,
      startTime = startTime.get,
      endTime = endTime.get,
      unpaidBreakMins = unpaidBreakMins,
      repeat = repeat.getOrElse(false),
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
      sendShiftStartNotification = sendShiftStartNotification.getOrElse(false),
      notes = notes,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: ShiftRecord): ShiftRecord =
    ShiftRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      userId = userId.getOrElse(record.userId),
      locationId = locationId.getOrElse(record.locationId),
      startDate = startDate.getOrElse(record.startDate),
      endDate = endDate.getOrElse(record.endDate),
      startTime = startTime.getOrElse(record.startTime),
      endTime = endTime.getOrElse(record.endTime),
      unpaidBreakMins = unpaidBreakMins.orElse(record.unpaidBreakMins),
      repeat = repeat.getOrElse(record.repeat),
      frequencyInterval = frequencyInterval.orElse(record.frequencyInterval),
      frequencyCount = frequencyCount.orElse(record.frequencyCount),
      sunday = sunday.orElse(record.sunday),
      monday = monday.orElse(record.monday),
      tuesday = tuesday.orElse(record.tuesday),
      wednesday = wednesday.orElse(record.wednesday),
      thursday = thursday.orElse(record.thursday),
      friday = friday.orElse(record.friday),
      saturday = saturday.orElse(record.saturday),
      status = status.orElse(record.status),
      bgColor = bgColor.orElse(record.bgColor),
      sendShiftStartNotification = sendShiftStartNotification.getOrElse(record.sendShiftStartNotification),
      notes = notes.orElse(record.notes),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
