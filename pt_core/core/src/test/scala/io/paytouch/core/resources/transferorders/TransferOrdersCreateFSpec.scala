package io.paytouch.core.resources.transferorders

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection

import io.paytouch.core.data.model.NextNumberRecord
import io.paytouch.core.data.model.enums._
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

@scala.annotation.nowarn("msg=Auto-application")
class TransferOrdersCreateFSpec extends TransferOrdersFSpec {
  abstract class TransferOrdersCreateFSpecContext extends TransferOrderResourceFSpecContext {
    val nextNumberDao = daos.nextNumberDao

    def assertNextTransferOrderNumber(nextNumber: Int) = {
      val record = nextNumberDao
        .findByScopeAndType(Scope.fromMerchantId(merchant.id), NextNumberType.TransferOrder)
        .await
      record should beSome[NextNumberRecord]
      record.get.nextVal ==== nextNumber
    }
  }

  "POST /v1/transfer_orders.create" in {
    "if request has valid token" in {

      "if all the ids are valid" should {
        "create transfer order and return 201" in new TransferOrdersCreateFSpecContext {
          val newTransferOrderId = UUID.randomUUID

          val product = Factory.simpleProduct(merchant).create
          val transferOrderProduct = random[TransferOrderProductUpsertion].copy(productId = product.id)

          val creation = random[TransferOrderCreation].copy(
            fromLocationId = rome.id,
            toLocationId = london.id,
            userId = user.id,
            products = Seq(transferOrderProduct),
          )

          Post(s"/v1/transfer_orders.create?transfer_order_id=$newTransferOrderId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()

            responseAs[ApiResponse[TransferOrder]].data
            assertCreation(newTransferOrderId, creation)
            assertNextTransferOrderNumber(nextNumber = 2)
          }
        }
      }

      "if the from location id is not valid" should {
        "return 404" in new TransferOrdersCreateFSpecContext {
          val newTransferOrderId = UUID.randomUUID

          val product = Factory.simpleProduct(merchant).create
          val transferOrderProduct = random[TransferOrderProductUpsertion].copy(productId = product.id)

          val creation = random[TransferOrderCreation].copy(
            toLocationId = london.id,
            userId = user.id,
            products = Seq(transferOrderProduct),
          )

          Post(s"/v1/transfer_orders.create?transfer_order_id=$newTransferOrderId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if the to location id is not valid" should {
        "return 404" in new TransferOrdersCreateFSpecContext {
          val newTransferOrderId = UUID.randomUUID

          val product = Factory.simpleProduct(merchant).create
          val transferOrderProduct = random[TransferOrderProductUpsertion].copy(productId = product.id)

          val creation = random[TransferOrderCreation].copy(
            fromLocationId = rome.id,
            userId = user.id,
            products = Seq(transferOrderProduct),
          )

          Post(s"/v1/transfer_orders.create?transfer_order_id=$newTransferOrderId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if the user id is not valid" should {
        "return 404" in new TransferOrdersCreateFSpecContext {
          val newTransferOrderId = UUID.randomUUID

          val product = Factory.simpleProduct(merchant).create
          val transferOrderProduct = random[TransferOrderProductUpsertion].copy(productId = product.id)

          val creation = random[TransferOrderCreation].copy(
            fromLocationId = rome.id,
            toLocationId = london.id,
            products = Seq(transferOrderProduct),
          )

          Post(s"/v1/transfer_orders.create?transfer_order_id=$newTransferOrderId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if the product id is not valid" should {
        "return 404" in new TransferOrdersCreateFSpecContext {
          val newTransferOrderId = UUID.randomUUID
          val transferOrderProduct = random[TransferOrderProductUpsertion]

          val creation = random[TransferOrderCreation].copy(
            fromLocationId = rome.id,
            toLocationId = london.id,
            userId = user.id,
            products = Seq(transferOrderProduct),
          )

          Post(s"/v1/transfer_orders.create?transfer_order_id=$newTransferOrderId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new TransferOrdersCreateFSpecContext {
        val newTransferOrderId = UUID.randomUUID
        val creation = random[TransferOrderCreation]
        Post(s"/v1/transfer_orders.create?transfer_order_id=$newTransferOrderId", creation)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
