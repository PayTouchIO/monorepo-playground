package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.LoyaltyRewardProductRecord

class LoyaltyRewardProductsTable(tag: Tag)
    extends SlickMerchantTable[LoyaltyRewardProductRecord](tag, "loyalty_reward_products") {

  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")
  def productId = column[UUID]("product_id")
  def loyaltyRewardId = column[UUID]("loyalty_reward_id")
  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      productId,
      loyaltyRewardId,
      createdAt,
      updatedAt,
    ).<>(LoyaltyRewardProductRecord.tupled, LoyaltyRewardProductRecord.unapply)

}
