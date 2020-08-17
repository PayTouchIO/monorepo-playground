package io.paytouch.core.resources.loyaltyrewards

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class LoyaltyRewardsListProductsFSpec extends LoyaltyRewardsFSpec {

  "GET /v1/loyalty_rewards.list_products?loyalty_reward_id=<loyalty-reward-id>" in {

    "if request has valid token" in {

      "return list ids of " in new LoyaltyRewardResourceFSpecContext {
        val product0 = Factory.simpleProduct(merchant).create
        Factory.loyaltyRewardProduct(loyaltyReward, product0).create
        val templateProduct1 = Factory.templateProduct(merchant).create
        val variantProduct1A = Factory.variantProduct(merchant, templateProduct1).create
        Factory.loyaltyRewardProduct(loyaltyReward, variantProduct1A).create

        Get(s"/v1/loyalty_rewards.list_products?loyalty_reward_id=${loyaltyReward.id}")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()
          val entities = responseAs[ApiResponse[Seq[Id]]].data
          entities.map(_.id) should containTheSameElementsAs(Seq(product0.id, variantProduct1A.id))
        }
      }

      "if loyaltyReward doesn't belong to same merchant" should {

        "return 404" in new LoyaltyRewardResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorLoyaltyProgram = Factory.loyaltyProgram(competitor).create
          val competitorLoyaltyReward = Factory.loyaltyReward(competitorLoyaltyProgram).create
          val competitorProduct0 = Factory.simpleProduct(competitor).create
          Factory.loyaltyRewardProduct(competitorLoyaltyReward, competitorProduct0).create

          Get(s"/v1/loyalty_rewards.list_products?loyalty_reward_id=${competitorLoyaltyReward.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val entities = responseAs[ApiResponse[Seq[Id]]].data
            entities should beEmpty
          }
        }
      }
    }

    "if request has invalid token" should {

      "be rejected" in new LoyaltyRewardResourceFSpecContext {
        val loyaltyRewardId = UUID.randomUUID
        Get(s"/v1/loyalty_rewards.list_products?loyalty_reward_id=$loyaltyRewardId")
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
