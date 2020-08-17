package io.paytouch.core.resources.locations

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.Ids
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class LocationsDeleteFSpec extends LocationsFSpec {

  abstract class LocationDeleteResourceFSpecContext extends LocationsResourceFSpecContext {

    def assertLocationIsMarkedAsDeleted(id: UUID) = locationDao.findDeletedById(id).await should beSome

    def assertLocationDoesntExist(id: UUID) = {
      locationDao.findDeletedById(id).await should beNone
      locationDao.findById(id).await should beNone
    }

    def assertLocationExists(id: UUID) = locationDao.findById(id).await should beSome
  }

  "POST /v1/locations.delete" in {

    "if request has valid token" in {
      "if location doesn't exist" should {
        "do nothing and return 204" in new LocationDeleteResourceFSpecContext {
          val nonExistingLocationId = UUID.randomUUID

          Post(s"/v1/locations.delete", Ids(ids = Seq(nonExistingLocationId)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)
            assertLocationDoesntExist(nonExistingLocationId)
          }
        }
      }

      "if location belongs to the merchant" should {
        "delete the location and return 204" in new LocationDeleteResourceFSpecContext {
          val location = Factory.location(merchant).create

          Post(s"/v1/locations.delete", Ids(ids = Seq(location.id)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            assertLocationIsMarkedAsDeleted(location.id)
          }
        }
      }

      "if location belongs to a different merchant" should {
        "do not delete the location and return 204" in new LocationDeleteResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorLocation = Factory.location(competitor).create

          Post(s"/v1/locations.delete", Ids(ids = Seq(competitorLocation.id)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            assertLocationExists(competitorLocation.id)
          }
        }
      }
    }
  }
}
