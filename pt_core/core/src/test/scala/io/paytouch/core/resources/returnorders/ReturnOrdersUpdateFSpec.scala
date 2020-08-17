package io.paytouch.core.resources.returnorders

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection

import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

@scala.annotation.nowarn("msg=Auto-application")
class ReturnOrdersUpdateFSpec extends ReturnOrdersFSpec {
  abstract class ReturnOrdersUpdateFSpecContext extends ReturnOrderResourceFSpecContext {
    val supplier = Factory.supplier(merchant).create
  }

  "POST /v1/return_orders.update" in {
    "if request has valid token" in {

      "if all the ids are valid" should {
        "update return order and return 201" in new ReturnOrdersUpdateFSpecContext {
          val returnOrder = Factory.returnOrder(user, supplier, rome).create

          val product = Factory.simpleProduct(merchant).create
          val returnOrderProduct = random[ReturnOrderProductUpsertion].copy(productId = product.id)

          val update = random[ReturnOrderUpdate].copy(
            supplierId = Some(supplier.id),
            locationId = Some(london.id),
            userId = Some(user.id),
            products = Some(Seq(returnOrderProduct)),
          )

          Post(s"/v1/return_orders.update?return_order_id=${returnOrder.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val returnOrder = responseAs[ApiResponse[ReturnOrder]].data
            assertUpdate(returnOrder.id, update)
          }
        }
      }

      "if the supplier id is not valid" should {
        "return 404" in new ReturnOrdersUpdateFSpecContext {
          val returnOrder = Factory.returnOrder(user, supplier, rome).create

          val product = Factory.simpleProduct(merchant).create
          val returnOrderProduct = random[ReturnOrderProductUpsertion].copy(productId = product.id)

          val update = random[ReturnOrderUpdate].copy(
            supplierId = Some(UUID.randomUUID),
            locationId = Some(london.id),
            userId = Some(user.id),
            products = Some(Seq(returnOrderProduct)),
          )

          Post(s"/v1/return_orders.update?return_order_id=${returnOrder.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if the location id is not valid" should {
        "return 404" in new ReturnOrdersUpdateFSpecContext {
          val returnOrder = Factory.returnOrder(user, supplier, rome).create

          val product = Factory.simpleProduct(merchant).create
          val returnOrderProduct = random[ReturnOrderProductUpsertion].copy(productId = product.id)

          val update = random[ReturnOrderUpdate].copy(
            supplierId = Some(supplier.id),
            locationId = Some(UUID.randomUUID),
            userId = Some(user.id),
            products = Some(Seq(returnOrderProduct)),
          )

          Post(s"/v1/return_orders.update?return_order_id=${returnOrder.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if the user id is not valid" should {
        "return 404" in new ReturnOrdersUpdateFSpecContext {
          val returnOrder = Factory.returnOrder(user, supplier, rome).create

          val product = Factory.simpleProduct(merchant).create
          val returnOrderProduct = random[ReturnOrderProductUpsertion].copy(productId = product.id)

          val update = random[ReturnOrderUpdate].copy(
            supplierId = Some(supplier.id),
            locationId = Some(london.id),
            userId = Some(UUID.randomUUID),
            products = Some(Seq(returnOrderProduct)),
          )

          Post(s"/v1/return_orders.update?return_order_id=${returnOrder.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if the product id is not valid" should {
        "return 404" in new ReturnOrdersUpdateFSpecContext {
          val returnOrder = Factory.returnOrder(user, supplier, rome).create
          val returnOrderProduct = random[ReturnOrderProductUpsertion]

          val update = random[ReturnOrderUpdate].copy(
            supplierId = Some(supplier.id),
            locationId = Some(london.id),
            userId = Some(user.id),
            products = Some(Seq(returnOrderProduct)),
          )

          Post(s"/v1/return_orders.update?return_order_id=${returnOrder.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new ReturnOrdersUpdateFSpecContext {
        val returnOrder = Factory.returnOrder(user, supplier, rome).create
        val update = random[ReturnOrderUpdate]
        Post(s"/v1/return_orders.update?return_order_id=${returnOrder.id}", update)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
