package io.paytouch.ordering.resources.carts

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{ AuthenticationFailedRejection, ValidationRejection }

import cats.implicits._

import io.paytouch.implicits._

import io.paytouch.ordering.clients.paytouch.core.{ CoreEmbeddedError, CoreEmbeddedErrorResponse }
import io.paytouch.ordering.clients.paytouch.core.entities.enums.AcceptanceStatus
import io.paytouch.ordering.entities._
import io.paytouch.ordering.entities.enums.{ CartStatus, OrderType }
import io.paytouch.ordering.stubs.PtCoreStubData
import io.paytouch.ordering.utils.{ FixtureDaoFactory => Factory }

class CartsSyncFSpec extends CartsFSpec {

  abstract class CartsSyncFSpecContext extends CartResourceFSpecContext {
    lazy val cart =
      Factory
        .cart(
          londonStore,
          line1 = None,
          city = None,
          postalCode = None,
          tipAmount = None,
          paymentMethodType = None,
          orderType = None,
        )
        .create

    implicit val storeContext = StoreContext.fromRecord(londonStore)
    implicit val authHeader = ptCoreClient.generateAuthHeaderForCore

    lazy val acceptanceStatus: AcceptanceStatus = AcceptanceStatus.Open

    lazy val order =
      randomOrder(acceptanceStatus.some)

    PtCoreStubData.recordOrder(order)
  }

  "POST /v1/carts.sync?cart_id=<cart-id>" in {
    "valid request" in {
      "syncs the cart" in new CartsSyncFSpecContext {
        Post(s"/v1/carts.sync?cart_id=${cart.id}")
          .addHeader(storeAuthorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val entity = responseAs[ApiResponse[Cart]].data
          assertResponseById(cart.id, entity)

          assertCartStatus(cart.id, status = CartStatus.New, synced = Some(true))
        }
      }

      "if cart is paid" should {
        "reject the request" in new CartsSyncFSpecContext {
          override lazy val cart =
            Factory.cart(londonStore, orderId = Some(UUID.randomUUID), orderType = Some(OrderType.TakeOut)).create
          cart.status ==== CartStatus.Paid

          Post(s"/v1/carts.sync?cart_id=${cart.id}")
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("ImmutableCart")
          }
        }
      }
    }

    "if cart id doesn't exist" in {
      "return 404" in new CartsSyncFSpecContext {
        val invalidId = UUID.randomUUID

        Post(s"/v1/carts.sync?cart_id=$invalidId")
          .addHeader(storeAuthorizationHeader) ~> routes ~> check {
          rejection ==== ValidationRejection("cart id does not exist")
        }
      }
    }

    "if request has an invalid token" in {
      "reject the request" in new CartsSyncFSpecContext {
        val randomId = UUID.randomUUID
        Post(s"/v1/carts.sync?cart_id=$randomId")
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
