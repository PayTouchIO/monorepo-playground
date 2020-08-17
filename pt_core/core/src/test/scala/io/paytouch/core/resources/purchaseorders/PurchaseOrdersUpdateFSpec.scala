package io.paytouch.core.resources.purchaseorders

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.data.model.enums.ReceivingObjectStatus
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class PurchaseOrdersUpdateFSpec extends PurchaseOrdersFSpec {

  abstract class PurchaseOrdersUpdateFSpecContext extends PurchaseOrderResourceFSpecContext {
    val supplier = Factory.supplier(merchant).create

    val product = Factory.simpleProduct(merchant).create
    val purchaseOrderProduct = random[PurchaseOrderProductUpsertion].copy(productId = product.id)

    val validUpdate = random[PurchaseOrderUpdate].copy(
      supplierId = Some(supplier.id),
      locationId = Some(rome.id),
      products = Some(Seq(purchaseOrderProduct)),
    )
  }

  "POST /v1/purchase_orders.update" in {
    "if request has valid token" in {

      "update purchase order and return 200" in new PurchaseOrdersUpdateFSpecContext {
        val purchaseOrder =
          Factory.purchaseOrderWithSupplier(supplier, rome, user, status = Some(ReceivingObjectStatus.Created)).create

        Post(s"/v1/purchase_orders.update?purchase_order_id=${purchaseOrder.id}", validUpdate)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val purchaseOrder = responseAs[ApiResponse[PurchaseOrder]].data
          assertUpdate(purchaseOrder.id, validUpdate)
        }
      }
    }

    "if request has invalid supplier id" should {
      "return 404" in new PurchaseOrdersUpdateFSpecContext {
        val purchaseOrder =
          Factory.purchaseOrderWithSupplier(supplier, rome, user, status = Some(ReceivingObjectStatus.Created)).create

        val update = validUpdate.copy(supplierId = Some(UUID.randomUUID))
        Post(s"/v1/purchase_orders.update?purchase_order_id=${purchaseOrder.id}", update)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NotFound)
        }
      }
    }

    "if request has invalid location id" should {
      "return 404" in new PurchaseOrdersUpdateFSpecContext {
        val purchaseOrder =
          Factory.purchaseOrderWithSupplier(supplier, rome, user, status = Some(ReceivingObjectStatus.Created)).create
        val update = validUpdate.copy(locationId = Some(UUID.randomUUID))

        Post(s"/v1/purchase_orders.update?purchase_order_id=${purchaseOrder.id}", update)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NotFound)
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new PurchaseOrdersUpdateFSpecContext {
        val newPurchaseOrderId = UUID.randomUUID
        val update = random[PurchaseOrderUpdate]
        Post(s"/v1/purchase_orders.update?purchase_order_id=$newPurchaseOrderId", update)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
