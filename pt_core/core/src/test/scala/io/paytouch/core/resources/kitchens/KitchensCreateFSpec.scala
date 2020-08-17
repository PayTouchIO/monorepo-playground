package io.paytouch.core.resources.kitchens

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.utils.{ SetupStepsAssertions, FixtureDaoFactory => Factory }
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.MerchantSetupSteps

class KitchensCreateFSpec extends KitchensFSpec {

  abstract class KitchensCreateFSpecContext extends KitchenResourceFSpecContext with SetupStepsAssertions

  "POST /v1/kitchens.create?kitchen_id=$" in {
    "if request has valid token" in {

      "create kitchen and return 201" in new KitchensCreateFSpecContext {
        val newKitchenId = UUID.randomUUID

        @scala.annotation.nowarn("msg=Auto-application")
        val creation = random[KitchenCreation].copy(locationId = london.id)

        Post(s"/v1/kitchens.create?kitchen_id=$newKitchenId", creation)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()

          val kitchen = responseAs[ApiResponse[Kitchen]].data
          assertCreation(kitchen.id, creation)
          assertSetupStepCompleted(merchant, MerchantSetupSteps.SetupKitchens)
        }
      }

      "reject creation if user does not have access to the given location" in new KitchensCreateFSpecContext {
        val newYork = Factory.location(merchant).create
        val newKitchenId = UUID.randomUUID

        @scala.annotation.nowarn("msg=Auto-application")
        val creation = random[KitchenCreation].copy(locationId = newYork.id)

        Post(s"/v1/kitchens.create?kitchen_id=$newKitchenId", creation)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NotFound)
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new KitchensCreateFSpecContext {
        val newKitchenId = UUID.randomUUID

        @scala.annotation.nowarn("msg=Auto-application")
        val creation = random[KitchenCreation]

        Post(s"/v1/kitchens.create?kitchen_id=$newKitchenId", creation)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
