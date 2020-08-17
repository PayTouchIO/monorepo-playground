package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch._

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.UaPassColumns
import io.paytouch.core.data.model.GiftCardPassRecord

class GiftCardPassesTable(tag: Tag)
    extends SlickMerchantTable[GiftCardPassRecord](tag, "gift_card_passes")
       with UaPassColumns {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def lookupId = column[String]("lookup_id")
  def giftCardId = column[UUID]("gift_card_id")
  def orderItemId = column[UUID]("order_item_id")

  def originalAmount = column[BigDecimal]("original_amount")
  def balanceAmount = column[BigDecimal]("balance_amount")
  def iosPassPublicUrl = column[Option[String]]("ios_pass_public_url")
  def androidPassPublicUrl = column[Option[String]]("android_pass_public_url")

  def isCustomAmount = column[Boolean]("is_custom_amount")
  def passInstalledAt = column[Option[ZonedDateTime]]("pass_installed_at")
  def recipientEmail = column[Option[String]]("recipient_email")

  def onlineCode = column[String]("online_code")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def passOptInColumn = passInstalledAt

  def * =
    (
      id,
      merchantId,
      lookupId,
      giftCardId,
      orderItemId,
      originalAmount,
      balanceAmount,
      iosPassPublicUrl,
      androidPassPublicUrl,
      isCustomAmount,
      passInstalledAt,
      recipientEmail,
      onlineCodeProjection,
      createdAt,
      updatedAt,
    ).<>(GiftCardPassRecord.tupled, GiftCardPassRecord.unapply)

  def onlineCodeProjection =
    onlineCode.<>(GiftCardPass.OnlineCode, GiftCardPass.OnlineCode.unapply)
}
