package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model.{ LoyaltyProgramRecord, LoyaltyProgramUpdate => LoyaltyProgramUpdateModel }
import io.paytouch.core.entities.{
  ImageUrls,
  Location,
  LoyaltyMembership,
  LoyaltyReward,
  MerchantContext,
  MonetaryAmount,
  UserContext,
  LoyaltyProgram => LoyaltyProgramEntity,
  LoyaltyProgramUpdate => LoyaltyProgramUpdateEntity,
}

trait LoyaltyProgramConversions extends EntityConversionMerchantContext[LoyaltyProgramRecord, LoyaltyProgramEntity] {
  def fromRecordToEntity(record: LoyaltyProgramRecord)(implicit merchant: MerchantContext): LoyaltyProgramEntity =
    fromRecordAndOptionsToEntity(record, Seq.empty, None, Seq.empty)

  def groupLoyaltyProgramsPerCustomer(
      loyaltyMemberships: Seq[LoyaltyMembership],
      loyaltyPrograms: Seq[LoyaltyProgramEntity],
    ) =
    loyaltyMemberships.groupBy(_.customerId).transform { (_, customerLoyaltyProgs) =>
      customerLoyaltyProgs
        .flatMap(loyaltyMembership => loyaltyPrograms.find(_.id == loyaltyMembership.loyaltyProgramId))
    }

  def fromRecordsAndOptionsToEntities(
      items: Seq[LoyaltyProgramRecord],
      rewardsPerLoyaltyProgram: Map[LoyaltyProgramRecord, Seq[LoyaltyReward]],
      locationsPerLoyaltyProgram: Option[Map[LoyaltyProgramRecord, Seq[Location]]],
      imageUrlsPerGiftCard: Map[LoyaltyProgramRecord, Seq[ImageUrls]],
    )(implicit
      merchant: MerchantContext,
    ) =
    items.map { item =>
      val locations = locationsPerLoyaltyProgram.map(_.getOrElse(item, Seq.empty))
      val rewards = rewardsPerLoyaltyProgram.getOrElse(item, Seq.empty)
      val imageUrls = imageUrlsPerGiftCard.getOrElse(item, Seq.empty)
      fromRecordAndOptionsToEntity(item, rewards, locations, imageUrls)
    }

  def fromRecordAndOptionsToEntity(
      record: LoyaltyProgramRecord,
      rewards: Seq[LoyaltyReward],
      locations: Option[Seq[Location]],
      imageUrls: Seq[ImageUrls],
    )(implicit
      merchant: MerchantContext,
    ): LoyaltyProgramEntity =
    LoyaltyProgramEntity(
      id = record.id,
      name = "Default Loyalty Program",
      `type` = record.`type`,
      points = record.points,
      spendAmountForPoints = record.spendAmountForPoints,
      pointsToReward = record.pointsToReward,
      minimumPurchase = MonetaryAmount.extract(record.minimumPurchaseAmount, merchant),
      signupRewardEnabled = record.signupRewardEnabled,
      signupRewardPoints = record.signupRewardPoints,
      active = record.active,
      businessName = record.businessName,
      templateDetails = record.templateDetails,
      welcomeEmailSubject = record.welcomeEmailSubject,
      welcomeEmailColor = record.welcomeEmailColor,
      rewards = rewards,
      locations = locations,
      iconImageUrls = imageUrls,
    )

  def fromUpsertionToUpdate(
      id: UUID,
      upsertion: LoyaltyProgramUpdateEntity,
    )(implicit
      user: UserContext,
    ): LoyaltyProgramUpdateModel =
    LoyaltyProgramUpdateModel(
      id = Some(id),
      merchantId = Some(user.merchantId),
      `type` = upsertion.`type`,
      points = upsertion.points,
      spendAmountForPoints = upsertion.spendAmountForPoints,
      pointsToReward = upsertion.pointsToReward,
      minimumPurchaseAmount = upsertion.minimumPurchaseAmount,
      signupRewardEnabled = upsertion.signupRewardEnabled,
      signupRewardPoints = upsertion.signupRewardPoints,
      active = upsertion.active,
      appleWalletTemplateId = None,
      androidPayTemplateId = None,
      businessName = upsertion.businessName,
      templateDetails = upsertion.templateDetails,
      welcomeEmailSubject = upsertion.welcomeEmailSubject,
      welcomeEmailColor = upsertion.welcomeEmailColor,
    )
}
