package io.paytouch.core.resources.kitchens

import java.time.ZonedDateTime

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, UtcTime }

class KitchensListFSpec extends KitchensFSpec {

  abstract class KitchensListFSpecContext extends KitchenResourceFSpecContext

  "GET /v1/kitchens.list" in {
    "if request has valid token" should {
      "with no parameters" should {
        "return a paginated list of kitchens" in new KitchensListFSpecContext {
          val kitchen1 = Factory.kitchen(london, name = Some("Alphabetically")).create
          val kitchen2 = Factory.kitchen(london, name = Some("Ordered")).create

          Get("/v1/kitchens.list").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val kitchens = responseAs[PaginatedApiResponse[Seq[Kitchen]]].data
            kitchens.map(_.id) ==== Seq(kitchen1.id, kitchen2.id)

            assertResponse(kitchens.find(_.id == kitchen1.id).get, kitchen1)
            assertResponse(kitchens.find(_.id == kitchen2.id).get, kitchen2)
          }
        }

        "doesn't return deleted kitchens" in new KitchensListFSpecContext {
          val kitchen1 = Factory.kitchen(london, name = Some("Alphabetically"), deletedAt = Some(UtcTime.now)).create
          val kitchen2 = Factory.kitchen(london, name = Some("Ordered")).create

          Get("/v1/kitchens.list").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val kitchens = responseAs[PaginatedApiResponse[Seq[Kitchen]]].data
            kitchens.map(_.id) ==== Seq(kitchen2.id)

            assertResponse(kitchens.find(_.id == kitchen2.id).get, kitchen2)
          }
        }
      }
      "with updated_since filter" should {
        "return a paginated list of kitchens" in new KitchensListFSpecContext {
          val now = ZonedDateTime.parse("2015-12-03T20:15:30Z")

          val kitchen1 = Factory.kitchen(london, name = Some("Alphabetically")).create
          val kitchen2 = Factory.kitchen(london, name = Some("Ordered"), overrideNow = Some(now.minusDays(1))).create

          Get("/v1/kitchens.list?updated_since=2015-12-03").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val kitchens = responseAs[PaginatedApiResponse[Seq[Kitchen]]].data
            kitchens.map(_.id) ==== Seq(kitchen1.id)

            assertResponse(kitchens.find(_.id == kitchen1.id).get, kitchen1)
          }
        }
      }
      "with location_id filter" should {
        "return a paginated list of kitchens" in new KitchensListFSpecContext {
          val kitchen1 = Factory.kitchen(london, name = Some("Alphabetically")).create
          val kitchen2 = Factory.kitchen(rome, name = Some("Ordered")).create

          Get(s"/v1/kitchens.list?location_id=${london.id}").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val kitchens = responseAs[PaginatedApiResponse[Seq[Kitchen]]].data
            kitchens.map(_.id) ==== Seq(kitchen1.id)

            assertResponse(kitchens.find(_.id == kitchen1.id).get, kitchen1)
          }
        }

        "not return data when the user does not have access to the given location" in new KitchensListFSpecContext {
          val kitchen1 = Factory.kitchen(london, name = Some("Alphabetically")).create
          val kitchen2 = Factory.kitchen(rome, name = Some("Ordered")).create

          val newYork = Factory.location(merchant).create
          val kitchen3 = Factory.kitchen(newYork, name = Some("Another")).create

          Get(s"/v1/kitchens.list?location_id=${newYork.id}").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val kitchens = responseAs[PaginatedApiResponse[Seq[Kitchen]]].data
            kitchens ==== Seq.empty
          }
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new KitchensListFSpecContext {
        val kitchen1 = Factory.kitchen(london, name = Some("Alphabetically")).create

        Get(s"/v1/kitchens.list?location_id=${london.id}").addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }

}
