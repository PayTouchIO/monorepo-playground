package io.paytouch.core.resources.loyaltyrewards

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.data.model.enums.{ LoyaltyPointsHistoryType, RewardRedemptionStatus }
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class LoyaltyRewardsReserveFSpec extends LoyaltyRewardsFSpec {

  class LoyaltyRewardsReserveFSpecContext extends LoyaltyRewardResourceFSpecContext {
    val newRewardRedemptionId = UUID.randomUUID
    val customer = Factory.globalCustomer().create
    val loyaltyMembership = Factory.loyaltyMembership(customer, loyaltyProgram, points = Some(200)).create
    Factory.loyaltyPointsHistory(loyaltyMembership, 200, LoyaltyPointsHistoryType.Spend).create

    val validCreation = random[RewardRedemptionCreation].copy(
      loyaltyMembershipId = loyaltyMembership.id,
      loyaltyRewardId = loyaltyReward.id,
      points = 120,
    )

    def assertRedemptionResponse(
        id: UUID,
        creation: RewardRedemptionCreation,
        response: RewardRedemption,
      ) = {
      response.id ==== id
      response.loyaltyMembershipId ==== creation.loyaltyMembershipId
      response.loyaltyRewardId ==== creation.loyaltyRewardId
      response.points ==== creation.points
      response.status ==== RewardRedemptionStatus.Reserved
      response.orderId ==== None
      response.objectId ==== None
      response.objectType ==== None
      response.loyaltyRewardType ==== loyaltyReward.`type`
      response.loyaltyMembership.isDefined must beTrue
      response.loyaltyMembership.get.id === creation.loyaltyMembershipId
    }
  }

  "POST /v1/loyalty_rewards.reserve?reward_redemption_id=<reward-redemption-id>" in {

    "if request has valid token" in {

      "if loyalty reward id is not valid" should {
        "return 404" in new LoyaltyRewardsReserveFSpecContext {
          val creation = validCreation.copy(loyaltyRewardId = UUID.randomUUID)
          Post(s"/v1/loyalty_rewards.reserve?reward_redemption_id=$newRewardRedemptionId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
            assertErrorCode("NonAccessibleLoyaltyRewardIds")
          }
        }
      }

      "if loyalty membership id is not valid" should {
        "return 404" in new LoyaltyRewardsReserveFSpecContext {
          val creation = validCreation.copy(loyaltyMembershipId = UUID.randomUUID)
          Post(s"/v1/loyalty_rewards.reserve?reward_redemption_id=$newRewardRedemptionId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
            assertErrorCode("NonAccessibleLoyaltyMembershipIds")
          }
        }
      }

      "if a reward redemption with same id already exists" should {
        "return 404" in new LoyaltyRewardsReserveFSpecContext {
          val rewardRedemption = Factory.rewardRedemption(loyaltyMembership, loyaltyReward).create
          Post(s"/v1/loyalty_rewards.reserve?reward_redemption_id=${rewardRedemption.id}", validCreation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
            assertErrorCode("InvalidRewardRedemptionIds")
          }
        }
      }

      "if loyalty membership has not enough points" should {
        "return 400" in new LoyaltyRewardsReserveFSpecContext {
          val creation = validCreation.copy(points = 500)
          Post(s"/v1/loyalty_rewards.reserve?reward_redemption_id=$newRewardRedemptionId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("NotEnoughLoyaltyPoints")
          }
        }
      }

      "if request is valid" should {
        "return 201" in new LoyaltyRewardsReserveFSpecContext {
          Post(s"/v1/loyalty_rewards.reserve?reward_redemption_id=$newRewardRedemptionId", validCreation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            val rewardRedemption = responseAs[ApiResponse[RewardRedemption]].data
            assertRedemptionResponse(newRewardRedemptionId, validCreation, rewardRedemption)
            assertBalanceUpdated(loyaltyMembership, 80)
          }
        }
      }

    }

    "if request has invalid token" should {

      "be rejected" in new LoyaltyRewardsReserveFSpecContext {
        val creation = random[RewardRedemptionCreation]
        Post(s"/v1/loyalty_rewards.reserve?reward_redemption_id=$newRewardRedemptionId", creation)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
