package io.paytouch.ordering.clients.paytouch.core

import java.util.UUID

import akka.http.scaladsl.model._

import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.errors.ClientError
import io.paytouch.ordering.utils.CommonArbitraries
import io.paytouch.ordering.utils.OrdersGetFixture

class OrdersGetSpec extends PtCoreClientSpec with CommonArbitraries {
  abstract class OrdersGetSpecContext extends CoreClientSpecContext {
    val id: UUID = "ac544e46-63a7-4d2f-81f9-53f68bcd02b2"

    val params = s"order_id=$id"

    def assertOrdersGet(implicit request: HttpRequest, authToken: HttpHeader) =
      assertUserAuthRequest(HttpMethods.GET, "/v1/orders.get", authToken, queryParams = Some(params))
  }

  "CoreClient" should {

    "call orders.get" should {

      "parse a order" in new OrdersGetSpecContext with OrderFixture {

        val response = when(ordersGet(id))
          .expectRequest(implicit request => assertOrdersGet)
          .respondWith(orderFileName)
        response.await.map(_.data) ==== Right(expectedOrder)
      }

      "parse rejection" in new OrdersGetSpecContext {
        val endpoint = completeUri(s"/v1/orders.get?$params")
        val expectedRejection = ClientError(endpoint, "The supplied authentication is invalid")
        val response = when(ordersGet(id))
          .expectRequest(implicit request => assertOrdersGet)
          .respondWithRejection("/core/responses/auth_rejection.json")
        response.await ==== Left(expectedRejection)
      }
    }
  }

  trait OrderFixture extends OrdersGetFixture { self: OrdersGetSpecContext =>
    val orderFileName = "/core/responses/orders_get.json"
    val expectedOrder = ordersGetFixture
  }
}
