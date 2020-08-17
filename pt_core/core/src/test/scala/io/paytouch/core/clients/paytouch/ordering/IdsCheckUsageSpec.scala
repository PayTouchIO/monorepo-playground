package io.paytouch.core.clients.paytouch.ordering

import java.util.UUID

import akka.http.scaladsl.model._
import io.paytouch.core.clients.paytouch.ordering.entities.{ IdsToCheck, IdsUsage }

class IdsCheckUsageSpec extends PtOrderingClientSpec {

  abstract class IdsCheckUsageSpecContext extends PtOrderingClientSpecContext {
    val ids = IdsToCheck(catalogIds = Seq(UUID.randomUUID))

    def assertIdsCheckUsage(ids: IdsToCheck)(implicit request: HttpRequest) = {
      assertAppAuthRequest(HttpMethods.POST, "/v1/ids.check_usage", body = Some(ids))
      request.uri.rawQueryString ==== Some(s"merchant_id=${merchant.id}")
    }
  }

  "PtOrderingClient" should {

    "call ids.check_usage?merchant_id=<>" should {

      "parse response" in new IdsCheckUsageSpecContext {
        val expectedIdsUsage = IdsUsage(
          accessible = IdsToCheck(
            Seq(
              "b82b7b62-8251-3fd9-9e18-d73ab1adb918",
              "c6342289-5123-3971-908d-57a29b01bd24",
            ),
          ),
          notUsed = IdsToCheck(
            Seq(
              "478a122b-37fc-318c-a70f-f95cdd699b78",
              "ac65f053-e966-3bdd-82f2-c8be7c02ad8c",
            ),
          ),
          nonAccessible = IdsToCheck(Seq("29f42b30-e479-3e2e-bd24-4003332f071d")),
        )
        val response = when(idsCheckUsage(ids))
          .expectRequest(implicit request => assertIdsCheckUsage(ids))
          .respondWith("/ordering/responses/ids_check_usage.json")
        response.await.map(_.data) ==== Right(expectedIdsUsage)
      }

      "parse rejection" in new IdsCheckUsageSpecContext {
        val response = when(idsCheckUsage(ids))
          .expectRequest(implicit request => assertIdsCheckUsage(ids))
          .respondWithRejection("/ordering/responses/ids_check_usage_rejection.json")
        response.await.isLeft should beTrue
      }
    }
  }

}
