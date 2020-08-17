package io.paytouch.core.resources.loyaltyrewards

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.data.model.RewardRedemptionRecord
import io.paytouch.core.data.model.enums.{ LoyaltyPointsHistoryType, RewardRedemptionStatus }
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class LoyaltyRewardsCancelFSpec extends LoyaltyRewardsFSpec {

  class LoyaltyRewardsCancelFSpecContext extends LoyaltyRewardResourceFSpecContext {
    val rewardRedemptionDao = daos.rewardRedemptionDao

    val newRewardRedemptionId = UUID.randomUUID
    val customer = Factory.globalCustomer().create
    val loyaltyMembership = Factory.loyaltyMembership(customer, loyaltyProgram, points = Some(80)).create

    val rewardRedemption1 = Factory.rewardRedemption(loyaltyMembership, loyaltyReward, points = Some(120)).create
    val rewardRedemption2 = Factory.rewardRedemption(loyaltyMembership, loyaltyReward, points = Some(120)).create

    Factory.loyaltyPointsHistory(loyaltyMembership, 320, LoyaltyPointsHistoryType.Spend).create
    Factory
      .loyaltyPointsHistory(
        loyaltyMembership = loyaltyMembership,
        points = -120,
        `type` = LoyaltyPointsHistoryType.RewardRedemption,
        objectId = Some(rewardRedemption1.id),
      )
      .create
    Factory
      .loyaltyPointsHistory(
        loyaltyMembership = loyaltyMembership,
        points = -120,
        `type` = LoyaltyPointsHistoryType.RewardRedemption,
        objectId = Some(rewardRedemption2.id),
      )
      .create

    def assertRedemptionIsCanceled(rewardRedemption: RewardRedemptionRecord) =
      rewardRedemptionDao.findById(rewardRedemption.id).await.get.status ==== RewardRedemptionStatus.Canceled

    def assertRedemptionResponse(record: RewardRedemptionRecord, response: RewardRedemption) = {
      response.id ==== record.id
      response.loyaltyMembershipId ==== record.loyaltyMembershipId
      response.loyaltyRewardId ==== record.loyaltyRewardId
      response.points ==== record.points
      response.status ==== record.status
      response.loyaltyMembership.isDefined must beTrue
      response.loyaltyMembership.get.id === record.loyaltyMembershipId
      response.loyaltyRewardType ==== loyaltyReward.`type`
    }
  }

  "POST /v1/loyalty_rewards.cancel?reward_redemption_id=<reward-redemption-id>" in {

    "if request has valid token" in {

      "if reward redemption id is not valid" should {
        "return 404" in new LoyaltyRewardsCancelFSpecContext {
          val cancellation = Ids(Seq(UUID.randomUUID))

          Post(s"/v1/loyalty_rewards.cancel", cancellation).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
            assertErrorCode("NonAccessibleRewardRedemptionIds")
          }
        }
      }

      "if request is valid" should {
        "return 204" in new LoyaltyRewardsCancelFSpecContext {
          val cancellation = Ids(Seq(rewardRedemption1.id, rewardRedemption2.id, rewardRedemption2.id))

          Post(s"/v1/loyalty_rewards.cancel", cancellation).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val entities = responseAs[ApiResponse[Seq[RewardRedemption]]].data

            entities.size ==== 2
            val entity1 = entities.find(_.id == rewardRedemption1.id).get
            assertRedemptionIsCanceled(rewardRedemption1)
            assertRedemptionResponse(rewardRedemption1.copy(status = RewardRedemptionStatus.Canceled), entity1)

            val entity2 = entities.find(_.id == rewardRedemption2.id).get
            assertRedemptionIsCanceled(rewardRedemption2)
            assertRedemptionResponse(rewardRedemption2.copy(status = RewardRedemptionStatus.Canceled), entity2)

            assertBalanceUpdated(loyaltyMembership, 320)
          }

          // Test idempotency
          Post(s"/v1/loyalty_rewards.cancel", cancellation).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertBalanceUpdated(loyaltyMembership, 320)
          }
        }
      }

    }

    "if request has invalid token" should {

      "be rejected" in new LoyaltyRewardsCancelFSpecContext {
        val cancellation = Ids(Seq(newRewardRedemptionId))

        Post(s"/v1/loyalty_rewards.cancel", cancellation).addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
