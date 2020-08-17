package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.AdminRecord
import io.paytouch.core.entities.AdminLogin

class AdminsTable(tag: Tag) extends SlickTable[AdminRecord](tag, "admins") {

  def id = column[UUID]("id", O.PrimaryKey)
  def firstName = column[String]("first_name")
  def lastName = column[String]("last_name")
  def email = column[String]("email")
  def password = column[String]("password")
  def lastLoginAt = column[Option[ZonedDateTime]]("last_login_at")
  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def adminLogin =
    (id, email, password).<>((AdminLogin.apply _).tupled, AdminLogin.unapply)

  def * =
    (
      id,
      firstName,
      lastName,
      email,
      password,
      lastLoginAt,
      createdAt,
      updatedAt,
    ).<>(AdminRecord.tupled, AdminRecord.unapply)

}
