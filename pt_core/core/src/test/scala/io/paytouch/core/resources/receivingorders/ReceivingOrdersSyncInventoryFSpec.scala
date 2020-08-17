package io.paytouch.core.resources.receivingorders

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.data.model.{ PurchaseOrderRecord, ReceivingOrderRecord, SlickRecord, TransferOrderRecord }
import io.paytouch.core.data.model.enums.{ ReceivingObjectStatus, ReceivingOrderStatus }
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class ReceivingOrdersSyncInventoryFSpec extends ReceivingOrdersFSpec {

  abstract class ReceivingOrdersSyncInventoryFSpecContext extends ReceivingOrderResourceFSpecContext {
    val stockDao = daos.stockDao
    val purchaseOrderDao = daos.purchaseOrderDao
    val transferOrderDao = daos.transferOrderDao
  }

  "POST /v1/receiving_orders.sync_inventory" in {
    "if request has valid token" in {

      "mark the receiving order as synced + received and create stocks if non-existing" in new ReceivingOrdersSyncInventoryFSpecContext {
        val quantityChange: BigDecimal = 5

        val simpleProduct = Factory.simpleProduct(merchant).create
        val productLocation = Factory.productLocation(simpleProduct, rome).create

        val receivingOrder = Factory.receivingOrder(rome, user, synced = Some(false)).create
        Factory.receivingOrderProduct(receivingOrder, simpleProduct, quantity = Some(quantityChange)).create

        Post(s"/v1/receiving_orders.sync_inventory?receiving_order_id=${receivingOrder.id}")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val receivingOrderResponse = responseAs[ApiResponse[ReceivingOrder]].data
          assertResponse(
            receivingOrderResponse,
            receivingOrder.copy(synced = true, status = ReceivingOrderStatus.Received),
          )
          val stocks = stockDao.findByProductIdsAndLocationIds(Seq(simpleProduct.id), Seq(rome.id)).await
          stocks.find(s => s.productId == simpleProduct.id).get.quantity ==== quantityChange
        }
      }

      "mark the receiving order as synced + received and update stocks if existing" in new ReceivingOrdersSyncInventoryFSpecContext {
        val stockQuantity: BigDecimal = 10
        val quantityChange: BigDecimal = 5

        val simpleProduct = Factory.simpleProduct(merchant).create
        val productLocation = Factory.productLocation(simpleProduct, rome).create
        val stock = Factory.stock(productLocation, quantity = Some(stockQuantity)).create

        val receivingOrder = Factory.receivingOrder(rome, user, synced = Some(false)).create
        Factory.receivingOrderProduct(receivingOrder, simpleProduct, quantity = Some(quantityChange)).create

        Post(s"/v1/receiving_orders.sync_inventory?receiving_order_id=${receivingOrder.id}")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val receivingOrderResponse = responseAs[ApiResponse[ReceivingOrder]].data
          assertResponse(
            receivingOrderResponse,
            receivingOrder.copy(synced = true, status = ReceivingOrderStatus.Received),
          )
          stockDao.findById(stock.id).await.get.quantity ==== stockQuantity + quantityChange
        }
      }

      trait ReceivingObjectFixtures[A <: SlickRecord] { self: ReceivingOrdersSyncInventoryFSpecContext =>
        val receivingObject: A
        val receivingOrder: ReceivingOrderRecord

        val stockQuantity: BigDecimal = 10
        val quantityChange: BigDecimal = 5

        val simpleProduct = Factory.simpleProduct(merchant).create

        val productLondon = Factory.productLocation(simpleProduct, london).create
        val stockLondon = Factory.stock(productLondon, quantity = Some(stockQuantity)).create

        val productRome = Factory.productLocation(simpleProduct, rome).create
        val stockRome = Factory.stock(productRome, quantity = Some(stockQuantity)).create

        def assertSynced(receivingOrderResponse: ReceivingOrder) =
          assertResponse(
            receivingOrderResponse,
            receivingOrder.copy(synced = true, status = ReceivingOrderStatus.Received),
          )

      }

      "when transfer order is involved" in {
        "mark the receiving order as synced + received and update stocks from both from and to locations" in new ReceivingOrdersSyncInventoryFSpecContext
          with ReceivingObjectFixtures[TransferOrderRecord] {
          val receivingObject =
            Factory.transferOrder(london, rome, user, status = Some(ReceivingObjectStatus.Created)).create
          Factory.transferOrderProduct(receivingObject, simpleProduct, quantity = Some(quantityChange)).create

          val receivingOrder = Factory.receivingOrderOfTransfer(rome, user, receivingObject).create
          Factory.receivingOrderProduct(receivingOrder, simpleProduct, quantity = Some(quantityChange)).create

          Post(s"/v1/receiving_orders.sync_inventory?receiving_order_id=${receivingOrder.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val receivingOrderResponse = responseAs[ApiResponse[ReceivingOrder]].data
            assertSynced(receivingOrderResponse)
            stockDao.findById(stockRome.id).await.get.quantity ==== stockQuantity + quantityChange
            stockDao.findById(stockLondon.id).await.get.quantity ==== stockQuantity - quantityChange
            transferOrderDao.findById(receivingObject.id).await.get.status === ReceivingObjectStatus.Completed
          }
        }
      }

      "when purchase order is involved" in {
        "mark the receiving order as synced + received and update stocks from both from and to locations" in new ReceivingOrdersSyncInventoryFSpecContext
          with ReceivingObjectFixtures[PurchaseOrderRecord] {
          val receivingObject =
            Factory.purchaseOrder(merchant, london, user, status = Some(ReceivingObjectStatus.Created)).create
          Factory.purchaseOrderProduct(receivingObject, simpleProduct, quantity = Some(quantityChange)).create

          val receivingOrder = Factory.receivingOrderOfPurchaseOrder(london, user, receivingObject).create
          Factory.receivingOrderProduct(receivingOrder, simpleProduct, quantity = Some(quantityChange)).create

          Post(s"/v1/receiving_orders.sync_inventory?receiving_order_id=${receivingOrder.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val receivingOrderResponse = responseAs[ApiResponse[ReceivingOrder]].data
            assertSynced(receivingOrderResponse)
            stockDao.findById(stockLondon.id).await.get.quantity ==== stockQuantity + quantityChange
            purchaseOrderDao.findById(receivingObject.id).await.get.status === ReceivingObjectStatus.Completed
          }
        }
      }

      "if receiving order is already being synced" should {
        "return 400" in new ReceivingOrdersSyncInventoryFSpecContext {
          val receivingOrder = Factory.receivingOrder(rome, user, synced = Some(true)).create

          Post(s"/v1/receiving_orders.sync_inventory?receiving_order_id=${receivingOrder.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
          }
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new ReceivingOrdersSyncInventoryFSpecContext {
        val newReceivingOrderId = UUID.randomUUID
        Post(s"/v1/receiving_orders.sync_inventory?receiving_order_id=$newReceivingOrderId")
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
