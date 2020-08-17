package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model.{ ShiftRecord, ShiftUpdate => ShiftUpdateModel }
import io.paytouch.core.entities.{
  Location,
  UserContext,
  UserInfo,
  Shift => ShiftEntity,
  ShiftUpdate => ShiftUpdateEntity,
}

trait ShiftConversions extends ModelConversion[ShiftUpdateEntity, ShiftUpdateModel] {

  def fromRecordsAndOptionsToEntities(
      records: Seq[ShiftRecord],
      users: Map[ShiftRecord, UserInfo],
      locationPerShift: Option[Map[ShiftRecord, Location]],
    ) =
    records.flatMap { record =>
      val location = locationPerShift.flatMap(_.get(record))
      for {
        user <- users.get(record)
      } yield fromRecordToEntity(record, user, location)
    }

  def groupByUserByShift(users: Seq[UserInfo], items: Seq[ShiftRecord]): Map[ShiftRecord, UserInfo] =
    items.flatMap(item => users.find(_.id == item.userId).map(user => (item, user))).toMap

  def fromRecordToEntity(
      record: ShiftRecord,
      user: UserInfo,
      location: Option[Location],
    ): ShiftEntity =
    ShiftEntity(
      id = record.id,
      user = user,
      startDate = record.startDate,
      endDate = record.endDate,
      startTime = record.startTime,
      endTime = record.endTime,
      unpaidBreakMins = record.unpaidBreakMins,
      repeat = record.repeat,
      frequencyInterval = record.frequencyInterval,
      frequencyCount = record.frequencyCount,
      sunday = record.sunday,
      monday = record.monday,
      tuesday = record.tuesday,
      wednesday = record.wednesday,
      thursday = record.thursday,
      friday = record.friday,
      saturday = record.saturday,
      status = record.status,
      bgColor = record.bgColor,
      sendShiftStartNotification = record.sendShiftStartNotification,
      notes = record.notes,
      location = location,
    )

  def fromUpsertionToUpdate(id: UUID, upsertion: ShiftUpdateEntity)(implicit user: UserContext): ShiftUpdateModel =
    ShiftUpdateModel(
      id = Some(id),
      merchantId = Some(user.merchantId),
      userId = upsertion.userId,
      locationId = upsertion.locationId,
      startDate = upsertion.startDate,
      endDate = upsertion.endDate,
      startTime = upsertion.startTime,
      endTime = upsertion.endTime,
      unpaidBreakMins = upsertion.unpaidBreakMins,
      repeat = upsertion.repeat,
      frequencyInterval = upsertion.frequencyInterval,
      frequencyCount = upsertion.frequencyCount,
      sunday = upsertion.sunday,
      monday = upsertion.monday,
      tuesday = upsertion.tuesday,
      wednesday = upsertion.wednesday,
      thursday = upsertion.thursday,
      friday = upsertion.friday,
      saturday = upsertion.saturday,
      status = upsertion.status,
      bgColor = upsertion.bgColor,
      sendShiftStartNotification = upsertion.sendShiftStartNotification,
      notes = upsertion.notes,
    )
}
