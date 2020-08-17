package io.paytouch.core.resources.loyaltyprograms

import java.time.ZonedDateTime

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.entities.{ LoyaltyProgram => LoyaltyProgramEntity, _ }
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class LoyaltyProgramListFSpec extends LoyaltyProgramsFSpec {

  abstract class LoyaltyResourceFSpecContext extends LoyaltyProgramResourceFSpecContext

  "GET /v1/loyalty_programs.list" in {
    "if request has valid token" should {
      "with no params" should {

        "return a paginated list of loyalty programs that belong to the current merchant" in new LoyaltyResourceFSpecContext {
          val loyaltyProgram1 = Factory.loyaltyProgram(merchant).create
          val loyaltyProgram2 = Factory.loyaltyProgram(merchant).create

          val image = Factory
            .imageUpload(
              merchant,
              objectId = Some(loyaltyProgram1.id),
              imageUploadType = Some(ImageUploadType.LoyaltyProgram),
            )
            .create

          val competitor = Factory.merchant.create
          val loyaltyProgramOfOtherMerchant = Factory.loyaltyProgram(competitor).create

          Get("/v1/loyalty_programs.list").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val loyaltyPrograms = responseAs[PaginatedApiResponse[Seq[LoyaltyProgramEntity]]]
            loyaltyPrograms.data.map(_.id) ==== Seq(loyaltyProgram1.id, loyaltyProgram2.id)
            assertResponse(
              loyaltyProgram1,
              loyaltyPrograms.data.find(_.id == loyaltyProgram1.id).get,
              images = Seq(image),
            )
            assertResponse(loyaltyProgram2, loyaltyPrograms.data.find(_.id == loyaltyProgram2.id).get)
          }
        }
      }

      "with location_id filter" should {

        "return a paginated list of loyalty programs filtered by location id" in new LoyaltyResourceFSpecContext {
          val loyaltyProgram1 = Factory.loyaltyProgram(merchant, locations = Seq(rome, london)).create
          val loyaltyProgram2 = Factory.loyaltyProgram(merchant, locations = Seq(london)).create

          val competitor = Factory.merchant.create
          val loyaltyProgramOfOtherMerchant = Factory.loyaltyProgram(competitor).create

          Get(s"/v1/loyalty_programs.list?location_id=${rome.id}").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val loyaltyPrograms = responseAs[PaginatedApiResponse[Seq[LoyaltyProgramEntity]]]
            loyaltyPrograms.data.map(_.id) ==== Seq(loyaltyProgram1.id)
            assertResponse(loyaltyProgram1, loyaltyPrograms.data.find(_.id == loyaltyProgram1.id).get)
          }
        }
      }

      "with filter updated_since" in {
        "return a paginated list of updated loyalty programs or loyalty programs with updated rewards filtered by updated_since" in new LoyaltyResourceFSpecContext {
          val now = ZonedDateTime.parse("2015-12-03T20:15:30Z")

          val loyaltyProgram1 =
            Factory.loyaltyProgram(merchant, locations = Seq(rome, london), overrideNow = Some(now)).create
          val loyaltyProgram2 =
            Factory.loyaltyProgram(merchant, locations = Seq(london), overrideNow = Some(now.minusMonths(10))).create
          val loyaltyProgram3 =
            Factory.loyaltyProgram(merchant, locations = Seq(london), overrideNow = Some(now.minusMonths(10))).create
          val loyaltyReward3 = Factory.loyaltyReward(loyaltyProgram3, overrideNow = Some(now)).create

          Get("/v1/loyalty_programs.list?updated_since=2015-12-03").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val loyaltyPrograms = responseAs[PaginatedApiResponse[Seq[LoyaltyProgramEntity]]]
            loyaltyPrograms.data.map(_.id) ==== Seq(loyaltyProgram3.id, loyaltyProgram1.id)
            assertResponse(loyaltyProgram1, loyaltyPrograms.data.find(_.id == loyaltyProgram1.id).get)
            assertResponse(loyaltyProgram3, loyaltyPrograms.data.find(_.id == loyaltyProgram3.id).get)
          }
        }
      }

      "with expand[]=locations" should {

        "return a paginated list of loyalty programs with locations expanded" in new LoyaltyResourceFSpecContext {
          val loyaltyProgram1 = Factory.loyaltyProgram(merchant, locations = Seq(rome)).create
          val loyaltyProgram2 = Factory.loyaltyProgram(merchant).create

          val expectedLocations1 = Seq(rome.id)
          val expectedLocations2 = Seq.empty

          Get("/v1/loyalty_programs.list?expand[]=locations").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val loyaltyPrograms = responseAs[PaginatedApiResponse[Seq[LoyaltyProgramEntity]]]
            loyaltyPrograms.data.map(_.id) ==== Seq(loyaltyProgram1.id, loyaltyProgram2.id)
            assertResponse(
              loyaltyProgram1,
              loyaltyPrograms.data.find(_.id == loyaltyProgram1.id).get,
              locationIds = Some(expectedLocations1),
            )
            assertResponse(
              loyaltyProgram2,
              loyaltyPrograms.data.find(_.id == loyaltyProgram2.id).get,
              locationIds = Some(expectedLocations2),
            )
          }
        }
      }
    }
  }
}
