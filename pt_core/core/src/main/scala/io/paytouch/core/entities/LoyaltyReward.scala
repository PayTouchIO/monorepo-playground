package io.paytouch.core.entities

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.entities.enums.{ ExposedName, RewardType }

final case class LoyaltyReward(
    id: UUID,
    `type`: RewardType,
    amount: Option[BigDecimal],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends ExposedEntity {
  val classShortName = ExposedName.LoyaltyReward
}

final case class LoyaltyRewardCreation(
    id: UUID,
    `type`: RewardType,
    amount: Option[BigDecimal],
  ) extends CreationEntity[LoyaltyReward, LoyaltyRewardUpdate] {
  def asUpdate =
    LoyaltyRewardUpdate(id = id, `type` = Some(`type`), amount = amount)
}

final case class LoyaltyRewardUpdate(
    id: UUID,
    `type`: Option[RewardType],
    amount: Option[BigDecimal],
  ) extends UpdateEntity[LoyaltyReward]
