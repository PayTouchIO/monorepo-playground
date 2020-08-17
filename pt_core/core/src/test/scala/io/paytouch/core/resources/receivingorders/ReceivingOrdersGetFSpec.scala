package io.paytouch.core.resources.receivingorders

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.{ ReceivingOrder => ReceivingOrderEntity, _ }
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class ReceivingOrdersGetFSpec extends ReceivingOrdersFSpec {

  "GET /v1/receiving_orders.get?receiving_order_id=$" in {
    "if request has valid token" in {

      "if the receiving order belongs to the merchant" in {
        "with no parameters" should {
          "return a receiving order" in new ReceivingOrderResourceFSpecContext {
            val receivingOrder = Factory.receivingOrder(rome, user).create

            Get(s"/v1/receiving_orders.get?receiving_order_id=${receivingOrder.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val entity = responseAs[ApiResponse[ReceivingOrderEntity]].data
              assertResponse(entity, receivingOrder)
            }
          }
        }

        "with expand[]=location" should {
          "return a receiving order" in new ReceivingOrderResourceFSpecContext {
            val receivingOrder = Factory.receivingOrder(rome, user).create

            Get(s"/v1/receiving_orders.get?receiving_order_id=${receivingOrder.id}&expand[]=location")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val entity = responseAs[ApiResponse[ReceivingOrderEntity]].data
              assertResponse(entity, receivingOrder, location = Some(rome))
            }
          }
        }

        "with expand[]=purchase_order" should {
          "return a receiving order" in new ReceivingOrderResourceFSpecContext {
            val purchaseOrder = Factory.purchaseOrder(merchant, rome, user).create
            val receivingOrder = Factory.receivingOrderOfPurchaseOrder(rome, user, purchaseOrder).create

            Get(s"/v1/receiving_orders.get?receiving_order_id=${receivingOrder.id}&expand[]=purchase_order")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val entity = responseAs[ApiResponse[ReceivingOrderEntity]].data
              assertResponse(entity, receivingOrder, purchaseOrder = Some(purchaseOrder))
            }
          }
        }

        "with expand[]=transfer_order" should {
          "return a receiving order" in new ReceivingOrderResourceFSpecContext {
            val supplier = Factory.supplier(merchant).create
            val transferOrder = Factory.transferOrder(london, rome, user).create
            val receivingOrder = Factory.receivingOrderOfTransfer(rome, user, transferOrder).create

            Get(s"/v1/receiving_orders.get?receiving_order_id=${receivingOrder.id}&expand[]=transfer_order")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val entity = responseAs[ApiResponse[ReceivingOrderEntity]].data
              assertResponse(entity, receivingOrder, transferOrder = Some(transferOrder))
            }
          }
        }

        "with expand[]=user" should {
          "return a receiving order" in new ReceivingOrderResourceFSpecContext {
            val receivingOrder = Factory.receivingOrder(rome, user).create

            Get(s"/v1/receiving_orders.get?receiving_order_id=${receivingOrder.id}&expand[]=user")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val entity = responseAs[ApiResponse[ReceivingOrderEntity]].data
              assertResponse(entity, receivingOrder, user = Some(user))
            }
          }
        }
      }

      "if the receiving order does not belong to the merchant" should {
        "return 404" in new ReceivingOrderResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorUser = Factory.user(competitor).create
          val competitorLocation = Factory.location(competitor).create
          val competitorReceivingOrder = Factory.receivingOrder(competitorLocation, competitorUser).create

          Get(s"/v1/receiving_orders.get?receiving_order_id=${competitorReceivingOrder.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }
  }
}
