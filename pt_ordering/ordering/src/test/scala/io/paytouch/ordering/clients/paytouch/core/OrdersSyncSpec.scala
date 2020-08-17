package io.paytouch.ordering.clients.paytouch.core

import java.util.UUID

import akka.http.scaladsl.model._
import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.errors.ClientError
import io.paytouch.ordering.utils.CommonArbitraries
import io.paytouch.ordering.utils.OrdersGetFixture

class OrdersSyncSpec extends PtCoreClientSpec with CommonArbitraries {

  abstract class OrdersSyncSpecContext extends CoreClientSpecContext {
    val bundles =
      random[OrderBundleUpsertion](3).map(
        _.copy(orderBundleSets =
          random[OrderBundleSetUpsertion](2).map(_.copy(orderBundleOptions = random[OrderBundleOptionUpsertion](2))),
        ),
      )

    @scala.annotation.nowarn("msg=Auto-application")
    val upsertion = random[OrderUpsertion].copy(bundles = bundles)
    val id: UUID = "63f291ce-da3f-35da-a050-9bc51f80eee6"

    val params = s"order_id=$id"

    def assertOrdersSync(implicit request: HttpRequest, authToken: HttpHeader) =
      assertUserAuthRequest(
        HttpMethods.POST,
        "/v1/orders.sync",
        authToken,
        queryParams = Some(params),
        body = Some(upsertion),
      )

    def assertOrdersValidatedSync(implicit request: HttpRequest, authToken: HttpHeader) =
      assertUserAuthRequest(
        HttpMethods.POST,
        "/v1/orders.validated_sync",
        authToken,
        queryParams = Some(params),
        body = Some(upsertion),
      )

  }

  "CoreClient" should {

    "call orders.sync" should {

      "parse a order" in new OrdersSyncSpecContext with OrderFixture {

        val response = when(ordersSync(id, upsertion))
          .expectRequest(implicit request => assertOrdersSync)
          .respondWith(orderFileName)
        response.await.map(_.data) ==== Right(expectedOrder)
      }

      "parse rejection" in new OrdersSyncSpecContext {
        val endpoint = completeUri(s"/v1/orders.sync?$params")
        val expectedRejection = ClientError(endpoint, "The supplied authentication is invalid")
        val response = when(ordersSync(id, upsertion))
          .expectRequest(implicit request => assertOrdersSync)
          .respondWithRejection("/core/responses/auth_rejection.json")
        response.await ==== Left(expectedRejection)
      }
    }

    "call orders.validated_sync" should {

      "parse a order" in new OrdersSyncSpecContext with OrderFixture {

        val response = when(ordersValidatedSync(id, upsertion))
          .expectRequest(implicit request => assertOrdersValidatedSync)
          .respondWith(orderFileName)
        response.await.map(_.data) ==== Right(expectedOrder)
      }

      "parse rejection" in new OrdersSyncSpecContext {
        val endpoint = completeUri(s"/v1/orders.validated_sync?$params")
        val expectedRejection = ClientError(
          endpoint,
          errors = Seq(
            CoreEmbeddedError(
              code = "ProductOutOfStock",
              message = "Product is out of stock",
              values = Seq(
                "03d3fcd7-96d1-3868-abee-ab3d0cad7bb4",
              ),
            ),
          ),
        )

        val response = when(ordersValidatedSync(id, upsertion))
          .expectRequest(implicit request => assertOrdersValidatedSync)
          .respondWithRejection("/core/responses/out_of_stock_rejection.json")
        response.await ==== Left(expectedRejection)
      }
    }
  }

  trait OrderFixture extends OrdersGetFixture { self: OrdersSyncSpecContext =>
    val orderFileName = "/core/responses/orders_get.json"
    val expectedOrder = ordersGetFixture
  }
}
