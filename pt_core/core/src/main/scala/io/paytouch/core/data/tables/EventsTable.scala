package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.EventRecord
import io.paytouch.core.entities.enums.{ ExposedName, TrackableAction }
import io.paytouch.core.json.JsonSupport.JValue

class EventsTable(tag: Tag) extends SlickMerchantTable[EventRecord](tag, "events") {

  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")

  def action = column[TrackableAction]("action")
  def `object` = column[ExposedName]("object")
  def data = column[Option[JValue]]("data")

  def receivedAt = column[ZonedDateTime]("received_at")
  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      action,
      `object`,
      data,
      receivedAt,
      createdAt,
      updatedAt,
    ).<>(EventRecord.tupled, EventRecord.unapply)
}
