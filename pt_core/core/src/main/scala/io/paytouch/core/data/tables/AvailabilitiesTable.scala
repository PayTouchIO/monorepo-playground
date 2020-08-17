package io.paytouch.core.data.tables

import java.time.{ LocalTime, ZonedDateTime }
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.AvailabilityRecord
import io.paytouch.core.data.model.enums.AvailabilityItemType

class AvailabilitiesTable(tag: Tag) extends SlickMerchantTable[AvailabilityRecord](tag, "availabilities") {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def itemId = column[UUID]("item_id")
  def itemType = column[AvailabilityItemType]("item_type")

  def sunday = column[Boolean]("sunday")
  def monday = column[Boolean]("monday")
  def tuesday = column[Boolean]("tuesday")
  def wednesday = column[Boolean]("wednesday")
  def thursday = column[Boolean]("thursday")
  def friday = column[Boolean]("friday")
  def saturday = column[Boolean]("saturday")

  def start = column[LocalTime]("start")
  def end = column[LocalTime]("end")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      itemId,
      itemType,
      sunday,
      monday,
      tuesday,
      wednesday,
      thursday,
      friday,
      saturday,
      start,
      end,
      createdAt,
      updatedAt,
    ).<>(AvailabilityRecord.tupled, AvailabilityRecord.unapply)
}
