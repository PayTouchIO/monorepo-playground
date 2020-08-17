package io.paytouch.seeds

import io.paytouch.core.data.model._

import scala.concurrent._

object LoyaltyRewardSeeds extends Seeds {

  lazy val loyaltyRewardDao = daos.loyaltyRewardDao

  def load(loyaltyPrograms: Seq[LoyaltyProgramRecord])(implicit user: UserRecord): Future[Seq[LoyaltyRewardRecord]] = {

    val loyaltyRewards = loyaltyPrograms.randomSample.map { loyaltyProgram =>
      LoyaltyRewardUpdate(
        id = None,
        merchantId = Some(user.merchantId),
        loyaltyProgramId = Some(loyaltyProgram.id),
        `type` = genRewardType.sample,
        amount = genBigDecimal.sample,
      )
    }

    loyaltyRewardDao.bulkUpsert(loyaltyRewards).extractRecords
  }
}
