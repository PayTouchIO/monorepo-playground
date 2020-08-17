package io.paytouch.core.data.model

import java.time.{ LocalTime, ZonedDateTime }
import java.util.UUID

import io.paytouch.core.data.model.enums.AvailabilityItemType
import io.paytouch.core.entities.Weekdays
import io.paytouch.core.entities.Weekdays.Day

final case class AvailabilityRecord(
    id: UUID,
    merchantId: UUID,
    itemId: UUID,
    itemType: AvailabilityItemType,
    sunday: Boolean,
    monday: Boolean,
    tuesday: Boolean,
    wednesday: Boolean,
    thursday: Boolean,
    friday: Boolean,
    saturday: Boolean,
    start: LocalTime,
    end: LocalTime,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord {

  def isApplicableOn(day: Day) =
    day match {
      case Weekdays.Sunday    => sunday
      case Weekdays.Monday    => monday
      case Weekdays.Tuesday   => tuesday
      case Weekdays.Wednesday => wednesday
      case Weekdays.Thursday  => thursday
      case Weekdays.Friday    => friday
      case Weekdays.Saturday  => saturday
    }
}

case class AvailabilityUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    itemId: Option[UUID],
    itemType: Option[AvailabilityItemType],
    sunday: Option[Boolean],
    monday: Option[Boolean],
    tuesday: Option[Boolean],
    wednesday: Option[Boolean],
    thursday: Option[Boolean],
    friday: Option[Boolean],
    saturday: Option[Boolean],
    start: Option[LocalTime],
    end: Option[LocalTime],
  ) extends SlickMerchantUpdate[AvailabilityRecord] {

  def toRecord: AvailabilityRecord = {
    require(merchantId.isDefined, s"Impossible to convert AvailabilityUpdate without a merchant id. [$this]")
    require(itemId.isDefined, s"Impossible to convert AvailabilityUpdate without an item id. [$this]")
    require(itemType.isDefined, s"Impossible to convert AvailabilityUpdate without an item type. [$this]")
    require(start.isDefined, s"Impossible to convert AvailabilityUpdate without a start. [$this]")
    require(end.isDefined, s"Impossible to convert AvailabilityUpdate without a end. [$this]")
    AvailabilityRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      itemId = itemId.get,
      itemType = itemType.get,
      sunday = sunday.getOrElse(false),
      monday = monday.getOrElse(false),
      tuesday = tuesday.getOrElse(false),
      wednesday = wednesday.getOrElse(false),
      thursday = thursday.getOrElse(false),
      friday = friday.getOrElse(false),
      saturday = saturday.getOrElse(false),
      start = start.get,
      end = end.get,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: AvailabilityRecord): AvailabilityRecord =
    AvailabilityRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      itemId = itemId.getOrElse(record.itemId),
      itemType = itemType.getOrElse(record.itemType),
      sunday = sunday.getOrElse(record.sunday),
      monday = monday.getOrElse(record.monday),
      tuesday = tuesday.getOrElse(record.tuesday),
      wednesday = wednesday.getOrElse(record.wednesday),
      thursday = thursday.getOrElse(record.thursday),
      friday = friday.getOrElse(record.friday),
      saturday = saturday.getOrElse(record.saturday),
      start = start.getOrElse(record.start),
      end = end.getOrElse(record.end),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
