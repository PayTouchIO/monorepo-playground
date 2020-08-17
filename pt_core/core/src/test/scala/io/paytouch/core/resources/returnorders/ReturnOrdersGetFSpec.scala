package io.paytouch.core.resources.returnorders

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.{ ReturnOrder => ReturnOrderEntity, _ }
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class ReturnOrdersGetFSpec extends ReturnOrdersFSpec {

  abstract class ReturnOrderGetFSpecContext extends ReturnOrderResourceFSpecContext

  "GET /v1/return_orders.get?return_order_id=$" in {
    "if request has valid token" in {

      "if the return order belongs to the merchant" in {
        "with no parameters" should {
          "return a return order" in new ReturnOrderGetFSpecContext {
            val supplier = Factory.supplier(merchant).create
            val returnOrder = Factory.returnOrder(user, supplier, rome).create

            val product = Factory.simpleProduct(merchant).create
            val returnOrderProduct = Factory.returnOrderProduct(returnOrder, product).create

            Get(s"/v1/return_orders.get?return_order_id=${returnOrder.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val entity = responseAs[ApiResponse[ReturnOrderEntity]].data
              assertResponse(entity, returnOrder)
            }
          }
        }

        "with expand[]=user" should {
          "return a return order" in new ReturnOrderGetFSpecContext {
            val supplier = Factory.supplier(merchant).create
            val returnOrder = Factory.returnOrder(user, supplier, rome).create

            val product = Factory.simpleProduct(merchant).create
            val returnOrderProduct = Factory.returnOrderProduct(returnOrder, product).create

            Get(s"/v1/return_orders.get?return_order_id=${returnOrder.id}&expand[]=user")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val entity = responseAs[ApiResponse[ReturnOrderEntity]].data
              assertResponse(entity, returnOrder, user = Some(user))
            }
          }
        }

        "with expand[]=supplier" should {
          "return a return order" in new ReturnOrderGetFSpecContext {
            val supplier = Factory.supplier(merchant).create
            val returnOrder = Factory.returnOrder(user, supplier, rome).create

            val product = Factory.simpleProduct(merchant).create
            val returnOrderProduct = Factory.returnOrderProduct(returnOrder, product).create

            Get(s"/v1/return_orders.get?return_order_id=${returnOrder.id}&expand[]=supplier")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val entity = responseAs[ApiResponse[ReturnOrderEntity]].data
              assertResponse(entity, returnOrder, supplier = Some(supplier))
            }
          }
        }

        "with expand[]=products_count" should {
          "return a return order" in new ReturnOrderGetFSpecContext {
            val supplier = Factory.supplier(merchant).create
            val returnOrder = Factory.returnOrder(user, supplier, rome).create

            val product1 = Factory.simpleProduct(merchant).create
            val product2 = Factory.simpleProduct(merchant).create

            val returnOrderProduct1 = Factory.returnOrderProduct(returnOrder, product1, quantity = Some(3)).create
            val returnOrderProduct2 = Factory.returnOrderProduct(returnOrder, product2, quantity = Some(4)).create

            Get(s"/v1/return_orders.get?return_order_id=${returnOrder.id}&expand[]=products_count")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val entity = responseAs[ApiResponse[ReturnOrderEntity]].data
              assertResponse(entity, returnOrder, productsCount = Some(7))
            }
          }
        }

        "with expand[]=purchase_order" should {
          "return a return order" in new ReturnOrderGetFSpecContext {
            val supplier = Factory.supplier(merchant).create
            val purchaseOrder = Factory.purchaseOrder(merchant, rome, user).create
            val returnOrder = Factory.returnOrder(user, supplier, rome, purchaseOrder = Some(purchaseOrder)).create

            val product = Factory.simpleProduct(merchant).create
            val returnOrderProduct = Factory.returnOrderProduct(returnOrder, product).create

            Get(s"/v1/return_orders.get?return_order_id=${returnOrder.id}&expand[]=purchase_order")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val entity = responseAs[ApiResponse[ReturnOrderEntity]].data
              assertResponse(entity, returnOrder, purchaseOrder = Some(purchaseOrder))
            }
          }
        }
      }

      "if the return order does not belong to the merchant" should {
        "return 404" in new ReturnOrderGetFSpecContext {
          val competitor = Factory.merchant.create
          val competitorUser = Factory.user(competitor).create
          val competitorLocation = Factory.location(competitor).create
          val competitorSupplier = Factory.supplier(competitor).create
          val competitorReturnOrder =
            Factory.returnOrder(competitorUser, competitorSupplier, competitorLocation).create

          Get(s"/v1/return_orders.get?return_order_id=${competitorReturnOrder.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }
  }
}
