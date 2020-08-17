package io.paytouch.core.resources.loyaltyrewards

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ UtcTime, FixtureDaoFactory => Factory }

class LoyaltyRewardsAssignProductsFSpec extends LoyaltyRewardsFSpec {

  "POST /v1/loyalty_rewards.assign_products?loyalty_reward_id=<loyalty-reward-id>" in {

    "if request has valid token" in {

      "if loyaltyReward belongs to same merchant" should {

        "assign the products" in new LoyaltyRewardResourceFSpecContext {
          val yesterday = UtcTime.now.minusDays(1)

          override lazy val loyaltyReward = Factory.loyaltyReward(loyaltyProgram, overrideNow = Some(yesterday)).create

          val product0 = Factory.simpleProduct(merchant, overrideNow = Some(yesterday)).create
          Factory.loyaltyRewardProduct(loyaltyReward, product0).create

          val product1 = Factory.simpleProduct(merchant, overrideNow = Some(yesterday)).create

          val template = Factory.templateProduct(merchant, overrideNow = Some(yesterday)).create
          val variant = Factory.variantProduct(merchant, template, overrideNow = Some(yesterday)).create

          val productIds = Seq(product1.id, variant.id)
          val loyaltyRewardAssignment = ProductsAssignment(productIds = productIds)

          Post(s"/v1/loyalty_rewards.assign_products?loyalty_reward_id=${loyaltyReward.id}", loyaltyRewardAssignment)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            loyaltyRewardProductDao
              .findByLoyaltyRewardId(loyaltyReward.id)
              .await
              .map(_.productId)
              .toSet ==== Set(product1.id, variant.id)

            val allProductIds = Seq(product0.id, product1.id, template.id, variant.id)
            val updatedProducts = articleDao.findByIds(allProductIds).await
            val updatedLoyaltyReward = loyaltyRewardDao.findById(loyaltyReward.id).await.get

            // Updated
            product0.updatedAt !=== updatedProducts.find(_.id == product0.id).get.updatedAt
            product1.updatedAt !=== updatedProducts.find(_.id == product1.id).get.updatedAt
            variant.updatedAt !=== updatedProducts.find(_.id == variant.id).get.updatedAt

            loyaltyReward.updatedAt !=== updatedLoyaltyReward.updatedAt

            // Not updated
            template.updatedAt ==== updatedProducts.find(_.id == template.id).get.updatedAt
          }
        }
      }

      "if one of the products is a template" should {

        "return 404" in new LoyaltyRewardResourceFSpecContext {
          val product0 = Factory.simpleProduct(merchant).create
          Factory.loyaltyRewardProduct(loyaltyReward, product0).create

          val simple = Factory.simpleProduct(merchant).create
          val template = Factory.templateProduct(merchant).create
          val variant = Factory.variantProduct(merchant, template).create

          val simplePart = Factory.simplePart(merchant).create

          val productIds = Seq(template.id)
          val loyaltyRewardAssignment = ProductsAssignment(productIds = productIds)

          Post(s"/v1/loyalty_rewards.assign_products?loyalty_reward_id=${loyaltyReward.id}", loyaltyRewardAssignment)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("UnexpectedNonStorableIds")
          }
        }
      }

      "if one of the articles is not a product" should {

        "return 404" in new LoyaltyRewardResourceFSpecContext {
          val product0 = Factory.simpleProduct(merchant).create
          Factory.loyaltyRewardProduct(loyaltyReward, product0).create

          val simple = Factory.simpleProduct(merchant).create
          val template = Factory.templateProduct(merchant).create
          val variant = Factory.variantProduct(merchant, template).create

          val simplePart = Factory.simplePart(merchant).create

          val productIds = Seq(simple.id, template.id, variant.id, simplePart.id)
          val loyaltyRewardAssignment = ProductsAssignment(productIds = productIds)

          Post(s"/v1/loyalty_rewards.assign_products?loyalty_reward_id=${loyaltyReward.id}", loyaltyRewardAssignment)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if any product id in the assignment doesn't belong to same merchant" should {

        "return 404" in new LoyaltyRewardResourceFSpecContext {
          val competitor = Factory.merchant.create

          val competitorProduct = Factory.simpleProduct(competitor).create
          val product = Factory.simpleProduct(merchant).create

          val productIds = Seq(competitorProduct.id, product.id)
          val loyaltyRewardAssignment = ProductsAssignment(productIds = productIds)

          Post(s"/v1/loyalty_rewards.assign_products?loyalty_reward_id=${loyaltyReward.id}", loyaltyRewardAssignment)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if loyaltyReward doesn't belong to same merchant" should {

        "return 404" in new LoyaltyRewardResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorLoyaltyProgram = Factory.loyaltyProgram(competitor).create
          val competitorLoyaltyReward = Factory.loyaltyReward(competitorLoyaltyProgram).create

          Post(s"/v1/loyalty_rewards.assign_products?loyalty_reward_id=${competitorLoyaltyReward.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }

    "if request has invalid token" should {

      "be rejected" in new LoyaltyRewardResourceFSpecContext {
        val loyaltyRewardId = UUID.randomUUID
        val loyaltyRewardAssignment = random[ProductsAssignment]
        Post(s"/v1/loyalty_rewards.assign_products?loyalty_reward_id=$loyaltyRewardId", loyaltyRewardAssignment)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
