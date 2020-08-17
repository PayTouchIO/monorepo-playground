package io.paytouch.core.resources.purchaseorders

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class PurchaseOrdersGetFSpec extends PurchaseOrdersFSpec {

  abstract class PurchaseOrdersGetFSpecContext extends PurchaseOrderResourceFSpecContext

  "GET /v1/purchase_orders.get?purchase_order_id=$" in {
    "if request has valid token" in {

      "if the purchase order exists" should {

        "with no parameters" should {
          "return the purchase order" in new PurchaseOrdersGetFSpecContext {
            val purchaseOrderRecord = Factory.purchaseOrder(merchant, london, user).create

            Get(s"/v1/purchase_orders.get?purchase_order_id=${purchaseOrderRecord.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val entity = responseAs[ApiResponse[PurchaseOrder]].data
              assertResponse(entity, purchaseOrderRecord)
            }
          }
        }

        "with expand[]=supplier" should {
          "return the purchase order" in new PurchaseOrdersGetFSpecContext {
            val supplier = Factory.supplier(merchant).create
            val purchaseOrderRecord = Factory.purchaseOrderWithSupplier(supplier, london, user).create

            Get(s"/v1/purchase_orders.get?purchase_order_id=${purchaseOrderRecord.id}&expand[]=supplier")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val entity = responseAs[ApiResponse[PurchaseOrder]].data
              assertResponse(entity, purchaseOrderRecord, supplier = Some(supplier))
            }
          }
        }

        "with expand[]=location" should {
          "return the purchase order" in new PurchaseOrdersGetFSpecContext {
            val purchaseOrderRecord = Factory.purchaseOrder(merchant, london, user).create

            Get(s"/v1/purchase_orders.get?purchase_order_id=${purchaseOrderRecord.id}&expand[]=location")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val entity = responseAs[ApiResponse[PurchaseOrder]].data
              assertResponse(entity, purchaseOrderRecord, location = Some(london))
            }
          }
        }

        "with expand[]=user" should {
          "return the purchase order" in new PurchaseOrdersGetFSpecContext {
            val purchaseOrderRecord = Factory.purchaseOrder(merchant, london, user).create

            Get(s"/v1/purchase_orders.get?purchase_order_id=${purchaseOrderRecord.id}&expand[]=user")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val entity = responseAs[ApiResponse[PurchaseOrder]].data
              assertResponse(entity, purchaseOrderRecord, user = Some(user))
            }
          }
        }

        "with expand[]=receiving_orders" should {
          "return the purchase order" in new PurchaseOrdersGetFSpecContext {
            val supplier = Factory.supplier(merchant).create
            val purchaseOrderRecord = Factory.purchaseOrderWithSupplier(supplier, london, user).create
            val receivingOrderA = Factory.receivingOrderOfPurchaseOrder(london, user, purchaseOrderRecord).create
            val receivingOrderB = Factory.receivingOrderOfPurchaseOrder(london, user, purchaseOrderRecord).create

            Get(s"/v1/purchase_orders.get?purchase_order_id=${purchaseOrderRecord.id}&expand[]=receiving_orders")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val entity = responseAs[ApiResponse[PurchaseOrder]].data
              assertResponse(entity, purchaseOrderRecord, receivingOrders = Some(Seq(receivingOrderA, receivingOrderB)))
            }
          }
        }
      }

      "if the purchase order does not belong to the merchant" should {
        "return 404" in new PurchaseOrdersGetFSpecContext {
          val competitor = Factory.merchant.create
          val locationCompetitor = Factory.location(competitor).create
          val supplierCompetitor = Factory.supplier(competitor).create
          val userCompetitor = Factory.user(competitor).create
          val purchaseOrderCompetitor =
            Factory.purchaseOrderWithSupplier(supplierCompetitor, locationCompetitor, userCompetitor).create

          Get(s"/v1/purchase_orders.get?purchase_order_id=${supplierCompetitor.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if the purchase order does not exist" should {
        "return 404" in new PurchaseOrdersGetFSpecContext {
          Get(s"/v1/purchase_orders.get?purchase_order_id=${UUID.randomUUID}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }
  }
}
