package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.entities.enums.{ ExposedName, LoyaltyProgramType }

final case class LoyaltyProgram(
    id: UUID,
    name: String,
    `type`: LoyaltyProgramType,
    points: Int,
    spendAmountForPoints: Option[BigDecimal],
    pointsToReward: Int,
    minimumPurchase: Option[MonetaryAmount],
    signupRewardEnabled: Option[Boolean],
    signupRewardPoints: Option[Int],
    active: Boolean,
    businessName: String,
    templateDetails: Option[String],
    welcomeEmailSubject: Option[String],
    welcomeEmailColor: Option[String],
    rewards: Seq[LoyaltyReward],
    locations: Option[Seq[Location]],
    iconImageUrls: Seq[ImageUrls],
  ) extends ExposedEntity {
  val classShortName = ExposedName.LoyaltyProgram
}

final case class LoyaltyProgramCreation(
    name: String,
    `type`: LoyaltyProgramType,
    points: Int,
    spendAmountForPoints: Option[BigDecimal],
    pointsToReward: Int,
    minimumPurchaseAmount: Option[BigDecimal],
    signupRewardEnabled: Option[Boolean],
    signupRewardPoints: Option[Int],
    active: Option[Boolean],
    businessName: String,
    templateDetails: Option[String],
    welcomeEmailSubject: Option[String],
    welcomeEmailColor: Option[String],
    locationIds: Option[Seq[UUID]],
    rewards: Option[Seq[LoyaltyRewardCreation]],
    imageUploadIds: Option[Seq[UUID]] = None,
  ) extends CreationEntity[LoyaltyProgram, LoyaltyProgramUpdate] {
  def asUpdate =
    LoyaltyProgramUpdate(
      name = Some(name),
      `type` = Some(`type`),
      points = Some(points),
      spendAmountForPoints = spendAmountForPoints,
      pointsToReward = Some(pointsToReward),
      minimumPurchaseAmount = minimumPurchaseAmount,
      signupRewardEnabled = signupRewardEnabled,
      signupRewardPoints = signupRewardPoints,
      active = active,
      businessName = Some(businessName),
      templateDetails = templateDetails,
      welcomeEmailSubject = welcomeEmailSubject,
      welcomeEmailColor = welcomeEmailColor,
      locationIds = locationIds,
      rewards = rewards.map(_.map(_.asUpdate)),
      imageUploadIds = imageUploadIds,
    )
}

final case class LoyaltyProgramUpdate(
    name: Option[String],
    `type`: Option[LoyaltyProgramType],
    points: Option[Int],
    spendAmountForPoints: Option[BigDecimal],
    pointsToReward: Option[Int],
    minimumPurchaseAmount: Option[BigDecimal],
    signupRewardEnabled: Option[Boolean],
    signupRewardPoints: Option[Int],
    active: Option[Boolean],
    businessName: Option[String],
    templateDetails: Option[String],
    welcomeEmailSubject: Option[String],
    welcomeEmailColor: Option[String],
    locationIds: Option[Seq[UUID]],
    rewards: Option[Seq[LoyaltyRewardUpdate]],
    imageUploadIds: Option[Seq[UUID]] = None,
  ) extends UpdateEntity[LoyaltyProgram]

object LoyaltyProgramUpdate {
  def empty: LoyaltyProgramUpdate =
    LoyaltyProgramUpdate(
      name = None,
      `type` = None,
      points = None,
      spendAmountForPoints = None,
      pointsToReward = None,
      minimumPurchaseAmount = None,
      signupRewardEnabled = None,
      signupRewardPoints = None,
      active = None,
      businessName = None,
      templateDetails = None,
      welcomeEmailSubject = None,
      welcomeEmailColor = None,
      locationIds = None,
      rewards = None,
      imageUploadIds = None,
    )
}
