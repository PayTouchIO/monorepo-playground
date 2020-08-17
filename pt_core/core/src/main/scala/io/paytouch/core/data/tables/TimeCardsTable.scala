package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.{ LocationIdColumn, OneToOneLocationColumns }
import io.paytouch.core.data.model.TimeCardRecord

class TimeCardsTable(tag: Tag)
    extends SlickMerchantTable[TimeCardRecord](tag, "time_cards")
       with OneToOneLocationColumns
       with LocationIdColumn {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def userId = column[UUID]("user_id")
  def locationId = column[UUID]("location_id")
  def shiftId = column[Option[UUID]]("shift_id")

  def deltaMins = column[Int]("delta_mins")
  def totalMins = column[Option[Int]]("total_mins")
  def regularMins = column[Option[Int]]("regular_mins")
  def overtimeMins = column[Option[Int]]("overtime_mins")
  def unpaidBreakMins = column[Option[Int]]("unpaid_break_mins")
  def notes = column[Option[String]]("notes")

  def startAt = column[Option[ZonedDateTime]]("start_at")
  def endAt = column[Option[ZonedDateTime]]("end_at")
  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      userId,
      locationId,
      shiftId,
      deltaMins,
      totalMins,
      regularMins,
      overtimeMins,
      unpaidBreakMins,
      notes,
      startAt,
      endAt,
      createdAt,
      updatedAt,
    ).<>(TimeCardRecord.tupled, TimeCardRecord.unapply)
}
