package io.paytouch.core.resources.kitchens

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class KitchensUpdateFSpec extends KitchensFSpec {

  abstract class KitchensUpdateFSpecContext extends KitchenResourceFSpecContext

  "POST /v1/kitchens.update?kitchen_id=$" in {
    "if request has valid token" in {
      "if kitchen belong to same merchant" should {
        "update kitchen and return 200" in new KitchensUpdateFSpecContext {
          val kitchen = Factory.kitchen(london).create

          @scala.annotation.nowarn("msg=Auto-application")
          val update = random[KitchenUpdate].copy(locationId = Some(london.id))

          Post(s"/v1/kitchens.update?kitchen_id=${kitchen.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertUpdate(kitchen.id, update)
          }
        }

        "reject update if the location has changed" in new KitchensUpdateFSpecContext {
          val kitchen = Factory.kitchen(london).create

          @scala.annotation.nowarn("msg=Auto-application")
          val update = random[KitchenUpdate].copy(locationId = Some(rome.id))

          Post(s"/v1/kitchens.update?kitchen_id=${kitchen.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
          }
        }
      }

      "if kitchen doesn't belong to current user's merchant" in {
        "not update kitchen and return 404" in new KitchensUpdateFSpecContext {
          val competitor = Factory.merchant.create
          val competitorLocation = Factory.location(competitor).create
          val competitorKitchen = Factory.kitchen(competitorLocation).create

          @scala.annotation.nowarn("msg=Auto-application")
          val update = random[KitchenUpdate]

          Post(s"/v1/kitchens.update?kitchen_id=${competitorKitchen.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)

            val updatedKitchen = kitchenDao.findById(competitorKitchen.id).await.get
            updatedKitchen ==== competitorKitchen
          }
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new KitchensUpdateFSpecContext {
        val kitchenId = UUID.randomUUID

        @scala.annotation.nowarn("msg=Auto-application")
        val update = random[KitchenUpdate]

        Post(s"/v1/kitchens.update?kitchen_id=$kitchenId", update)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
