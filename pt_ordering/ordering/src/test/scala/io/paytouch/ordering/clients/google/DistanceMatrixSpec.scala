package io.paytouch.ordering.clients.google

import akka.http.scaladsl.model._
import io.paytouch.ordering.clients.google.entities._

class DistanceMatrixSpec extends GMapsClientSpec {

  abstract class DistanceMatrixSpecContext extends GDirectionClientSpecContext {
    val origin = "75+9th+Ave+New+York,+NY"
    val destination = "MetLife+Stadium+1+MetLife+Stadium+Dr+East+Rutherford,+NJ+07073"

    val params = s"origins=$origin&destinations=$destination"

    def assertDirection(implicit request: HttpRequest) =
      assertKeyRequest(HttpMethods.GET, "/maps/api/distancematrix/json", queryParams = Some(params))
  }

  "GMapClient" should {

    "call distance matrix API" should {

      "parse the response" in new DistanceMatrixSpecContext {
        val fileName = "/google/responses/distance_ok.json"

        val expectedDistance = GDistance("362 km", 361940)
        val expectedDuration = GDuration("3 hours 50 mins", 13812)

        val response = when(distanceMatrix(origin = origin, destination = destination))
          .expectRequest(implicit request => assertDirection)
          .respondWith(fileName)
        response.await ==== Right(GDistanceMatrix(expectedDistance, expectedDuration))
      }

      "parse not found" in new DistanceMatrixSpecContext {
        val fileName = "/google/responses/distance_not_found.json"

        val expectedNotFound = GDistanceMatrix(GDistance(), GDuration(), GStatus.NotFound)

        val response = when(distanceMatrix(origin = origin, destination = destination))
          .expectRequest(implicit request => assertDirection)
          .respondWith(fileName)
        response.await ==== Right(expectedNotFound)
      }

      "parse request denied" in new DistanceMatrixSpecContext {
        val fileName = "/google/responses/request_denied.json"

        val response = when(distanceMatrix(origin = origin, destination = destination))
          .expectRequest(implicit request => assertDirection)
          .respondWith(fileName)
        response.await ==== Left(GError("The provided API key is invalid.", "REQUEST_DENIED"))
      }

    }
  }

}
