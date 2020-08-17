package io.paytouch.core.resources.loyaltyrewards

import io.paytouch.core.data.model.{ LoyaltyMembershipRecord, LoyaltyRewardRecord }
import io.paytouch.core.entities.{ LoyaltyReward => LoyaltyRewardEntity, _ }
import io.paytouch.core.utils._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

abstract class LoyaltyRewardsFSpec extends FSpec {

  abstract class LoyaltyRewardResourceFSpecContext extends FSpecContext with MultipleLocationFixtures {

    val articleDao = daos.articleDao
    val loyaltyMembershipDao = daos.loyaltyMembershipDao
    val loyaltyRewardDao = daos.loyaltyRewardDao
    val loyaltyRewardProductDao = daos.loyaltyRewardProductDao

    lazy val loyaltyProgram = Factory.loyaltyProgram(merchant).create
    lazy val loyaltyReward = Factory.loyaltyReward(loyaltyProgram).create

    def assertResponse(record: LoyaltyRewardRecord, entity: LoyaltyRewardEntity) = {
      entity.id ==== record.id
      entity.`type` ==== record.`type`
      entity.amount ==== record.amount
    }

    def assertBalanceUpdated(loyaltyMembership: LoyaltyMembershipRecord, expectedPoints: Int) =
      loyaltyMembershipDao.findById(loyaltyMembership.id).await.get.points ==== expectedPoints
  }
}
