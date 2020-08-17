package io.paytouch.core.resources.kitchens

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class KitchensDeleteFSpec extends KitchensFSpec {

  abstract class KitchensDeleteFSpecContext extends KitchenResourceFSpecContext {
    def assertKitchenIsMarkedAsDeleted(id: UUID) = {
      val kitchen = kitchenDao.findDeletedById(id).await
      kitchen should beSome
      kitchen.flatMap(_.deletedAt) should beSome
    }

    def assertKitchenIsNotMarkedAsDeleted(id: UUID) = {
      val kitchen = kitchenDao.findById(id).await
      kitchen should beSome
      kitchen.flatMap(_.deletedAt) should beNone
    }
  }

  "POST /v1/kitchens.delete?kitchen_id=$" in {
    "if request has valid token" in {
      "if kitchen belong to same merchant" should {
        "mark the kitchen as deleted and return 204" in new KitchensDeleteFSpecContext {
          val kitchen = Factory.kitchen(london).create

          Post(s"/v1/kitchens.delete", Ids(ids = Seq(kitchen.id)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            assertKitchenIsMarkedAsDeleted(kitchen.id)
          }
        }
      }

      "if kitchen belong to same merchant but is in use" should {
        "reject with 400" in new KitchensDeleteFSpecContext {
          val kitchen = Factory.kitchen(london).create
          val product = Factory.simpleProduct(merchant).create
          Factory.productLocation(product, london, routeToKitchen = Some(kitchen)).create

          Post(s"/v1/kitchens.delete", Ids(ids = Seq(kitchen.id)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("KitchenStillInUse")

            assertKitchenIsNotMarkedAsDeleted(kitchen.id)
          }
        }
      }

      "if kitchen doesn't belong to current user's merchant" in {
        "not mark the kitchen as deleted and return 204" in new KitchensDeleteFSpecContext {
          val competitor = Factory.merchant.create
          val competitorLocation = Factory.location(competitor).create
          val competitorKitchen = Factory.kitchen(competitorLocation).create

          Post(s"/v1/kitchens.delete", Ids(ids = Seq(competitorKitchen.id)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            assertKitchenIsNotMarkedAsDeleted(competitorKitchen.id)
          }
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new KitchensDeleteFSpecContext {
        val kitchen = Factory.kitchen(london).create

        Post(s"/v1/kitchens.delete", Ids(ids = Seq(kitchen.id)))
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]

          assertKitchenIsNotMarkedAsDeleted(kitchen.id)
        }
      }
    }
  }
}
