package io.paytouch.core.resources.loyaltyprograms

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes

import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.entities.{ LoyaltyProgram => LoyaltyProgramEntity, _ }
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class LoyaltyProgramsCreateFSpec extends LoyaltyProgramsFSpec {
  abstract class LoyaltyProgramsCreateFSpecContext extends LoyaltyProgramResourceFSpecContext {
    val newLoyaltyProgramId = UUID.randomUUID
    val locationIds = Seq(rome.id)
  }

  "POST /v1/loyalty_programs.create?loyalty_program_id=$" in {
    "if request has valid token" in {

      "create loyaltyProgram and return 201" in new LoyaltyProgramsCreateFSpecContext {
        val rewards = Seq(random[LoyaltyRewardCreation])
        val image = Factory.imageUpload(merchant, imageUploadType = Some(ImageUploadType.LoyaltyProgram)).create
        val loyaltyProgramCreation =
          random[LoyaltyProgramCreation].copy(
            locationIds = Some(locationIds),
            rewards = Some(rewards),
            imageUploadIds = Some(Seq(image.id)),
          )

        Post(s"/v1/loyalty_programs.create?loyalty_program_id=$newLoyaltyProgramId", loyaltyProgramCreation)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()

          val creationResponse = responseAs[ApiResponse[LoyaltyProgramEntity]]
          creationResponse.data.iconImageUrls.head.imageUploadId ==== image.id

          val loyaltyProgramId = creationResponse.data.id
          val loyaltyProgramDb = loyaltyProgramDao.findById(loyaltyProgramId).await.head
          assertResponse(loyaltyProgramDb, creationResponse.data, images = Seq(image))
          assertUpdate(loyaltyProgramId, loyaltyProgramCreation.asUpdate)
        }
      }

      "if loyalty program already exists" should {
        "return 400" in new LoyaltyProgramsCreateFSpecContext {
          val loayltyProgram = Factory.loyaltyProgram(merchant).create
          val loyaltyProgramCreation =
            random[LoyaltyProgramCreation].copy(locationIds = Some(locationIds), rewards = None, imageUploadIds = None)

          Post(s"/v1/loyalty_programs.create?loyalty_program_id=$newLoyaltyProgramId", loyaltyProgramCreation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("LoyaltyProgramUniquePerMerchant")
          }
        }
      }

      "reject request if any location id does not exist or does not belong to merchant" in new LoyaltyProgramsCreateFSpecContext {
        val competitor = Factory.merchant.create
        val competitorLocation = Factory.location(competitor).create

        val competitorLocationIds = Seq(competitorLocation.id)
        val loyaltyProgramCreation =
          random[LoyaltyProgramCreation].copy(locationIds = Some(competitorLocationIds))

        Post(s"/v1/loyalty_programs.create?loyalty_program_id=$newLoyaltyProgramId", loyaltyProgramCreation)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NotFound)

          loyaltyProgramDao.findById(newLoyaltyProgramId).await ==== None
          itemLocationDao.findByItemId(newLoyaltyProgramId).await ==== Seq.empty
        }
      }
    }
  }
}
