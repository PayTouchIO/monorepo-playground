package io.paytouch.core.resources.locations

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection

import io.paytouch.core.entities.{ Location => LocationEntity, _ }
import io.paytouch.core.utils.{ AppTokenFixtures, UtcTime, FixtureDaoFactory => Factory }

class LocationsGetFSpec extends LocationsFSpec {
  abstract class LocationsGetFSpecContext extends LocationsResourceFSpecContext
  "GET /v1/locations.get?location_id=$" in {
    "if request has valid token" in {
      "with no parameters" should {
        "return a location object" in new LocationsGetFSpecContext {
          Get(s"/v1/locations.get?location_id=${rome.id}").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val entity = responseAs[ApiResponse[LocationEntity]].data
            assertResponse(rome, entity)
          }
        }

        "return 404 if location marked as deleted" in new LocationsGetFSpecContext {
          val deletedLocation = Factory.location(merchant, deletedAt = Some(UtcTime.now)).create

          Get(s"/v1/locations.get?location_id=${deletedLocation.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "with expand[]=settings" should {
        "return a location with expanded settings" in new LocationsGetFSpecContext {
          Factory.locationSettings(rome).create
          Get(s"/v1/locations.get?location_id=${rome.id}&expand[]=settings")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val entity = responseAs[ApiResponse[LocationEntity]].data
            assertResponse(rome, entity, withSettings = true)
          }
        }
      }
    }

    "if request has valid app token" should {
      "return a location object" in new LocationsGetFSpecContext with AppTokenFixtures {
        Get(s"/v1/locations.get?location_id=${rome.id}").addHeader(appAuthorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val entity = responseAs[ApiResponse[LocationEntity]].data
          assertResponse(rome, entity)
        }
      }
    }

    "if request has invalid token" should {
      "reject request" in new LocationsGetFSpecContext {
        Get(s"/v1/locations.get?location_id=${rome.id}").addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
