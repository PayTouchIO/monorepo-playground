package io.paytouch.core.resources.purchaseorders

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class PurchaseOrdersDeleteFSpec extends PurchaseOrdersFSpec {

  "POST /v1/purchase_orders.delete" in {
    "if request has valid token" in {
      "if purchase order belongs to the current merchant" should {
        "delete a purchase order" in new PurchaseOrderResourceFSpecContext {
          val purchaseOrder = Factory.purchaseOrder(merchant, rome, user).create

          Post(s"/v1/purchase_orders.delete", Ids(ids = Seq(purchaseOrder.id)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            purchaseOrderDao.findById(purchaseOrder.id).await should beEmpty
          }
        }
      }

      "if purchase order does not belongs to the current merchant" should {
        "NOT delete a purchase order" in new PurchaseOrderResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorLocation = Factory.location(competitor).create
          val competitorUser = Factory.user(competitor).create
          val competitorPurchaseOrder = Factory.purchaseOrder(competitor, competitorLocation, competitorUser).create

          Post(s"/v1/purchase_orders.delete", Ids(ids = Seq(competitorPurchaseOrder.id)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            purchaseOrderDao.findById(competitorPurchaseOrder.id).await should beSome
          }
        }
      }

    }
  }
}
