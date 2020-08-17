package io.paytouch.core.resources.receivingorders

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection

import io.paytouch.core.data.model.NextNumberRecord
import io.paytouch.core.data.model.enums.{ NextNumberType, ReceivingOrderObjectType, ScopeType }
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

@scala.annotation.nowarn("msg=Auto-application")
class ReceivingOrdersCreateFSpec extends ReceivingOrdersFSpec {
  abstract class ReceivingOrdersCreateFSpecContext extends ReceivingOrderResourceFSpecContext {
    val nextNumberDao = daos.nextNumberDao

    def assertNextReceivingOrderNumber(nextNumber: Int) = {
      val record = nextNumberDao
        .findByScopeAndType(Scope.fromMerchantId(merchant.id), NextNumberType.ReceivingOrder)
        .await
      record should beSome[NextNumberRecord]
      record.get.nextVal ==== nextNumber
    }
  }

  "POST /v1/receiving_orders.create" in {
    "if request has valid token" in {

      "if receiving object id/type are none" should {
        "create receiving order and return 201" in new ReceivingOrdersCreateFSpecContext {
          val newReceivingOrderId = UUID.randomUUID

          val product = Factory.simpleProduct(merchant).create
          val receivingOrderProduct = random[ReceivingOrderProductUpsertion].copy(productId = product.id)

          val creation = random[ReceivingOrderCreation].copy(
            locationId = rome.id,
            receivingObjectId = None,
            receivingObjectType = None,
            products = Seq(receivingOrderProduct),
          )

          Post(s"/v1/receiving_orders.create?receiving_order_id=$newReceivingOrderId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()

            responseAs[ApiResponse[ReceivingOrder]].data

            assertCreation(newReceivingOrderId, creation)
          }
        }
      }

      "if receiving object id/type reference an existing purchase order id" should {
        "create receiving order and return 201" in new ReceivingOrdersCreateFSpecContext {
          val newReceivingOrderId = UUID.randomUUID

          val product = Factory.simpleProduct(merchant).create
          val purchaseOrder = Factory.purchaseOrder(merchant, rome, user).create
          val receivingOrderProduct = random[ReceivingOrderProductUpsertion].copy(productId = product.id)

          val creation = random[ReceivingOrderCreation].copy(
            locationId = rome.id,
            receivingObjectId = Some(purchaseOrder.id),
            receivingObjectType = Some(ReceivingOrderObjectType.PurchaseOrder),
            products = Seq(receivingOrderProduct),
          )

          Post(s"/v1/receiving_orders.create?receiving_order_id=$newReceivingOrderId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()

            responseAs[ApiResponse[ReceivingOrder]].data
            assertCreation(newReceivingOrderId, creation)
          }
        }
      }
    }

    "if request has receiving order id but no type" should {
      "return 400" in new ReceivingOrdersCreateFSpecContext {
        val newReceivingOrderId = UUID.randomUUID
        val creation = random[ReceivingOrderCreation].copy(
          locationId = rome.id,
          receivingObjectId = Some(UUID.randomUUID),
          receivingObjectType = None,
          products = Seq.empty,
        )
        Post(s"/v1/receiving_orders.create?receiving_order_id=$newReceivingOrderId", creation)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.BadRequest)
        }
      }
    }

    "if request has invalid purchase order id" should {
      "return 404" in new ReceivingOrdersCreateFSpecContext {
        val newReceivingOrderId = UUID.randomUUID
        val creation = random[ReceivingOrderCreation].copy(
          locationId = rome.id,
          receivingObjectId = Some(UUID.randomUUID),
          receivingObjectType = Some(ReceivingOrderObjectType.PurchaseOrder),
          products = Seq.empty,
        )
        Post(s"/v1/receiving_orders.create?receiving_order_id=$newReceivingOrderId", creation)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NotFound)
        }
      }
    }

    "if request has invalid transfer order id" should {
      "return 404" in new ReceivingOrdersCreateFSpecContext {
        val newReceivingOrderId = UUID.randomUUID
        val creation =
          random[ReceivingOrderCreation].copy(
            locationId = rome.id,
            receivingObjectId = Some(UUID.randomUUID),
            receivingObjectType = Some(ReceivingOrderObjectType.Transfer),
            products = Seq.empty,
          )
        Post(s"/v1/receiving_orders.create?receiving_order_id=$newReceivingOrderId", creation)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NotFound)
        }
      }
    }

    "if request has invalid location id" should {
      "return 404" in new ReceivingOrdersCreateFSpecContext {
        val newReceivingOrderId = UUID.randomUUID
        val creation = random[ReceivingOrderCreation].copy(
          locationId = UUID.randomUUID,
          receivingObjectId = None,
          receivingObjectType = None,
          products = Seq.empty,
        )
        Post(s"/v1/receiving_orders.create?receiving_order_id=$newReceivingOrderId", creation)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NotFound)
        }
      }
    }

    "if request has invalid product id" should {
      "return 404" in new ReceivingOrdersCreateFSpecContext {
        val newReceivingOrderId = UUID.randomUUID

        val receivingOrderProduct = random[ReceivingOrderProductUpsertion]

        val creation =
          random[ReceivingOrderCreation].copy(
            locationId = rome.id,
            receivingObjectId = None,
            receivingObjectType = None,
            products = Seq(receivingOrderProduct),
          )

        Post(s"/v1/receiving_orders.create?receiving_order_id=$newReceivingOrderId", creation)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NotFound)
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new ReceivingOrdersCreateFSpecContext {
        val newReceivingOrderId = UUID.randomUUID
        val creation = random[ReceivingOrderCreation]
        Post(s"/v1/receiving_orders.create?receiving_order_id=$newReceivingOrderId", creation)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
