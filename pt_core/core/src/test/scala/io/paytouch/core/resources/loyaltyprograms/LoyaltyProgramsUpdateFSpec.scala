package io.paytouch.core.resources.loyaltyprograms

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.{ LoyaltyProgram => LoyaltyProgramEntity, _ }
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class LoyaltyProgramsUpdateFSpec extends LoyaltyProgramsFSpec {

  "POST /v1/loyalty_programs.update?loyalty_program_id=$" in {
    "if request has valid token" in {
      "if loyaltyProgram belongs to current merchant" should {
        "update the loyaltyProgram" in new LoyaltyProgramResourceFSpecContext {
          val loyaltyProgram = Factory.loyaltyProgram(merchant).create
          val loyaltyReward = Factory.loyaltyReward(loyaltyProgram).create

          val rewards = Seq(random[LoyaltyRewardUpdate].copy(id = loyaltyReward.id))
          val loyaltyProgramUpdate =
            random[LoyaltyProgramUpdate].copy(locationIds = None, rewards = Some(rewards))

          Post(s"/v1/loyalty_programs.update?loyalty_program_id=${loyaltyProgram.id}", loyaltyProgramUpdate)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val updateResponse = responseAs[ApiResponse[LoyaltyProgramEntity]]
            val loyaltyProgramDb = loyaltyProgramDao.findById(loyaltyProgram.id).await.head

            assertResponse(loyaltyProgramDb, updateResponse.data)
            assertUpdate(loyaltyProgram.id, loyaltyProgramUpdate)
          }
        }

        "update the loyaltyProgram with location overrides" in new LoyaltyProgramResourceFSpecContext {
          val loyaltyProgram = Factory.loyaltyProgram(merchant).create

          val loyaltyProgramLocationRome =
            Factory.loyaltyProgramLocation(loyaltyProgram, rome).create
          val loyaltyProgramLocationLondon = Factory.loyaltyProgramLocation(loyaltyProgram, london).create

          val locationIdsUpdate = Seq(rome.id)

          val loyaltyProgramUpdate =
            random[LoyaltyProgramUpdate].copy(locationIds = Some(locationIdsUpdate), rewards = None)

          Post(s"/v1/loyalty_programs.update?loyalty_program_id=${loyaltyProgram.id}", loyaltyProgramUpdate)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val updateResponse = responseAs[ApiResponse[LoyaltyProgramEntity]]
            val loyaltyProgramDb = loyaltyProgramDao.findById(loyaltyProgram.id).await.head

            assertResponse(loyaltyProgramDb, updateResponse.data)
            assertUpdate(loyaltyProgram.id, loyaltyProgramUpdate)
          }
        }
        "reject request if any location id does not exist or does not belong to merchant" in new LoyaltyProgramResourceFSpecContext {
          val loyaltyProgram = Factory.loyaltyProgram(merchant).create

          val competitor = Factory.merchant.create
          val competitorLocation = Factory.location(competitor).create

          val competitorLocationIds = Seq(competitorLocation.id)

          val loyaltyProgramUpdate =
            random[LoyaltyProgramUpdate].copy(locationIds = Some(competitorLocationIds), rewards = None)

          Post(s"/v1/loyalty_programs.update?loyalty_program_id=${loyaltyProgram.id}", loyaltyProgramUpdate)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)

            loyaltyProgramDao.findById(loyaltyProgram.id).await.get ==== loyaltyProgram
          }
        }
      }
      "if loyaltyProgram doesn't belong to current merchant" should {
        "not update loyaltyProgram and return 404" in new LoyaltyProgramResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorLoyaltyProgram = Factory.loyaltyProgram(competitor).create
          val loyaltyProgramUpdate = random[LoyaltyProgramUpdate]

          Post(s"/v1/loyalty_programs.update?loyalty_program_id=${competitorLoyaltyProgram.id}", loyaltyProgramUpdate)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)

            loyaltyProgramDao.findById(competitorLoyaltyProgram.id).await.get ==== competitorLoyaltyProgram
          }
        }
      }
    }
  }
}
