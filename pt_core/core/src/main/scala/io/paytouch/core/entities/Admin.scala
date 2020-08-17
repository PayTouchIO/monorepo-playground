package io.paytouch.core.entities

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.AdminRecord
import io.paytouch.core.entities.enums.ExposedName

final case class Admin(
    id: UUID,
    firstName: String,
    lastName: String,
    email: String,
    lastLoginAt: Option[ZonedDateTime],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends ExposedEntity {
  val classShortName = ExposedName.Admin

}

final case class AdminLogin(
    id: UUID,
    email: String,
    encryptedPassword: String,
  )

object AdminLogin {
  def fromRecord(record: AdminRecord) =
    AdminLogin(id = record.id, email = record.email, encryptedPassword = record.password)
}

final case class AdminContext(id: UUID)

final case class AdminCreation(
    firstName: String,
    lastName: String,
    password: String,
    email: String,
  ) extends CreationEntity[Admin, AdminUpdate] {
  def asUpdate =
    AdminUpdate(
      firstName = Some(firstName),
      lastName = Some(lastName),
      password = Some(password),
      email = Some(email),
    )
}

final case class AdminUpdate(
    firstName: Option[String],
    lastName: Option[String],
    password: Option[String],
    email: Option[String],
  ) extends UpdateEntity[Admin]

final case class GoogleIdToken(idToken: String)
