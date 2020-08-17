package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.UaPassColumns
import io.paytouch.core.data.model.LoyaltyMembershipRecord

class LoyaltyMembershipsTable(tag: Tag)
    extends SlickMerchantTable[LoyaltyMembershipRecord](tag, "loyalty_memberships")
       with UaPassColumns {

  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")
  def customerId = column[UUID]("customer_id")
  def loyaltyProgramId = column[UUID]("loyalty_program_id")
  def lookupId = column[String]("lookup_id")
  def iosPassPublicUrl = column[Option[String]]("ios_pass_public_url")
  def androidPassPublicUrl = column[Option[String]]("android_pass_public_url")
  def points = column[Int]("points")
  def customerOptInAt = column[Option[ZonedDateTime]]("customer_opt_in_at")
  def merchantOptInAt = column[Option[ZonedDateTime]]("merchant_opt_in_at")
  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def passOptInColumn = customerOptInAt

  def * =
    (
      id,
      merchantId,
      customerId,
      loyaltyProgramId,
      lookupId,
      iosPassPublicUrl,
      androidPassPublicUrl,
      points,
      customerOptInAt,
      merchantOptInAt,
      createdAt,
      updatedAt,
    ).<>(LoyaltyMembershipRecord.tupled, LoyaltyMembershipRecord.unapply)

}
