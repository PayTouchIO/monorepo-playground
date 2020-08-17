package io.paytouch.core.resources.transferorders

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection

import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

@scala.annotation.nowarn("msg=Auto-application")
class TransferOrdersUpdateFSpec extends TransferOrdersFSpec {
  abstract class TransferOrdersUpdateFSpecContext extends TransferOrderResourceFSpecContext

  "POST /v1/transfer_orders.update" in {
    "if request has valid token" in {

      "if all the ids are valid" should {
        "update transfer order and return 201" in new TransferOrdersUpdateFSpecContext {
          val transferOrder = Factory.transferOrder(london, rome, user).create
          val product = Factory.simpleProduct(merchant).create

          val oldProduct = Factory.simpleProduct(merchant).create
          val oldTransferOrderProduct = Factory.transferOrderProduct(transferOrder, oldProduct).create

          val transferOrderProduct = random[TransferOrderProductUpsertion].copy(productId = product.id)

          val update = random[TransferOrderUpdate].copy(
            fromLocationId = Some(rome.id),
            toLocationId = Some(london.id),
            userId = Some(user.id),
            products = Some(Seq(transferOrderProduct)),
          )

          Post(s"/v1/transfer_orders.update?transfer_order_id=${transferOrder.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val transferOrder = responseAs[ApiResponse[TransferOrder]].data
            assertUpdate(transferOrder.id, update)

            transferOrderProductDao.findById(oldTransferOrderProduct.id).await should beNone
          }
        }
      }

      "if the from location id is not valid" should {
        "return 404" in new TransferOrdersUpdateFSpecContext {
          val transferOrder = Factory.transferOrder(london, rome, user).create

          val product = Factory.simpleProduct(merchant).create
          val transferOrderProduct = random[TransferOrderProductUpsertion].copy(productId = product.id)

          val update = random[TransferOrderUpdate].copy(
            fromLocationId = Some(UUID.randomUUID),
            toLocationId = Some(london.id),
            userId = Some(user.id),
            products = Some(Seq(transferOrderProduct)),
          )

          Post(s"/v1/transfer_orders.update?transfer_order_id=${transferOrder.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if the to location id is not valid" should {
        "return 404" in new TransferOrdersUpdateFSpecContext {
          val transferOrder = Factory.transferOrder(london, rome, user).create

          val product = Factory.simpleProduct(merchant).create
          val transferOrderProduct = random[TransferOrderProductUpsertion].copy(productId = product.id)

          val update = random[TransferOrderUpdate].copy(
            fromLocationId = Some(rome.id),
            toLocationId = Some(UUID.randomUUID),
            userId = Some(user.id),
            products = Some(Seq(transferOrderProduct)),
          )

          Post(s"/v1/transfer_orders.update?transfer_order_id=${transferOrder.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if the user id is not valid" should {
        "return 404" in new TransferOrdersUpdateFSpecContext {
          val transferOrder = Factory.transferOrder(london, rome, user).create

          val product = Factory.simpleProduct(merchant).create
          val transferOrderProduct = random[TransferOrderProductUpsertion].copy(productId = product.id)

          val update = random[TransferOrderUpdate].copy(
            fromLocationId = Some(rome.id),
            toLocationId = Some(london.id),
            userId = Some(UUID.randomUUID),
            products = Some(Seq(transferOrderProduct)),
          )

          Post(s"/v1/transfer_orders.update?transfer_order_id=${transferOrder.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if the product id is not valid" should {
        "return 404" in new TransferOrdersUpdateFSpecContext {
          val transferOrder = Factory.transferOrder(london, rome, user).create
          val transferOrderProduct = random[TransferOrderProductUpsertion]

          val update = random[TransferOrderUpdate].copy(
            fromLocationId = Some(rome.id),
            toLocationId = Some(london.id),
            userId = Some(user.id),
            products = Some(Seq(transferOrderProduct)),
          )

          Post(s"/v1/transfer_orders.update?transfer_order_id=${transferOrder.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new TransferOrdersUpdateFSpecContext {
        val transferOrder = Factory.transferOrder(london, rome, user).create
        val update = random[TransferOrderUpdate]
        Post(s"/v1/transfer_orders.update?transfer_order_id=${transferOrder.id}", update)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
