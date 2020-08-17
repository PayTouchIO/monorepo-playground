package io.paytouch.core.data.tables

import java.time.{ LocalDate, ZonedDateTime }
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.OAuthAppRecord

class OAuthAppsTable(tag: Tag) extends SlickTable[OAuthAppRecord](tag, "oauth_apps") {

  def id = column[UUID]("id", O.PrimaryKey)
  def clientId = column[UUID]("client_id")
  def clientSecret = column[UUID]("client_secret")
  def name = column[String]("name")
  def redirectUris = column[String]("redirect_uris")
  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      clientId,
      clientSecret,
      name,
      redirectUris,
      createdAt,
      updatedAt,
    ).<>(OAuthAppRecord.tupled, OAuthAppRecord.unapply)

}
