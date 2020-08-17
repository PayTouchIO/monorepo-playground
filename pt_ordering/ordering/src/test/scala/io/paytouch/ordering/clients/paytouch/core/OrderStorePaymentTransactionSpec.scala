package io.paytouch.ordering.clients.paytouch.core

import java.util.UUID

import akka.http.scaladsl.model._

import io.paytouch.implicits._

import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.errors.ClientError
import io.paytouch.ordering.utils.CommonArbitraries
import io.paytouch.ordering.utils.OrdersGetFixture

class OrderStorePaymentTransactionSpec extends PtCoreClientSpec with CommonArbitraries {
  trait OrderFixture extends OrdersGetFixture { self: OrderStorePaymentTransactionSpecContext =>
    val orderFileName = "/core/responses/orders_get.json"
    val expectedOrder = ordersGetFixture
  }

  abstract class OrderStorePaymentTransactionSpecContext extends CoreClientSpecContext with OrderFixture {
    val id = "ac544e46-63a7-4d2f-81f9-53f68bcd02b2"
    val params = s"order_id=$id"

    val upsertion = OrderServiceStorePaymentTransactionUpsertion
      .toOrderServiceStorePaymentTransactionUpsertion(randomPaymentTransactionUpsertion())

    def assertOrderStorePaymentTransaction(implicit request: HttpRequest, authToken: HttpHeader) =
      assertUserAuthRequest(
        HttpMethods.POST,
        "/v1/orders.store_payment_transaction",
        authToken,
        queryParams = Some(params),
        body = Some(upsertion),
      )
  }

  "CoreClient" should {
    "call orders.store_payment_transaction" should {
      "store a payment transaction" in new OrderStorePaymentTransactionSpecContext {
        val response = when(orderStorePaymentTransaction(id, upsertion))
          .expectRequest(implicit request => assertOrderStorePaymentTransaction)
          .respondWith(orderFileName)

        response.await.map(_.data) ==== Right(expectedOrder)
      }

      "parse rejection" in new OrderStorePaymentTransactionSpecContext {
        val endpoint = completeUri(s"/v1/orders.store_payment_transaction?$params")
        val expectedRejection = ClientError(endpoint, "The supplied authentication is invalid")
        val response = when(orderStorePaymentTransaction(id, upsertion))
          .expectRequest(implicit request => assertOrderStorePaymentTransaction)
          .respondWithRejection("/core/responses/auth_rejection.json")
        response.await ==== Left(expectedRejection)
      }
    }
  }
}
