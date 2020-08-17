package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.data.model.enums.{ RewardRedemptionStatus, RewardRedemptionType }
import io.paytouch.core.entities.enums.{ ExposedName, RewardType }

final case class RewardRedemptionSync(
    rewardRedemptionId: UUID,
    objectId: UUID,
    objectType: RewardRedemptionType,
  )

final case class RewardRedemption(
    id: UUID,
    loyaltyRewardId: UUID,
    loyaltyMembershipId: UUID,
    points: Int,
    status: RewardRedemptionStatus,
    loyaltyMembership: Option[LoyaltyMembership],
    loyaltyRewardType: RewardType,
    orderId: Option[UUID],
    objectId: Option[UUID],
    objectType: Option[RewardRedemptionType],
  ) extends ExposedEntity {
  val classShortName = ExposedName.RewardRedemption
}

final case class RewardRedemptionCreation(
    loyaltyRewardId: UUID,
    loyaltyMembershipId: UUID,
    points: Int,
  )
