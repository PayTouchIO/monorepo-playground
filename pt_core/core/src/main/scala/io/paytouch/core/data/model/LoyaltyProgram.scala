package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.entities.enums.{ LoyaltyProgramType, PassType }

final case class LoyaltyProgramRecord(
    id: UUID,
    merchantId: UUID,
    `type`: LoyaltyProgramType,
    points: Int,
    spendAmountForPoints: Option[BigDecimal],
    pointsToReward: Int,
    minimumPurchaseAmount: Option[BigDecimal],
    signupRewardEnabled: Option[Boolean],
    signupRewardPoints: Option[Int],
    active: Boolean,
    appleWalletTemplateId: Option[String],
    androidPayTemplateId: Option[String],
    businessName: String,
    templateDetails: Option[String],
    welcomeEmailSubject: Option[String],
    welcomeEmailColor: Option[String],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord {

  def templateIdByPassType(passType: PassType) =
    passType match {
      case PassType.Ios     => appleWalletTemplateId
      case PassType.Android => androidPayTemplateId
    }

}

case class LoyaltyProgramUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    `type`: Option[LoyaltyProgramType],
    points: Option[Int],
    spendAmountForPoints: Option[BigDecimal],
    pointsToReward: Option[Int],
    minimumPurchaseAmount: Option[BigDecimal],
    signupRewardEnabled: Option[Boolean],
    signupRewardPoints: Option[Int],
    active: Option[Boolean],
    appleWalletTemplateId: Option[String],
    androidPayTemplateId: Option[String],
    businessName: Option[String],
    templateDetails: Option[String],
    welcomeEmailSubject: Option[String],
    welcomeEmailColor: Option[String],
  ) extends SlickMerchantUpdate[LoyaltyProgramRecord] {

  def toRecord: LoyaltyProgramRecord = {
    require(merchantId.isDefined, s"Impossible to convert LoyaltyProgramUpdate without a merchant id. [$this]")
    require(`type`.isDefined, s"Impossible to convert LoyaltyProgramUpdate without a `type`. [$this]")
    require(businessName.isDefined, s"Impossible to convert LoyaltyProgramUpdate without a business name. [$this]")
    LoyaltyProgramRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      `type` = `type`.get,
      points = points.getOrElse(0),
      pointsToReward = pointsToReward.getOrElse(0),
      minimumPurchaseAmount = minimumPurchaseAmount,
      spendAmountForPoints = spendAmountForPoints,
      signupRewardEnabled = signupRewardEnabled.orElse(Some(false)),
      signupRewardPoints = signupRewardPoints,
      active = active.getOrElse(false),
      appleWalletTemplateId = appleWalletTemplateId,
      androidPayTemplateId = androidPayTemplateId,
      businessName = businessName.get,
      templateDetails = templateDetails,
      welcomeEmailSubject = welcomeEmailSubject,
      welcomeEmailColor = welcomeEmailColor,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: LoyaltyProgramRecord): LoyaltyProgramRecord =
    LoyaltyProgramRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      `type` = `type`.getOrElse(record.`type`),
      points = points.getOrElse(record.points),
      pointsToReward = pointsToReward.getOrElse(record.pointsToReward),
      minimumPurchaseAmount = minimumPurchaseAmount.orElse(record.minimumPurchaseAmount),
      spendAmountForPoints = spendAmountForPoints.orElse(record.spendAmountForPoints),
      signupRewardEnabled = signupRewardEnabled.orElse(record.signupRewardEnabled),
      signupRewardPoints = signupRewardPoints.orElse(record.signupRewardPoints),
      active = active.getOrElse(record.active),
      appleWalletTemplateId = appleWalletTemplateId.orElse(record.appleWalletTemplateId),
      androidPayTemplateId = androidPayTemplateId.orElse(record.androidPayTemplateId),
      businessName = businessName.getOrElse(record.businessName),
      templateDetails = templateDetails.orElse(record.templateDetails),
      welcomeEmailSubject = welcomeEmailSubject.orElse(record.welcomeEmailSubject),
      welcomeEmailColor = welcomeEmailColor.orElse(record.welcomeEmailColor),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}

object LoyaltyProgramUpdate {
  def empty =
    LoyaltyProgramUpdate(
      id = None,
      merchantId = None,
      `type` = None,
      points = None,
      spendAmountForPoints = None,
      pointsToReward = None,
      minimumPurchaseAmount = None,
      signupRewardEnabled = None,
      signupRewardPoints = None,
      active = None,
      appleWalletTemplateId = None,
      androidPayTemplateId = None,
      businessName = None,
      templateDetails = None,
      welcomeEmailSubject = None,
      welcomeEmailColor = None,
    )
}
