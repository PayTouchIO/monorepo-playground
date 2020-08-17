package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.calculations.LookupIdUtils
import io.paytouch.core.data.model.enums.LoyaltyPointsHistoryType
import io.paytouch.core.data.model.{ LoyaltyMembershipRecord, LoyaltyMembershipUpdate, LoyaltyPointsHistoryUpdate }
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.{ PassItemType, PassType }
import io.paytouch.core.services.PassService

trait LoyaltyMembershipConversions extends LookupIdUtils {

  val passService: PassService

  def fromRecordsToEntities(
      records: Seq[LoyaltyMembershipRecord],
      customerTotalsByCustomers: Map[LoyaltyMembershipRecord, CustomerTotals],
      loyaltyProgramsByCustomers: Map[LoyaltyMembershipRecord, LoyaltyProgram],
    )(implicit
      merchant: MerchantContext,
    ) =
    records.flatMap { record =>
      val zeroTotal = CustomerTotals(record.id, MonetaryAmount(0, merchant), 0)
      loyaltyProgramsByCustomers.get(record).map { loyaltyProgram =>
        val customerTotals = customerTotalsByCustomers.getOrElse(record, zeroTotal)
        fromRecordToEntity(record, customerTotals, loyaltyProgram)
      }
    }

  def fromRecordToEntity(
      record: LoyaltyMembershipRecord,
      customerTotals: CustomerTotals,
      loyaltyProgram: LoyaltyProgram,
    )(implicit
      merchant: MerchantContext,
    ): LoyaltyMembership =
    LoyaltyMembership(
      id = record.id,
      customerId = record.customerId,
      loyaltyProgramId = record.loyaltyProgramId,
      lookupId = record.lookupId,
      points = record.points,
      pointsToNextReward = (loyaltyProgram.pointsToReward - record.points).max(0),
      passPublicUrls = generatePassUrls(record, orderId = None),
      customerOptInAt = record.customerOptInAt,
      merchantOptInAt = record.merchantOptInAt,
      enrolled = record.isEnrolled,
      visits = customerTotals.totalVisits,
      totalSpend = customerTotals.totalSpend,
    )

  def generatePassUrls(record: LoyaltyMembershipRecord, orderId: Option[UUID]): PassUrls =
    generatePassUrls(record.id, record.iosPassPublicUrl, record.androidPassPublicUrl, orderId)

  def generatePassUrls(entity: LoyaltyMembership, orderId: Option[UUID]): PassUrls =
    generatePassUrls(entity.id, entity.passPublicUrls.ios, entity.passPublicUrls.android, orderId)

  private def generatePassUrls(
      id: UUID,
      iosPassPublicUrl: Option[String],
      androidPassPublicUrl: Option[String],
      orderId: Option[UUID],
    ): PassUrls = {
    val iosPassUrl =
      iosPassPublicUrl.map(_ => passService.generateUrl(id, PassType.Ios, PassItemType.LoyaltyMembership, orderId))
    val androidPassUrl = androidPassPublicUrl.map(_ =>
      passService.generateUrl(id, PassType.Android, PassItemType.LoyaltyMembership, orderId),
    )
    val passUrls = PassUrls(ios = iosPassUrl, android = androidPassUrl)
    passUrls
  }

  def toLoyaltyMembershipsUpdate(record: LoyaltyMembershipRecord)(implicit user: UserContext): LoyaltyMembershipUpdate =
    toLoyaltyMembershipsUpdate(record.id, record.lookupId, record.customerId, record.loyaltyProgramId)

  def toLoyaltyMembershipsUpdateWithLookupId(
      customerId: UUID,
      loyaltyProgramId: UUID,
    )(implicit
      user: UserContext,
    ): LoyaltyMembershipUpdate = {
    val id = UUID.randomUUID
    val lookupId = generateLookupId(id)
    toLoyaltyMembershipsUpdate(id, lookupId, customerId, loyaltyProgramId)
  }

  def toLoyaltyMembershipsUpdate(
      id: UUID,
      lookupId: String,
      customerId: UUID,
      loyaltyProgramId: UUID,
    )(implicit
      user: UserContext,
    ): LoyaltyMembershipUpdate =
    LoyaltyMembershipUpdate(
      id = Some(id),
      merchantId = Some(user.merchantId),
      customerId = Some(customerId),
      loyaltyProgramId = Some(loyaltyProgramId),
      lookupId = Some(lookupId),
      iosPassPublicUrl = None,
      androidPassPublicUrl = None,
      points = None,
      customerOptInAt = None,
      merchantOptInAt = None,
    )

  def toSignupBonusHistoryUpdate(loyaltyMembership: LoyaltyMembershipRecord, loyaltyProgram: LoyaltyProgram) =
    LoyaltyPointsHistoryUpdate(
      id = None,
      merchantId = Some(loyaltyMembership.merchantId),
      loyaltyMembershipId = Some(loyaltyMembership.id),
      `type` = Some(LoyaltyPointsHistoryType.SignUpBonus),
      points = loyaltyProgram.signupRewardPoints,
      orderId = None,
      objectId = None,
      objectType = None,
    )
}
