package io.paytouch.core.async.monitors

import akka.actor.Props
import io.paytouch.core.data.model.ReceivingOrderProductRecord
import io.paytouch.core.entities.ReceivingOrder
import io.paytouch.core.services.{ ProductService, PurchaseOrderService, ReceivingOrderService, TransferOrderService }
import io.paytouch.core.utils.{ MockedRestApi, FixtureDaoFactory => Factory }

class ReceivingOrderMonitorSpec extends MonitorSpec {

  abstract class ReceivingOrderMonitorSpecContext extends MonitorSpecContext with MonitorStateFixtures {
    val productService = mock[ProductService]
    val receivingOrderService = mock[ReceivingOrderService]
    val purchaseOrderService = mock[PurchaseOrderService]
    val receivingOrderProductService = MockedRestApi.receivingOrderProductService
    val transferOrderService = mock[TransferOrderService]
    lazy val monitor =
      monitorSystem.actorOf(
        Props(new ReceivingOrderMonitor(productService, receivingOrderService, receivingOrderProductService)),
      )

    val product = Factory.simpleProduct(merchant).create
  }

  "ReceivingOrderMonitor" should {

    "when creating a receiving order" should {

      "if receiving order refers to a purchase order" should {
        "update status of the linked purchase order and update average cost" in new ReceivingOrderMonitorSpecContext {
          val purchaseOrder = Factory.purchaseOrder(merchant, rome, user).create

          val receivingOrder = Factory.receivingOrderOfPurchaseOrder(rome, user, purchaseOrder).create
          val receivingOrderProduct = Factory.receivingOrderProduct(receivingOrder, product).create

          @scala.annotation.nowarn("msg=Auto-application")
          val entity = random[ReceivingOrder].copy(id = receivingOrder.id)

          monitor ! ReceivingOrderChange(None, entity, userContext)

          afterAWhile {
            there was one(receivingOrderService).updateReceivingObjectStatus(receivingOrder)(userContext)
            there was one(productService).updateAverageCost(Seq(product.id), rome.id)
          }
        }
      }

      "if receiving order refers to a transfer order" should {
        "update status of the linked transfer order and update average cost" in new ReceivingOrderMonitorSpecContext {
          val transferOrder = Factory.transferOrder(london, rome, user).create

          val receivingOrder = Factory.receivingOrderOfTransfer(rome, user, transferOrder).create
          val receivingOrderProduct = Factory.receivingOrderProduct(receivingOrder, product).create

          @scala.annotation.nowarn("msg=Auto-application")
          val entity = random[ReceivingOrder].copy(id = receivingOrder.id)

          monitor ! ReceivingOrderChange(None, entity, userContext)

          afterAWhile {
            there was one(receivingOrderService).updateReceivingObjectStatus(receivingOrder)(userContext)
            there was one(productService).updateAverageCost(Seq(product.id), rome.id)
          }
        }
      }

      "if receiving order does not refer to any object" should {
        "update average cost" in new ReceivingOrderMonitorSpecContext {
          val receivingOrder = Factory.receivingOrder(rome, user).create
          val receivingOrderProduct = Factory.receivingOrderProduct(receivingOrder, product).create

          @scala.annotation.nowarn("msg=Auto-application")
          val entity = random[ReceivingOrder].copy(id = receivingOrder.id)

          monitor ! ReceivingOrderChange(None, entity, userContext)

          afterAWhile {
            there was one(receivingOrderService).updateReceivingObjectStatus(receivingOrder)(userContext)
            there was one(productService).updateAverageCost(Seq(product.id), rome.id)
          }
        }
      }
    }

    "when updating a receiving order" should {

      "if receiving order refers to a purchase order" should {
        "update status of the linked purchase order and update average cost" in new ReceivingOrderMonitorSpecContext {
          val supplier = Factory.supplier(merchant).create
          val purchaseOrder = Factory.purchaseOrderWithSupplier(supplier, rome, user).create

          val receivingOrder = Factory.receivingOrderOfPurchaseOrder(rome, user, purchaseOrder).create
          val receivingOrderProduct = Factory.receivingOrderProduct(receivingOrder, product).create

          @scala.annotation.nowarn("msg=Auto-application")
          val entity = random[ReceivingOrder].copy(id = receivingOrder.id)

          val state = (receivingOrder, Seq.empty)

          monitor ! ReceivingOrderChange(Some(state), entity, userContext)

          afterAWhile {
            there was one(receivingOrderService).updateReceivingObjectStatus(receivingOrder)(userContext)
          }
        }
      }

      "if receiving order refers to a transfer order" should {
        "update status of the linked transfer order and update average cost" in new ReceivingOrderMonitorSpecContext {
          val transferOrder = Factory.transferOrder(london, rome, user).create

          val receivingOrder = Factory.receivingOrderOfTransfer(rome, user, transferOrder).create
          val receivingOrderProduct = Factory.receivingOrderProduct(receivingOrder, product).create

          @scala.annotation.nowarn("msg=Auto-application")
          val entity = random[ReceivingOrder].copy(id = receivingOrder.id)

          val state = (receivingOrder, Seq.empty)

          monitor ! ReceivingOrderChange(Some(state), entity, userContext)

          afterAWhile {
            there was one(receivingOrderService).updateReceivingObjectStatus(receivingOrder)(userContext)
            there was one(productService).updateAverageCost(Seq(product.id), rome.id)
          }
        }
      }

      "if receiving order does not refer to any object" should {
        "update average cost" in new ReceivingOrderMonitorSpecContext {
          val receivingOrder = Factory.receivingOrder(rome, user).create
          val receivingOrderProduct = Factory.receivingOrderProduct(receivingOrder, product).create
          val previouslyLinkedReceivingOrderProduct = random[ReceivingOrderProductRecord]

          @scala.annotation.nowarn("msg=Auto-application")
          val entity = random[ReceivingOrder].copy(id = receivingOrder.id)

          val state = (receivingOrder, Seq(previouslyLinkedReceivingOrderProduct))

          monitor ! ReceivingOrderChange(Some(state), entity, userContext)

          val expectedProductIds = Seq(
            product.id,
            previouslyLinkedReceivingOrderProduct.productId,
          )

          afterAWhile {
            there was one(receivingOrderService).updateReceivingObjectStatus(receivingOrder)(userContext)
            there was one(productService).updateAverageCost(expectedProductIds, rome.id)
          }

        }
      }
    }
  }
}
