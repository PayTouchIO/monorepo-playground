package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.OAuthAppSessionRecord

class OAuthAppSessionsTable(tag: Tag) extends SlickMerchantTable[OAuthAppSessionRecord](tag, "oauth_apps_sessions") {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def oauthAppId = column[UUID]("oauth_app_id")
  def sessionId = column[UUID]("session_id")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      oauthAppId,
      sessionId,
      createdAt,
      updatedAt,
    ).<>(OAuthAppSessionRecord.tupled, OAuthAppSessionRecord.unapply)
}
