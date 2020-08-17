package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.{ Currency, UUID }

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.LoyaltyRewardRecord
import io.paytouch.core.entities.enums.RewardType

class LoyaltyRewardsTable(tag: Tag) extends SlickMerchantTable[LoyaltyRewardRecord](tag, "loyalty_rewards") {

  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")
  def loyaltyProgramId = column[UUID]("loyalty_program_id")
  def `type` = column[RewardType]("type")
  def amount = column[Option[BigDecimal]]("amount")
  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      loyaltyProgramId,
      `type`,
      amount,
      createdAt,
      updatedAt,
    ).<>(LoyaltyRewardRecord.tupled, LoyaltyRewardRecord.unapply)

}
