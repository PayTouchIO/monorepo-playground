package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.OAuthCodeRecord

class OAuthCodesTable(tag: Tag) extends SlickMerchantTable[OAuthCodeRecord](tag, "oauth_codes") {

  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")
  def userId = column[UUID]("user_id")
  def oauthAppId = column[UUID]("oauth_app_id")
  def code = column[UUID]("code")
  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      userId,
      oauthAppId,
      code,
      createdAt,
      updatedAt,
    ).<>(OAuthCodeRecord.tupled, OAuthCodeRecord.unapply)

}
