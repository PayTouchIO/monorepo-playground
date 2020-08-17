package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.{ Currency, UUID }

import io.paytouch.core.entities.enums.RewardType

final case class LoyaltyRewardRecord(
    id: UUID,
    merchantId: UUID,
    loyaltyProgramId: UUID,
    `type`: RewardType,
    amount: Option[BigDecimal],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class LoyaltyRewardUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    loyaltyProgramId: Option[UUID],
    `type`: Option[RewardType],
    amount: Option[BigDecimal],
  ) extends SlickMerchantUpdate[LoyaltyRewardRecord] {

  def toRecord: LoyaltyRewardRecord = {
    require(merchantId.isDefined, s"Impossible to convert LoyaltyRewardUpdate without a merchant id. [$this]")
    require(
      loyaltyProgramId.isDefined,
      s"Impossible to convert LoyaltyRewardUpdate without a loyalty program id. [$this]",
    )
    require(`type`.isDefined, s"Impossible to convert LoyaltyRewardUpdate without a `type`. [$this]")
    LoyaltyRewardRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      loyaltyProgramId = loyaltyProgramId.get,
      `type` = `type`.get,
      amount = amount,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: LoyaltyRewardRecord): LoyaltyRewardRecord =
    LoyaltyRewardRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      loyaltyProgramId = loyaltyProgramId.getOrElse(record.loyaltyProgramId),
      `type` = `type`.getOrElse(record.`type`),
      amount = amount.orElse(record.amount),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
