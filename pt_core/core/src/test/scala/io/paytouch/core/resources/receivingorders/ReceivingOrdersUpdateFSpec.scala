package io.paytouch.core.resources.receivingorders

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.data.model.enums.ReceivingOrderObjectType
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

@scala.annotation.nowarn("msg=Auto-application")
class ReceivingOrdersUpdateFSpec extends ReceivingOrdersFSpec {
  abstract class ReceivingOrdersUpdateFSpecContext extends ReceivingOrderResourceFSpecContext

  "POST /v1/receiving_orders.update" in {
    "if request has valid token" in {
      "if receiving object id/type are none" should {
        "update receiving order and return 200" in new ReceivingOrdersUpdateFSpecContext {
          val receivingOrder = Factory.receivingOrder(rome, user).create

          val product = Factory.simpleProduct(merchant).create
          val receivingOrderProduct = random[ReceivingOrderProductUpsertion].copy(productId = product.id)

          val update =
            random[ReceivingOrderUpdate].copy(
              locationId = Some(rome.id),
              receivingObjectId = None,
              receivingObjectType = None,
              products = Some(Seq(receivingOrderProduct)),
            )

          Post(s"/v1/receiving_orders.update?receiving_order_id=${receivingOrder.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val receivingOrder = responseAs[ApiResponse[ReceivingOrder]].data
            assertUpdate(receivingOrder.id, update)
          }
        }
      }

      "if receiving object id/type reference an existing purchase order id" should {
        "update receiving order and return 201" in new ReceivingOrdersUpdateFSpecContext {
          val supplier = Factory.supplier(merchant).create
          val product = Factory.simpleProduct(merchant).create
          val purchaseOrder = Factory.purchaseOrderWithSupplier(supplier, rome, user).create
          val receivingOrder = Factory.receivingOrder(rome, user).create
          val receivingOrderProduct = random[ReceivingOrderProductUpsertion].copy(productId = product.id)

          val update =
            random[ReceivingOrderUpdate].copy(
              locationId = Some(rome.id),
              receivingObjectId = Some(purchaseOrder.id),
              receivingObjectType = Some(ReceivingOrderObjectType.PurchaseOrder),
              products = Some(Seq(receivingOrderProduct)),
            )

          Post(s"/v1/receiving_orders.update?receiving_order_id=${receivingOrder.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val receivingOrder = responseAs[ApiResponse[ReceivingOrder]].data
            assertUpdate(receivingOrder.id, update)
          }
        }
      }

      "if request has receiving order id but no type" should {
        "return 400" in new ReceivingOrdersUpdateFSpecContext {
          val receivingOrder = Factory.receivingOrder(rome, user).create

          val update = random[ReceivingOrderUpdate].copy(
            locationId = Some(rome.id),
            receivingObjectId = Some(UUID.randomUUID),
            receivingObjectType = None,
            products = None,
          )
          Post(s"/v1/receiving_orders.update?receiving_order_id=${receivingOrder.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
          }
        }
      }

      "if request has invalid purchase order id" should {
        "return 404" in new ReceivingOrdersUpdateFSpecContext {
          val receivingOrder = Factory.receivingOrder(rome, user).create

          val update = random[ReceivingOrderUpdate].copy(
            locationId = Some(rome.id),
            receivingObjectId = Some(UUID.randomUUID),
            receivingObjectType = Some(ReceivingOrderObjectType.PurchaseOrder),
            products = None,
          )
          Post(s"/v1/receiving_orders.update?receiving_order_id=${receivingOrder.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if request has invalid location id" should {
        "return 404" in new ReceivingOrdersUpdateFSpecContext {
          val receivingOrder = Factory.receivingOrder(rome, user).create

          val update = random[ReceivingOrderUpdate].copy(
            locationId = Some(UUID.randomUUID),
            receivingObjectId = None,
            receivingObjectType = None,
            products = None,
          )
          Post(s"/v1/receiving_orders.update?receiving_order_id=${receivingOrder.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if receiving object has already been synced and update a blocked field" should {
        "return 400" in new ReceivingOrdersUpdateFSpecContext {
          val receivingOrder = Factory.receivingOrder(rome, user, synced = Some(true)).create

          val update = random[ReceivingOrderUpdate].copy(locationId = Some(rome.id))
          Post(s"/v1/receiving_orders.update?receiving_order_id=${receivingOrder.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
          }
        }
      }

      "if receiving object has already been synced you can stil updated payment informations" should {
        "return 200" in new ReceivingOrdersUpdateFSpecContext {
          val receivingOrder = Factory.receivingOrder(rome, user, synced = Some(true)).create

          val update = ReceivingOrderUpdate.extractAfterSyncAllowedFields(random[ReceivingOrderUpdate])
          Post(s"/v1/receiving_orders.update?receiving_order_id=${receivingOrder.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
          }
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new ReceivingOrdersUpdateFSpecContext {
        val newReceivingOrderId = UUID.randomUUID
        val update = random[ReceivingOrderUpdate]
        Post(s"/v1/receiving_orders.update?receiving_order_id=$newReceivingOrderId", update)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
