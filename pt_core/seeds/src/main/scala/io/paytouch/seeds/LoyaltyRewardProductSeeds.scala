package io.paytouch.seeds

import io.paytouch.core.data.model._

import scala.concurrent._

object LoyaltyRewardProductSeeds extends Seeds {

  lazy val loyaltyRewardProductDao = daos.loyaltyRewardProductDao

  def load(
      loyaltyRewards: Seq[LoyaltyRewardRecord],
      products: Seq[ArticleRecord],
    )(implicit
      user: UserRecord,
    ): Future[Seq[LoyaltyRewardProductRecord]] = {

    val loyaltyRewardProducts = loyaltyRewards.flatMap { loyaltyReward =>
      products.randomAtLeast(15).map { product =>
        LoyaltyRewardProductUpdate(
          id = None,
          merchantId = Some(user.merchantId),
          productId = Some(product.id),
          loyaltyRewardId = Some(loyaltyReward.id),
        )
      }
    }

    loyaltyRewardProductDao.bulkUpsertByRelIds(loyaltyRewardProducts).extractRecords
  }
}
