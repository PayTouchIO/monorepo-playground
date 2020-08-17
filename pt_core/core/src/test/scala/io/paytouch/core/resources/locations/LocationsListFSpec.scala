package io.paytouch.core.resources.locations

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection

import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.entities.{ Location => LocationEntity, _ }
import io.paytouch.core.utils.{ AppTokenFixtures, UtcTime, FixtureDaoFactory => Factory }

class LocationsListFSpec extends LocationsFSpec {
  abstract class LocationsListFSpecContext extends LocationsResourceFSpecContext

  "GET /v1/locations.list" in {
    "if request has valid token" in {

      "with no parameters" should {
        "return a paginated list of all accessible locations sorted by title" in new LocationsListFSpecContext {
          val newYork = Factory.location(merchant).create
          val deletedLocation = Factory.location(merchant, deletedAt = Some(UtcTime.now)).create
          Factory.userLocation(user, deletedLocation).create

          Get("/v1/locations.list").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val locations = responseAs[PaginatedApiResponse[Seq[LocationEntity]]]
            locations.data.map(_.id).toSet ==== Set(rome.id, london.id)
            assertResponse(rome, locations.data.find(_.id == rome.id).get)
            assertResponse(london, locations.data.find(_.id == london.id).get)
          }
        }
      }

      "with q= filters" should {
        "return a list of accessible locations matching query" in new LocationsListFSpecContext {
          val deletedLocation = Factory.location(merchant, deletedAt = Some(UtcTime.now)).create

          Get("/v1/locations.list?q=lond").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val locations = responseAs[PaginatedApiResponse[Seq[LocationEntity]]]
            locations.data.map(_.id).toSet ==== Set(london.id)
            assertResponse(london, locations.data.find(_.id == london.id).get)
          }
        }
      }

      "with all filters" should {
        "return a list of all the locations of a merchant, regardless of user locations" in new LocationsListFSpecContext {
          val deletedLocation = Factory.location(merchant, deletedAt = Some(UtcTime.now)).create
          val newYork = Factory.location(merchant).create

          Get("/v1/locations.list?all=true").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val locations = responseAs[PaginatedApiResponse[Seq[LocationEntity]]]
            locations.data.map(_.id).toSet ==== Set(london.id, rome.id, newYork.id)
            assertResponse(london, locations.data.find(_.id == london.id).get)
            assertResponse(rome, locations.data.find(_.id == rome.id).get)
            assertResponse(newYork, locations.data.find(_.id == newYork.id).get)
          }
        }
      }

      "with expand[]=settings" should {
        "return a list of accessible locations with expanded settings" in new LocationsListFSpecContext {
          val deletedLocation = Factory.location(merchant, deletedAt = Some(UtcTime.now)).create

          Factory
            .imageUpload(
              merchant,
              objectId = Some(rome.id),
              imageUploadType = Some(ImageUploadType.EmailReceipt),
              urls = Some(Map("foo" -> "bar")),
            )
            .create
          Factory
            .imageUpload(
              merchant,
              objectId = Some(rome.id),
              imageUploadType = Some(ImageUploadType.PrintReceipt),
              urls = Some(Map("foo" -> "bar")),
            )
            .create

          Factory.locationSettings(rome).create
          Factory.locationSettings(london).create
          Get("/v1/locations.list?expand[]=settings").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val locations = responseAs[PaginatedApiResponse[Seq[LocationEntity]]]
            locations.data.map(_.id).toSet ==== Set(rome.id, london.id)
            assertResponse(rome, locations.data.find(_.id == rome.id).get, withSettings = true)
            assertResponse(london, locations.data.find(_.id == london.id).get, withSettings = true)
          }
        }
      }

      "with expand[]=tax_rates" should {
        "return a list of accessible locations with expanded tax rates" in new LocationsListFSpecContext {
          val newYork = Factory.location(merchant).create

          val taxRateRome = Factory.taxRate(merchant, locations = Seq(rome)).create
          val taxRateLondon = Factory.taxRate(merchant, locations = Seq(london)).create

          Get("/v1/locations.list?expand[]=tax_rates").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val locations = responseAs[PaginatedApiResponse[Seq[LocationEntity]]]
            locations.data.map(_.id).toSet ==== Set(rome.id, london.id)
            assertResponse(rome, locations.data.find(_.id == rome.id).get, taxRateIds = Some(Seq(taxRateRome.id)))
            assertResponse(london, locations.data.find(_.id == london.id).get, taxRateIds = Some(Seq(taxRateLondon.id)))
          }
        }
      }

      "with expand[]=opening_hours" should {
        "return a list of accessible locations with expanded opening hours" in new LocationsListFSpecContext {
          val locationAvailability = Factory.locationAvailability(rome, Seq(Weekdays.Monday, Weekdays.Tuesday)).create

          val expectedAvailabilityMap = Map(
            Weekdays.Monday -> Seq(Availability(locationAvailability.start, locationAvailability.end)),
            Weekdays.Tuesday -> Seq(Availability(locationAvailability.start, locationAvailability.end)),
          )

          Get("/v1/locations.list?expand[]=opening_hours").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val locations = responseAs[PaginatedApiResponse[Seq[LocationEntity]]]
            locations.data.map(_.id).toSet ==== Set(rome.id, london.id)

            assertResponse(
              rome,
              locations.data.find(_.id == rome.id).get,
              availabilityMap = Some(expectedAvailabilityMap),
            )
          }
        }
      }

    }

    "if request has valid app token" should {
      "return locations" in new LocationsListFSpecContext with AppTokenFixtures {
        Get(s"/v1/locations.list").addHeader(appAuthorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val locations = responseAs[PaginatedApiResponse[Seq[LocationEntity]]]
          locations.data.map(_.id).toSet ==== Set(rome.id, london.id)
          assertResponse(rome, locations.data.find(_.id == rome.id).get)
          assertResponse(london, locations.data.find(_.id == london.id).get)
        }
      }
    }

    "if request has invalid token" should {
      "reject request" in new LocationsListFSpecContext {
        Get(s"/v1/locations.list").addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
