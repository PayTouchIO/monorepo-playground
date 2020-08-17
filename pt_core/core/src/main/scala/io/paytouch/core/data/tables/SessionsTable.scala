package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.SessionRecord
import io.paytouch.core.entities.enums.LoginSource

class SessionsTable(tag: Tag) extends SlickTable[SessionRecord](tag, "sessions") {

  def id = column[UUID]("id", O.PrimaryKey)

  def userId = column[UUID]("user_id")
  def jti = column[String]("jti")
  def source = column[LoginSource]("source")
  def adminId = column[Option[UUID]]("admin_id")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * = (id, userId, jti, source, adminId, createdAt, updatedAt).<>(SessionRecord.tupled, SessionRecord.unapply)
}
