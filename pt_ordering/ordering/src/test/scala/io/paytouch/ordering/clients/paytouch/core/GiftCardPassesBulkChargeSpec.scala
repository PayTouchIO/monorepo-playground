package io.paytouch.ordering.clients.paytouch.core

import java.util.UUID

import akka.http.scaladsl.model._

import cats.implicits._

import io.paytouch._
import io.paytouch.implicits._

import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.errors.ClientError
import io.paytouch.ordering.utils.CommonArbitraries
import io.paytouch.ordering.utils.OrdersGetFixture

class GiftCardPassesBulkChargeSpec extends PtCoreClientSpec with CommonArbitraries {
  trait OrderFixture extends OrdersGetFixture { self: GiftCardPassesBulkChargeSpecContext =>
    val orderFileName = "/core/responses/orders_get.json"
    val expectedOrder = ordersGetFixture
  }

  abstract class GiftCardPassesBulkChargeSpecContext extends CoreClientSpecContext with OrderFixture {
    val id = OrderId("ac544e46-63a7-4d2f-81f9-53f68bcd02b2")
    val params = s"order_id=${id.value}"

    val bulkCharge: Seq[GiftCardPassCharge] =
      Seq(
        GiftCardPassCharge(
          giftCardPassId = io.paytouch.GiftCardPass.Id("ac544e46-63a7-4d2f-81f9-53f68bcd02b2"),
          amount = 20,
        ),
      )

    def assertgiftCardPassesBulkCharge(implicit request: HttpRequest, authToken: HttpHeader) =
      assertUserAuthRequest(
        HttpMethods.POST,
        "/v1/gift_card_passes.bulk_charge",
        authToken,
        queryParams = params.some,
        body = bulkCharge.some,
      )
  }

  "CoreClient" should {
    "call gift_card_passes.bulk_charge" should {
      "store a payment transaction" in new GiftCardPassesBulkChargeSpecContext {
        val response = when(giftCardPassesBulkCharge(id, bulkCharge))
          .expectRequest(implicit request => assertgiftCardPassesBulkCharge)
          .respondWith(orderFileName)

        response.await.map(_.data) ==== Right(expectedOrder)
      }

      "parse rejection" in new GiftCardPassesBulkChargeSpecContext {
        val endpoint = completeUri(s"/v1/gift_card_passes.bulk_charge?$params")
        val expectedRejection = ClientError(endpoint, "The supplied authentication is invalid")
        val response = when(giftCardPassesBulkCharge(id, bulkCharge))
          .expectRequest(implicit request => assertgiftCardPassesBulkCharge)
          .respondWithRejection("/core/responses/auth_rejection.json")

        response.await ==== Left(expectedRejection)
      }
    }
  }
}
