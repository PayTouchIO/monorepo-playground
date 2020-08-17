package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.LoyaltyProgramRecord
import io.paytouch.core.entities.enums.LoyaltyProgramType

class LoyaltyProgramsTable(tag: Tag) extends SlickMerchantTable[LoyaltyProgramRecord](tag, "loyalty_programs") {

  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")
  def `type` = column[LoyaltyProgramType]("type")
  def points = column[Int]("points", O.Default(0))
  def spendAmountForPoints = column[Option[BigDecimal]]("spend_amount_for_points")
  def pointsToReward = column[Int]("points_to_reward", O.Default(0))
  def minimumPurchaseAmount = column[Option[BigDecimal]]("minimum_purchase_amount")
  def signupRewardEnabled = column[Option[Boolean]]("signup_reward_enabled", O.Default(Some(false)))
  def signupRewardPoints = column[Option[Int]]("signup_reward_points")
  def active = column[Boolean]("active", O.Default(false))
  def appleWalletTemplateId = column[Option[String]]("apple_wallet_template_id")
  def androidPayTemplateId = column[Option[String]]("android_pay_template_id")
  def businessName = column[String]("business_name")
  def templateDetails = column[Option[String]]("template_details_text")
  def welcomeEmailSubject = column[Option[String]]("welcome_email_subject")
  def welcomeEmailColor = column[Option[String]]("welcome_email_color")
  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      `type`,
      points,
      spendAmountForPoints,
      pointsToReward,
      minimumPurchaseAmount,
      signupRewardEnabled,
      signupRewardPoints,
      active,
      appleWalletTemplateId,
      androidPayTemplateId,
      businessName,
      templateDetails,
      welcomeEmailSubject,
      welcomeEmailColor,
      createdAt,
      updatedAt,
    ).<>(LoyaltyProgramRecord.tupled, LoyaltyProgramRecord.unapply)

}
