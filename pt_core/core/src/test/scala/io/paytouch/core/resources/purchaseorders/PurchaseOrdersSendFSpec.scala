package io.paytouch.core.resources.purchaseorders

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class PurchaseOrdersSendFSpec extends PurchaseOrdersFSpec {

  "POST /v1/purchase_orders.send" in {
    "if request has valid token" in {
      "if purchase order belongs to the current merchant" should {
        "send a purchase order" in new PurchaseOrderResourceFSpecContext {
          val purchaseOrder = Factory.purchaseOrder(merchant, rome, user).create

          Post(s"/v1/purchase_orders.send?purchase_order_id=${purchaseOrder.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val entity = responseAs[ApiResponse[PurchaseOrder]].data
            assertResponse(entity, purchaseOrder.copy(sent = true))
          }
        }
      }
    }
  }
}
