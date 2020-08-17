package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.TimeOffCardRecord
import io.paytouch.core.data.model.enums.TimeOffType

class TimeOffCardsTable(tag: Tag) extends SlickMerchantTable[TimeOffCardRecord](tag, "time_off_cards") {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def userId = column[UUID]("user_id")

  def paid = column[Boolean]("paid")
  def `type` = column[Option[TimeOffType]]("type")
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
      paid,
      `type`,
      notes,
      startAt,
      endAt,
      createdAt,
      updatedAt,
    ).<>(TimeOffCardRecord.tupled, TimeOffCardRecord.unapply)
}
