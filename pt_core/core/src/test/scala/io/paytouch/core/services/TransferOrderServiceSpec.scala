package io.paytouch.core.services

import io.paytouch.core.data.model.enums.{ ReceivingObjectStatus, ReceivingOrderStatus }
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class TransferOrderServiceSpec extends ServiceDaoSpec {

  abstract class TransferOrdersServiceSpecContext extends ServiceDaoSpecContext {
    val transferOrder = Factory.transferOrder(london, rome, user).create

    def assertStatus(receivingObjectStatus: ReceivingObjectStatus) =
      daos.transferOrderDao.findById(transferOrder.id).await.get.status ==== receivingObjectStatus
  }

  "TransferOrderService" in {
    "inferAndUpdateStatus" should {
      "if there are no receiving orders" should {
        "set status to Created" in new TransferOrdersServiceSpecContext {
          transferOrderService.inferAndUpdateStatuses(transferOrder.id).await
          assertStatus(ReceivingObjectStatus.Created)
        }
      }

      "if all receiving orders have status = Receiving" should {
        "set status to Receiving" in new TransferOrdersServiceSpecContext {
          val receivingOrder1 = Factory
            .receivingOrderOfTransfer(rome, user, transferOrder, status = Some(ReceivingOrderStatus.Receiving))
            .create
          val receivingOrder2 = Factory
            .receivingOrderOfTransfer(rome, user, transferOrder, status = Some(ReceivingOrderStatus.Receiving))
            .create

          transferOrderService.inferAndUpdateStatuses(transferOrder.id).await
          assertStatus(ReceivingObjectStatus.Receiving)
        }
      }

      "if some receiving orders have status = Received" in {

        "if some quantities are missing" should {
          "set status to Partial" in new TransferOrdersServiceSpecContext {
            val product1 = Factory.simpleProduct(merchant).create
            val quantityP1 = 30
            val transferOrderProduct1 =
              Factory.transferOrderProduct(transferOrder, product1, quantity = Some(quantityP1)).create

            val product2 = Factory.simpleProduct(merchant).create
            val quantityP2 = 80
            val transferOrderProduct2 =
              Factory.transferOrderProduct(transferOrder, product2, quantity = Some(quantityP2)).create

            val receivingOrder1 = Factory
              .receivingOrderOfTransfer(rome, user, transferOrder, status = Some(ReceivingOrderStatus.Received))
              .create
            val receivingOrder1Product1 =
              Factory.receivingOrderProduct(receivingOrder1, product1, quantity = Some(quantityP1 - 1)).create
            val receivingOrder1Product2 =
              Factory.receivingOrderProduct(receivingOrder1, product2, quantity = Some(quantityP2 - 2)).create

            // Quantities from this RO would complete the PO, but the status is not yet Received
            val receivingOrder2 = Factory
              .receivingOrderOfTransfer(rome, user, transferOrder, status = Some(ReceivingOrderStatus.Receiving))
              .create
            val receivingOrder2Product1 =
              Factory.receivingOrderProduct(receivingOrder2, product1, quantity = Some(1)).create
            val receivingOrder2Product2 =
              Factory.receivingOrderProduct(receivingOrder2, product2, quantity = Some(2)).create

            transferOrderService.inferAndUpdateStatuses(transferOrder.id).await
            assertStatus(ReceivingObjectStatus.Partial)
          }
        }

        "if no quantities are missing and all receiving orders have status Received" should {
          "set status to Completed" in new TransferOrdersServiceSpecContext {
            val product1 = Factory.simpleProduct(merchant).create
            val quantityP1 = 30
            val transferOrderProduct1 =
              Factory.transferOrderProduct(transferOrder, product1, quantity = Some(quantityP1)).create

            val product2 = Factory.simpleProduct(merchant).create
            val quantityP2 = 80
            val transferOrderProduct2 =
              Factory.transferOrderProduct(transferOrder, product2, quantity = Some(quantityP2)).create

            val receivingOrder1 = Factory
              .receivingOrderOfTransfer(rome, user, transferOrder, status = Some(ReceivingOrderStatus.Received))
              .create
            val receivingOrder1Product1 =
              Factory.receivingOrderProduct(receivingOrder1, product1, quantity = Some(quantityP1 - 1)).create
            val receivingOrder1Product2 =
              Factory.receivingOrderProduct(receivingOrder1, product2, quantity = Some(quantityP2 - 2)).create

            val receivingOrder2 = Factory
              .receivingOrderOfTransfer(rome, user, transferOrder, status = Some(ReceivingOrderStatus.Received))
              .create
            val receivingOrder2Product1 =
              Factory.receivingOrderProduct(receivingOrder2, product1, quantity = Some(1)).create
            val receivingOrder2Product2 =
              Factory.receivingOrderProduct(receivingOrder2, product2, quantity = Some(2)).create

            val receivingOrder3 = Factory
              .receivingOrderOfTransfer(rome, user, transferOrder, status = Some(ReceivingOrderStatus.Receiving))
              .create
            val receivingOrder3Product1 =
              Factory.receivingOrderProduct(receivingOrder3, product1, quantity = Some(100)).create
            val receivingOrder3Product2 =
              Factory.receivingOrderProduct(receivingOrder3, product2, quantity = Some(200)).create

            transferOrderService.inferAndUpdateStatuses(transferOrder.id).await
            assertStatus(ReceivingObjectStatus.Completed)
          }
        }
      }
    }
  }
}
