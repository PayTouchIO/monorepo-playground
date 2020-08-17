package io.paytouch.core.resources

import java.time.ZonedDateTime
import java.util.UUID

import akka.http.scaladsl.server.Route

import io.paytouch.core.entities._
import io.paytouch.core.services._

trait LoyaltyRewardResource extends JsonResource {
  def loyaltyRewardService: LoyaltyRewardService
  def rewardRedemptionService: RewardRedemptionService

  val loyaltyRewardRoutes: Route =
    path("loyalty_rewards.assign_products") {
      post {
        parameter("loyalty_reward_id".as[UUID]) { id =>
          entity(as[ProductsAssignment]) { productsAssignment =>
            authenticate { implicit user =>
              onSuccess(loyaltyRewardService.assignProducts(id, productsAssignment)) { result =>
                completeAsEmptyResponse(result)
              }
            }
          }
        }
      }
    } ~
      path("loyalty_rewards.list_products") {
        get {
          paginateWithDefaults(30) { implicit pagination =>
            parameters("loyalty_reward_id".as[UUID], "updated_since".as[ZonedDateTime].?) { (id, updatedSince) =>
              authenticate { implicit user =>
                onSuccess(loyaltyRewardService.listProducts(id, updatedSince)) { (ids, count) =>
                  completeAsPaginatedApiResponse(ids, count)
                }
              }
            }
          }
        }
      } ~
      path("loyalty_rewards.cancel") {
        post {
          entity(as[Ids]) { cancellation =>
            authenticate { implicit user =>
              onSuccess(rewardRedemptionService.bulkCancellation(cancellation.ids)) { result =>
                completeSeqAsApiResponse(result)
              }
            }
          }
        }
      } ~
      path("loyalty_rewards.reserve") {
        post {
          parameter("reward_redemption_id".as[UUID]) { id =>
            entity(as[RewardRedemptionCreation]) { rewardRedemptionCreation =>
              authenticate { implicit user =>
                onSuccess(rewardRedemptionService.reserve(id, rewardRedemptionCreation)) { result =>
                  completeAsApiResponse(result)
                }
              }
            }
          }
        }
      }
}
