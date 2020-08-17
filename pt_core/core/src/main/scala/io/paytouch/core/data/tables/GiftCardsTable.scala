package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.ActiveColumn
import io.paytouch.core.data.model.GiftCardRecord

class GiftCardsTable(tag: Tag) extends SlickMerchantTable[GiftCardRecord](tag, "gift_cards") with ActiveColumn {
  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def productId = column[UUID]("product_id")

  def amounts = column[Seq[BigDecimal]]("amounts")
  def businessName = column[String]("business_name")
  def templateDetails = column[Option[String]]("template_details_text")
  def appleWalletTemplateId = column[Option[String]]("apple_wallet_template_id")
  def androidPayTemplateId = column[Option[String]]("android_pay_template_id")
  def active = column[Boolean]("active")
  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      productId,
      amounts,
      businessName,
      templateDetails,
      appleWalletTemplateId,
      androidPayTemplateId,
      active,
      createdAt,
      updatedAt,
    ).<>(GiftCardRecord.tupled, GiftCardRecord.unapply)
}
