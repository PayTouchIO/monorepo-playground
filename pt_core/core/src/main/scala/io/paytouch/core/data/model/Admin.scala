package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class AdminRecord(
    id: UUID,
    firstName: String,
    lastName: String,
    email: String,
    password: String,
    lastLoginAt: Option[ZonedDateTime],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickRecord

case class AdminUpdate(
    id: Option[UUID],
    firstName: Option[String],
    lastName: Option[String],
    email: Option[String],
    password: Option[String],
    lastLoginAt: Option[ZonedDateTime],
  ) extends SlickUpdate[AdminRecord] {

  def toRecord: AdminRecord = {
    require(firstName.isDefined, s"Impossible to convert AdminUpdate without a first name. [$this]")
    require(lastName.isDefined, s"Impossible to convert AdminUpdate without a last name. [$this]")
    require(email.isDefined, s"Impossible to convert AdminUpdate without a email. [$this]")
    require(password.isDefined, s"Impossible to convert AdminUpdate without a password. [$this]")
    AdminRecord(
      id = id.getOrElse(UUID.randomUUID),
      firstName = firstName.get,
      lastName = lastName.get,
      email = email.get,
      password = password.get,
      lastLoginAt = lastLoginAt,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: AdminRecord): AdminRecord =
    AdminRecord(
      id = id.getOrElse(record.id),
      firstName = firstName.getOrElse(record.firstName),
      lastName = lastName.getOrElse(record.lastName),
      email = email.getOrElse(record.email),
      password = password.getOrElse(record.password),
      lastLoginAt = lastLoginAt.orElse(record.lastLoginAt),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
