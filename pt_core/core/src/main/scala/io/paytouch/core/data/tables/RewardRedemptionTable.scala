package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.RewardRedemptionRecord
import io.paytouch.core.data.model.enums.{ RewardRedemptionStatus, RewardRedemptionType }
import io.paytouch.core.entities.enums.RewardType

class RewardRedemptionTable(tag: Tag) extends SlickMerchantTable[RewardRedemptionRecord](tag, "reward_redemptions") {

  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")
  def loyaltyRewardId = column[UUID]("loyalty_reward_id")
  def loyaltyRewardType = column[RewardType]("loyalty_reward_type")
  def loyaltyMembershipId = column[UUID]("loyalty_membership_id")
  def points = column[Int]("points")
  def status = column[RewardRedemptionStatus]("status")
  def orderId = column[Option[UUID]]("order_id")
  def objectId = column[Option[UUID]]("object_id")
  def objectType = column[Option[RewardRedemptionType]]("object_type")
  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      loyaltyRewardId,
      loyaltyRewardType,
      loyaltyMembershipId,
      points,
      status,
      orderId,
      objectId,
      objectType,
      createdAt,
      updatedAt,
    ).<>(RewardRedemptionRecord.tupled, RewardRedemptionRecord.unapply)

}
