package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.LoyaltyPointsHistoryRecord
import io.paytouch.core.data.model.enums.{ LoyaltyPointsHistoryRelatedType, LoyaltyPointsHistoryType }

class LoyaltyPointsHistoryTable(tag: Tag)
    extends SlickMerchantTable[LoyaltyPointsHistoryRecord](tag, "loyalty_points_history") {

  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")
  def loyaltyMembershipId = column[UUID]("loyalty_membership_id")
  def `type` = column[LoyaltyPointsHistoryType]("type")
  def points = column[Int]("points")
  def orderId = column[Option[UUID]]("order_id")
  def objectId = column[Option[UUID]]("object_id")
  def objectType = column[Option[LoyaltyPointsHistoryRelatedType]]("object_type")
  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      loyaltyMembershipId,
      `type`,
      points,
      orderId,
      objectId,
      objectType,
      createdAt,
      updatedAt,
    ).<>(LoyaltyPointsHistoryRecord.tupled, LoyaltyPointsHistoryRecord.unapply)

}
