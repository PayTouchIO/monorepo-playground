package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.PasswordResetTokenRecord

class PasswordResetTokensTable(tag: Tag) extends SlickTable[PasswordResetTokenRecord](tag, "password_reset_tokens") {

  def id = column[UUID]("id", O.PrimaryKey)
  def userId = column[UUID]("user_id")
  def key = column[String]("key")

  def expiresAt = column[ZonedDateTime]("expires_at")
  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      userId,
      key,
      expiresAt,
      createdAt,
      updatedAt,
    ).<>(PasswordResetTokenRecord.tupled, PasswordResetTokenRecord.unapply)
}
