package io.paytouch.core.services

import io.paytouch.core.data.model.enums.{
  PurchaseOrderPaymentStatus,
  ReceivingObjectStatus,
  ReceivingOrderPaymentStatus,
  ReceivingOrderStatus,
}
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class PurchaseOrderServiceSpec extends ServiceDaoSpec {

  abstract class PurchaseOrdersServiceSpecContext extends ServiceDaoSpecContext {
    val purchaseOrder = Factory.purchaseOrder(merchant, rome, user).create

    def assertStatus(receivingObjectStatus: ReceivingObjectStatus) =
      daos.purchaseOrderDao.findById(purchaseOrder.id).await.get.status ==== receivingObjectStatus

    def assertPaymentStatus(paymentStatus: PurchaseOrderPaymentStatus) =
      daos.purchaseOrderDao.findById(purchaseOrder.id).await.get.paymentStatus ==== Option(paymentStatus)
  }

  "PurchaseOrderService" in {
    "inferAndUpdateStatuses" should {
      "status" should {
        "if there are no receiving orders" should {
          "set status to Created" in new PurchaseOrdersServiceSpecContext {
            purchaseOrderService.inferAndUpdateStatuses(purchaseOrder.id).await
            assertStatus(ReceivingObjectStatus.Created)
          }
        }

        "if all receiving orders have status = Receiving" should {
          "set status to Receiving" in new PurchaseOrdersServiceSpecContext {
            val receivingOrder1 = Factory
              .receivingOrderOfPurchaseOrder(rome, user, purchaseOrder, status = Some(ReceivingOrderStatus.Receiving))
              .create
            val receivingOrder2 = Factory
              .receivingOrderOfPurchaseOrder(rome, user, purchaseOrder, status = Some(ReceivingOrderStatus.Receiving))
              .create

            purchaseOrderService.inferAndUpdateStatuses(purchaseOrder.id).await
            assertStatus(ReceivingObjectStatus.Receiving)
          }
        }

        "if some receiving orders have status = Received" in {
          "if some quantities are missing" should {
            "set status to Partial" in new PurchaseOrdersServiceSpecContext {
              val product1 = Factory.simpleProduct(merchant).create
              val quantityP1 = 30
              val purchaseOrderProduct1 =
                Factory.purchaseOrderProduct(purchaseOrder, product1, quantity = Some(quantityP1)).create

              val product2 = Factory.simpleProduct(merchant).create
              val quantityP2 = 80
              val purchaseOrderProduct2 =
                Factory.purchaseOrderProduct(purchaseOrder, product2, quantity = Some(quantityP2)).create

              val receivingOrder1 = Factory
                .receivingOrderOfPurchaseOrder(rome, user, purchaseOrder, status = Some(ReceivingOrderStatus.Received))
                .create
              val receivingOrder1Product1 =
                Factory.receivingOrderProduct(receivingOrder1, product1, quantity = Some(quantityP1 - 1)).create
              val receivingOrder1Product2 =
                Factory.receivingOrderProduct(receivingOrder1, product2, quantity = Some(quantityP2 - 2)).create

              // Quantities from this RO would complete the PO, but the status is not yet Received
              val receivingOrder2 = Factory
                .receivingOrderOfPurchaseOrder(rome, user, purchaseOrder, status = Some(ReceivingOrderStatus.Receiving))
                .create
              val receivingOrder2Product1 =
                Factory.receivingOrderProduct(receivingOrder2, product1, quantity = Some(1)).create
              val receivingOrder2Product2 =
                Factory.receivingOrderProduct(receivingOrder2, product2, quantity = Some(2)).create

              purchaseOrderService.inferAndUpdateStatuses(purchaseOrder.id).await
              assertStatus(ReceivingObjectStatus.Partial)
            }
          }

          "if no quantities are missing and all receiving orders have status Received" should {
            "set status to Completed" in new PurchaseOrdersServiceSpecContext {
              val product1 = Factory.simpleProduct(merchant).create
              val quantityP1 = 30
              val purchaseOrderProduct1 =
                Factory.purchaseOrderProduct(purchaseOrder, product1, quantity = Some(quantityP1)).create

              val product2 = Factory.simpleProduct(merchant).create
              val quantityP2 = 80
              val purchaseOrderProduct2 =
                Factory.purchaseOrderProduct(purchaseOrder, product2, quantity = Some(quantityP2)).create

              val receivingOrder1 = Factory
                .receivingOrderOfPurchaseOrder(rome, user, purchaseOrder, status = Some(ReceivingOrderStatus.Received))
                .create
              val receivingOrder1Product1 =
                Factory.receivingOrderProduct(receivingOrder1, product1, quantity = Some(quantityP1 - 1)).create
              val receivingOrder1Product2 =
                Factory.receivingOrderProduct(receivingOrder1, product2, quantity = Some(quantityP2 - 2)).create

              val receivingOrder2 = Factory
                .receivingOrderOfPurchaseOrder(rome, user, purchaseOrder, status = Some(ReceivingOrderStatus.Received))
                .create
              val receivingOrder2Product1 =
                Factory.receivingOrderProduct(receivingOrder2, product1, quantity = Some(1)).create
              val receivingOrder2Product2 =
                Factory.receivingOrderProduct(receivingOrder2, product2, quantity = Some(2)).create

              val receivingOrder3 = Factory
                .receivingOrderOfPurchaseOrder(rome, user, purchaseOrder, status = Some(ReceivingOrderStatus.Receiving))
                .create
              val receivingOrder3Product1 =
                Factory.receivingOrderProduct(receivingOrder3, product1, quantity = Some(100)).create
              val receivingOrder3Product2 =
                Factory.receivingOrderProduct(receivingOrder3, product2, quantity = Some(200)).create

              purchaseOrderService.inferAndUpdateStatuses(purchaseOrder.id).await
              assertStatus(ReceivingObjectStatus.Completed)
            }
          }
        }
      }
      "payment status" should {
        "if there are no receiving orders" should {
          "set payment status to Unpaid" in new PurchaseOrdersServiceSpecContext {
            purchaseOrderService.inferAndUpdateStatuses(purchaseOrder.id).await
            assertPaymentStatus(PurchaseOrderPaymentStatus.Unpaid)
          }
        }

        "if all receiving orders have payment status = Unpaid" should {
          "set payment status to Unpaid" in new PurchaseOrdersServiceSpecContext {
            val receivingOrder1 = Factory
              .receivingOrderOfPurchaseOrder(
                rome,
                user,
                purchaseOrder,
                paymentStatus = Some(ReceivingOrderPaymentStatus.Unpaid),
              )
              .create
            val receivingOrder2 = Factory
              .receivingOrderOfPurchaseOrder(
                rome,
                user,
                purchaseOrder,
                paymentStatus = Some(ReceivingOrderPaymentStatus.Unpaid),
              )
              .create

            purchaseOrderService.inferAndUpdateStatuses(purchaseOrder.id).await
            assertPaymentStatus(PurchaseOrderPaymentStatus.Unpaid)
          }
        }
        "if all receiving orders have payment status = Paid" should {
          "set payment status to Paid" in new PurchaseOrdersServiceSpecContext {
            val receivingOrder1 = Factory
              .receivingOrderOfPurchaseOrder(
                rome,
                user,
                purchaseOrder,
                paymentStatus = Some(ReceivingOrderPaymentStatus.Paid),
              )
              .create
            val receivingOrder2 = Factory
              .receivingOrderOfPurchaseOrder(
                rome,
                user,
                purchaseOrder,
                paymentStatus = Some(ReceivingOrderPaymentStatus.Paid),
              )
              .create

            purchaseOrderService.inferAndUpdateStatuses(purchaseOrder.id).await
            assertPaymentStatus(PurchaseOrderPaymentStatus.Paid)
          }
        }

        "if some receiving orders have payment status = Paid and some not" in {
          "set payment status to Partial" in new PurchaseOrdersServiceSpecContext {
            val receivingOrder1 = Factory
              .receivingOrderOfPurchaseOrder(
                rome,
                user,
                purchaseOrder,
                paymentStatus = Some(ReceivingOrderPaymentStatus.Unpaid),
              )
              .create
            val receivingOrder2 = Factory
              .receivingOrderOfPurchaseOrder(
                rome,
                user,
                purchaseOrder,
                paymentStatus = Some(ReceivingOrderPaymentStatus.Paid),
              )
              .create

            purchaseOrderService.inferAndUpdateStatuses(purchaseOrder.id).await
            assertPaymentStatus(PurchaseOrderPaymentStatus.Partial)
          }
        }
      }
    }
  }
}
