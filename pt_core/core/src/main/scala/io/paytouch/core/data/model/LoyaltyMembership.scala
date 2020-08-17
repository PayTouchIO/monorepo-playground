package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class LoyaltyMembershipRecord(
    id: UUID,
    merchantId: UUID,
    customerId: UUID,
    loyaltyProgramId: UUID,
    lookupId: String,
    iosPassPublicUrl: Option[String],
    androidPassPublicUrl: Option[String],
    points: Int,
    customerOptInAt: Option[ZonedDateTime],
    merchantOptInAt: Option[ZonedDateTime],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord {

  def isEnrolled = customerOptInAt.isDefined || merchantOptInAt.isDefined
}

case class LoyaltyMembershipUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    customerId: Option[UUID],
    loyaltyProgramId: Option[UUID],
    lookupId: Option[String],
    iosPassPublicUrl: Option[String],
    androidPassPublicUrl: Option[String],
    points: Option[Int],
    customerOptInAt: Option[ZonedDateTime],
    merchantOptInAt: Option[ZonedDateTime],
  ) extends SlickMerchantUpdate[LoyaltyMembershipRecord] {

  def toRecord: LoyaltyMembershipRecord = {
    require(merchantId.isDefined, s"Impossible to convert LoyaltyMembershipUpdate without a merchant id. [$this]")
    require(customerId.isDefined, s"Impossible to convert LoyaltyMembershipUpdate without a customer id. [$this]")
    require(
      loyaltyProgramId.isDefined,
      s"Impossible to convert LoyaltyMembershipUpdate without a loyalty program id. [$this]",
    )
    require(lookupId.isDefined, s"Impossible to convert LoyaltyMembershipUpdate without a lookup id. [$this]")
    LoyaltyMembershipRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      customerId = customerId.get,
      loyaltyProgramId = loyaltyProgramId.get,
      lookupId = lookupId.get,
      iosPassPublicUrl = iosPassPublicUrl,
      androidPassPublicUrl = androidPassPublicUrl,
      points = points.getOrElse(0),
      customerOptInAt = customerOptInAt,
      merchantOptInAt = merchantOptInAt,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: LoyaltyMembershipRecord): LoyaltyMembershipRecord =
    LoyaltyMembershipRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      customerId = customerId.getOrElse(record.customerId),
      loyaltyProgramId = loyaltyProgramId.getOrElse(record.loyaltyProgramId),
      lookupId = lookupId.getOrElse(record.lookupId),
      iosPassPublicUrl = iosPassPublicUrl.orElse(record.iosPassPublicUrl),
      androidPassPublicUrl = androidPassPublicUrl.orElse(record.androidPassPublicUrl),
      points = points.getOrElse(record.points),
      customerOptInAt = customerOptInAt.orElse(record.customerOptInAt),
      merchantOptInAt = merchantOptInAt.orElse(record.merchantOptInAt),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
