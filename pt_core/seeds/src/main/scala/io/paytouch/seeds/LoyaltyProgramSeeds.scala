package io.paytouch.seeds

import io.paytouch.core.data.model.{ LoyaltyProgramRecord, LoyaltyProgramUpdate, UserRecord }
import io.paytouch.seeds.IdsProvider._

import scala.concurrent._

object LoyaltyProgramSeeds extends Seeds {

  lazy val loyaltyProgramDao = daos.loyaltyProgramDao

  def load(implicit user: UserRecord): Future[Seq[LoyaltyProgramRecord]] = {
    val loyaltyProgramIds = loyaltyProgramIdsPerEmail(user.email)

    val coinFlip = genBoolean.instance

    val loyaltyPrograms = loyaltyProgramIds.map { loyaltyProgramId =>
      LoyaltyProgramUpdate
        .empty
        .copy(
          id = Some(loyaltyProgramId),
          merchantId = Some(user.merchantId),
          `type` = genLoyaltyProgramType.sample,
          points = genInt.sample,
          spendAmountForPoints = if (coinFlip) Some(genBigDecimal.instance) else None,
          pointsToReward = genInt.sample,
          minimumPurchaseAmount = if (coinFlip) Some(genBigDecimal.instance) else None,
          signupRewardEnabled = if (coinFlip) Some(genBoolean.instance) else None,
          signupRewardPoints = if (coinFlip) Some(genInt.instance) else None,
          active = Some(true),
          businessName = Some("Business Name"),
        )
    }

    loyaltyProgramDao.bulkUpsert(loyaltyPrograms).extractRecords
  }
}
