package io.paytouch.ordering.clients.paytouch.core

import java.time.{ LocalTime, ZoneId }

import akka.http.scaladsl.model._

import cats.implicits._

import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.clients.paytouch.core.entities.enums.Day
import io.paytouch.ordering.errors.ClientError

class LocationsListSpec extends PtCoreClientSpec {

  abstract class LocationsListSpecContext extends CoreClientSpecContext {

    val params = "expand[]=opening_hours"

    def assertLocationsList(implicit request: HttpRequest, authToken: HttpHeader) =
      assertUserAuthRequest(HttpMethods.GET, "/v1/locations.list", authToken, queryParams = Some(params))

  }

  "CoreClient" should {

    "call locations.get" should {

      "parse a location" in new LocationsListSpecContext with LocationFixture {
        val response = when(locationsList)
          .expectRequest(implicit request => assertLocationsList)
          .respondWith(locationFileName)
        response.await.map(_.data) ==== Right(expectedLocations)
      }

      "parse rejection" in new LocationsListSpecContext {
        val endpoint =
          completeUri(s"/v1/locations.list?$params")
        val expectedRejection = ClientError(endpoint, "The supplied authentication is invalid")
        val response = when(locationsList)
          .expectRequest(implicit request => assertLocationsList)
          .respondWithRejection("/core/responses/auth_rejection.json")
        response.await ==== Left(expectedRejection)
      }
    }
  }

  trait LocationFixture { self: LocationsListSpecContext =>
    val locationFileName = "/core/responses/locations_list.json"

    private val expectedAddress = Address(
      line1 = "700 5th Ave".some,
      line2 = None,
      city = "New York".some,
      state = "New York".some,
      country = "United States".some,
      postalCode = "NY 10153".some,
      stateData = State(
        State.Name("New York").some,
        State.Code("NY"),
        Country(
          Country.Code("US"),
          Country.Name("United States"),
        ).some,
      ).some,
    )

    private val expectedOpeningHours: Map[Day, Seq[Availability]] =
      Day
        .values
        .map(day => day -> Seq(Availability(start = LocalTime.NOON, end = LocalTime.MIDNIGHT)))
        .toMap

    val expectedLocations: Seq[Location] = Seq(
      Location(
        id = "c6342289-5123-3971-908d-57a29b01bd24",
        name = "Apple Store",
        email = Some("apple-store@paytouch-test.io"),
        phoneNumber = "+090800701",
        website = Some("https://www.paytouch.io"),
        active = true,
        address = expectedAddress,
        timezone = ZoneId.of("America/New_York"),
        currency = USD,
        openingHours = Some(expectedOpeningHours),
        coordinates = Some(Coordinates(1.23, 2.34)),
        settings = None,
      ),
    )
  }
}
