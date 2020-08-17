package io.paytouch.core.resources.returnorders

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.data.model.enums.ReturnOrderStatus
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class ReturnOrdersSyncInventoryFSpec extends ReturnOrdersFSpec {

  abstract class ReturnOrdersSyncInventoryFSpecContext extends ReturnOrderResourceFSpecContext {
    val stockDao = daos.stockDao

    val stockQuantity: BigDecimal = 10
    val supplier = Factory.supplier(merchant).create
    val returnOrder = Factory.returnOrder(user, supplier, rome).create
    val product = Factory.simpleProduct(merchant).create
    val productLocation = Factory.productLocation(product, rome).create
    val stock = Factory.stock(productLocation, quantity = Some(stockQuantity)).create
  }

  "POST /v1/return_orders.sync_inventory" in {
    "if request has valid token" in {
      "mark the return order as synced + sent and remove quantity from stocks" in new ReturnOrdersSyncInventoryFSpecContext {

        val returnOrderProduct = Factory.returnOrderProduct(returnOrder, product, Some(stockQuantity)).create

        Post(s"/v1/return_orders.sync_inventory?return_order_id=${returnOrder.id}")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val returnOrderResponse = responseAs[ApiResponse[ReturnOrder]].data
          assertResponse(returnOrderResponse, returnOrder.copy(synced = true, status = ReturnOrderStatus.Sent))
          val stocks = stockDao.findByProductIdsAndLocationIds(Seq(product.id), Seq(rome.id)).await
          stocks.find(s => s.productId == product.id).get.quantity ==== 0
        }
      }

      "mark the return order as synced + sent and updated stocks has remaining quantities" in new ReturnOrdersSyncInventoryFSpecContext {

        val returnOrderProduct = Factory.returnOrderProduct(returnOrder, product, Some(stockQuantity - 5)).create

        Post(s"/v1/return_orders.sync_inventory?return_order_id=${returnOrder.id}")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val returnOrderResponse = responseAs[ApiResponse[ReturnOrder]].data
          assertResponse(returnOrderResponse, returnOrder.copy(synced = true, status = ReturnOrderStatus.Sent))
          val stocks = stockDao.findByProductIdsAndLocationIds(Seq(product.id), Seq(rome.id)).await
          stocks.find(s => s.productId == product.id).get.quantity ==== 5
        }
      }

      "if return order is already being synced" should {
        "return 400" in new ReturnOrdersSyncInventoryFSpecContext {

          override val returnOrder = Factory.returnOrder(user, supplier, rome, synced = Some(true)).create

          Post(s"/v1/return_orders.sync_inventory?return_order_id=${returnOrder.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
          }
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new ReturnOrdersSyncInventoryFSpecContext {

        val newReceivingOrderId = UUID.randomUUID

        Post(s"/v1/return_orders.sync_inventory?return_order_id=${returnOrder.id}")
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
