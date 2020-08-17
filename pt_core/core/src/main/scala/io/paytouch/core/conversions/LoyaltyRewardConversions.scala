package io.paytouch.core.conversions

import io.paytouch.core.data.model.LoyaltyRewardRecord
import io.paytouch.core.entities.{ MerchantContext, LoyaltyReward => LoyaltyRewardEntity }

trait LoyaltyRewardConversions extends EntityConversionMerchantContext[LoyaltyRewardRecord, LoyaltyRewardEntity] {

  def fromRecordToEntity(record: LoyaltyRewardRecord)(implicit merchant: MerchantContext): LoyaltyRewardEntity =
    LoyaltyRewardEntity(
      id = record.id,
      `type` = record.`type`,
      amount = record.amount,
      createdAt = record.createdAt,
      updatedAt = record.updatedAt,
    )

}
