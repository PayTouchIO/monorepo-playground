package io.paytouch.core.resources.kitchens

import java.time.ZonedDateTime

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class KitchensGetFSpec extends KitchensFSpec {

  abstract class KitchensGetFSpecContext extends KitchenResourceFSpecContext

  "GET /v1/kitchens.get" in {
    "if request has valid token" in {
      "if kitchen belong to same merchant" should {
        "return the kitchen" in new KitchensGetFSpecContext {
          val kitchen = Factory.kitchen(london).create

          Get(s"/v1/kitchens.get?kitchen_id=${kitchen.id}").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val entity = responseAs[ApiResponse[Kitchen]].data
            assertResponse(entity, kitchen)
          }
        }
      }

      "if kitchen doesn't belong to current user's merchant" in {
        "return 404" in new KitchensGetFSpecContext {
          val competitor = Factory.merchant.create
          val competitorLocation = Factory.location(competitor).create
          val competitorKitchen = Factory.kitchen(competitorLocation).create

          Get(s"/v1/kitchens.get?kitchen_id=${competitorKitchen.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new KitchensGetFSpecContext {
        val kitchen = Factory.kitchen(london).create

        Get(s"/v1/kitchens.get?kitchen_id=${kitchen.id}").addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
