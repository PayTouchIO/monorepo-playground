package io.paytouch.ordering.clients.paytouch.core

import java.time.{ Duration, LocalTime, ZoneId }
import java.time.temporal.ChronoUnit
import java.util.UUID

import akka.http.scaladsl.model._

import cats.implicits._

import io.paytouch._

import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.clients.paytouch.core.entities.enums.Day
import io.paytouch.ordering.errors.ClientError

class LocationsGetSpec extends PtCoreClientSpec {

  abstract class LocationsGetSpecContext extends CoreClientSpecContext {

    val id: UUID = "c6342289-5123-3971-908d-57a29b01bd24"

    val params = {
      val expansions = "expand[]=opening_hours,settings"
      s"location_id=$id&$expansions"
    }

    def assertLocationsGet(implicit request: HttpRequest, authToken: HttpHeader) =
      assertUserAuthRequest(HttpMethods.GET, "/v1/locations.get", authToken, queryParams = Some(params))
  }

  "CoreClient" should {

    "call locations.get" should {

      "parse a location" in new LocationsGetSpecContext with LocationFixture {
        val response = when(locationsGet(id))
          .expectRequest(implicit request => assertLocationsGet)
          .respondWith(locationFileName)
        response.await.map(_.data) ==== Right(expectedLocation)
      }

      "parse rejection" in new LocationsGetSpecContext {
        val endpoint =
          completeUri(s"/v1/locations.get?$params")
        val expectedRejection = ClientError(endpoint, "The supplied authentication is invalid")
        val response = when(locationsGet(id))
          .expectRequest(implicit request => assertLocationsGet)
          .respondWithRejection("/core/responses/auth_rejection.json")
        response.await ==== Left(expectedRejection)
      }
    }
  }

  trait LocationFixture { self: LocationsGetSpecContext =>
    val locationFileName = "/core/responses/locations_get.json"

    private val expectedAddress = Address(
      line1 = "700 5th Ave".some,
      line2 = "".some,
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
        .map { day =>
          day -> Seq(
            Availability(
              start = LocalTime.MIDNIGHT,
              end = LocalTime.MIDNIGHT.minus(Duration.of(1, ChronoUnit.MINUTES)),
            ),
          )
        }
        .toMap

    val expectedLocation: Location = Location(
      id = id,
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
      settings = Some(
        LocationSettings(
          onlineOrder = LocationOnlineOrderSettings(
            defaultEstimatedPrepTimeInMins = None,
          ),
        ),
      ),
    )
  }
}
