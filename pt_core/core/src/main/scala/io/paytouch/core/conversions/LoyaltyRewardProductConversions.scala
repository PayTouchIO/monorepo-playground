package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model.LoyaltyRewardProductUpdate
import io.paytouch.core.entities.UserContext

trait LoyaltyRewardProductConversions {

  def toLoyaltyRewardProductUpdates(
      loyaltyRewardId: UUID,
      productId: UUID,
    )(implicit
      user: UserContext,
    ): LoyaltyRewardProductUpdate =
    LoyaltyRewardProductUpdate(
      id = None,
      merchantId = Some(user.merchantId),
      productId = Some(productId),
      loyaltyRewardId = Some(loyaltyRewardId),
    )

  def toLoyaltyRewardProductUpdates(
      loyaltyRewardId: UUID,
      productIds: Seq[UUID],
    )(implicit
      user: UserContext,
    ): Seq[LoyaltyRewardProductUpdate] =
    productIds.map(toLoyaltyRewardProductUpdates(loyaltyRewardId, _))
}
