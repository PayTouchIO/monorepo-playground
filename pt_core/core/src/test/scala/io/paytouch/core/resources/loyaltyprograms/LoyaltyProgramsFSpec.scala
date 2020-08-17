package io.paytouch.core.resources.loyaltyprograms

import java.util.UUID

import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.data.model.{ ImageUploadRecord, LoyaltyProgramRecord, LoyaltyRewardRecord }
import io.paytouch.core.entities.{ LoyaltyProgram => LoyaltyProgramEntity, _ }
import io.paytouch.core.utils._

abstract class LoyaltyProgramsFSpec extends FSpec {

  abstract class LoyaltyProgramResourceFSpecContext extends FSpecContext with MultipleLocationFixtures {

    val loyaltyProgramDao = daos.loyaltyProgramDao
    val imageUploadDao = daos.imageUploadDao
    val itemLocationDao = daos.loyaltyProgramLocationDao
    val loyaltyRewardDao = daos.loyaltyRewardDao

    def assertResponse(
        record: LoyaltyProgramRecord,
        entity: LoyaltyProgramEntity,
        locationIds: Option[Seq[UUID]] = None,
        images: Seq[ImageUploadRecord] = Seq.empty,
      ) = {
      entity.id ==== record.id
      entity.name ==== "Default Loyalty Program"
      entity.`type` ==== record.`type`
      entity.points ==== record.points
      entity.spendAmountForPoints ==== record.spendAmountForPoints
      entity.pointsToReward ==== record.pointsToReward
      entity.minimumPurchase.map(_.amount) ==== record.minimumPurchaseAmount
      entity.signupRewardEnabled ==== record.signupRewardEnabled
      entity.signupRewardPoints ==== record.signupRewardPoints
      entity.templateDetails ==== record.templateDetails
      entity.businessName ==== record.businessName
      entity.welcomeEmailSubject ==== record.welcomeEmailSubject
      entity.welcomeEmailColor ==== record.welcomeEmailColor

      entity.locations.map(_.map(_.id)) ==== locationIds
      entity.iconImageUrls.map(_.imageUploadId) ==== images.map(_.id)
    }

    def assertUpdateRewards(loyaltyProgramId: UUID, rewardUpdates: Seq[LoyaltyRewardUpdate]) = {
      val rewardRecords = loyaltyRewardDao.findByLoyaltyProgramId(loyaltyProgramId).await
      rewardUpdates.foreach(update => assertUpdateReward(rewardRecords.find(_.id == update.id).get, update))
    }

    def assertUpdateReward(rewardRecord: LoyaltyRewardRecord, rewardUpdate: LoyaltyRewardUpdate) = {
      if (rewardUpdate.`type`.isDefined) rewardUpdate.`type` ==== Some(rewardRecord.`type`)
      if (rewardUpdate.amount.isDefined) rewardUpdate.amount ==== rewardRecord.amount
    }

    def assertUpdate(loyaltyProgramId: UUID, update: LoyaltyProgramUpdate) = {
      val record = loyaltyProgramDao.findById(loyaltyProgramId).await.get

      if (update.`type`.isDefined) update.`type` ==== Some(record.`type`)
      if (update.points.isDefined) update.points ==== Some(record.points)
      if (update.spendAmountForPoints.isDefined) update.spendAmountForPoints ==== record.spendAmountForPoints
      if (update.pointsToReward.isDefined) update.pointsToReward ==== Some(record.pointsToReward)
      if (update.minimumPurchaseAmount.isDefined) update.minimumPurchaseAmount ==== record.minimumPurchaseAmount
      if (update.signupRewardEnabled.isDefined) update.signupRewardEnabled ==== record.signupRewardEnabled
      if (update.signupRewardPoints.isDefined) update.signupRewardPoints ==== record.signupRewardPoints
      if (update.businessName.isDefined) update.businessName ==== Some(record.businessName)
      if (update.templateDetails.isDefined) update.templateDetails ==== record.templateDetails
      if (update.welcomeEmailSubject.isDefined) update.welcomeEmailSubject ==== record.welcomeEmailSubject
      if (update.welcomeEmailColor.isDefined) update.welcomeEmailColor ==== record.welcomeEmailColor
      if (update.rewards.isDefined) assertUpdateRewards(loyaltyProgramId, update.rewards.get)

      if (update.locationIds.isDefined) {
        val loyaltyProgramLocations = itemLocationDao.findByItemId(record.id).await
        update.locationIds ==== Some(loyaltyProgramLocations.map(_.locationId))
      }

      if (update.imageUploadIds.isDefined) {
        val images = imageUploadDao.findByObjectIds(Seq(record.id)).await
        images.forall(_.objectType == ImageUploadType.LoyaltyProgram) should beTrue
        update.imageUploadIds ==== Some(images.map(_.id))
      }
    }
  }
}
