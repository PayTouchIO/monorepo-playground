package io.paytouch.core.resources.purchaseorders

import java.time.ZoneId
import java.util.UUID

import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums.ReceivingObjectStatus
import io.paytouch.core.entities.{
  PurchaseOrderCreation,
  PurchaseOrderProductUpsertion,
  PurchaseOrderUpdate,
  PurchaseOrder => PurchaseOrderEntity,
}
import io.paytouch.core.utils._

abstract class PurchaseOrdersFSpec extends FSpec {

  abstract class PurchaseOrderResourceFSpecContext extends FSpecContext with MultipleLocationFixtures {
    val purchaseOrderDao = daos.purchaseOrderDao
    val purchaseOrderProductDao = daos.purchaseOrderProductDao

    def assertResponse(
        entity: PurchaseOrderEntity,
        record: PurchaseOrderRecord,
        supplier: Option[SupplierRecord] = None,
        location: Option[LocationRecord] = None,
        user: Option[UserRecord] = None,
        receivingOrders: Option[Seq[ReceivingOrderRecord]] = None,
        orderedProductsCount: Option[BigDecimal] = None,
        receivedProductsCount: Option[BigDecimal] = None,
        returnedProductsCount: Option[BigDecimal] = None,
      ) = {
      entity.id ==== record.id
      entity.number ==== record.number
      entity.paymentStatus ==== record.paymentStatus
      entity.expectedDeliveryDate ==== record.expectedDeliveryDate
      entity.status ==== record.status
      entity.sent ==== record.sent
      entity.notes ==== record.notes

      entity.supplier.map(_.id) ==== supplier.map(_.id)
      entity.location.map(_.id) ==== location.map(_.id)
      entity.user.map(_.id) ==== user.map(_.id)
      entity.receivingOrders.map(_.map(_.id).toSet) ==== receivingOrders.map(_.map(_.id).toSet)
      entity.orderedProductsCount ==== orderedProductsCount
      entity.receivedProductsCount ==== receivedProductsCount
      entity.returnedProductsCount ==== returnedProductsCount
    }

    def assertCreation(id: UUID, creation: PurchaseOrderCreation) = {
      assertUpdate(id, creation.asUpdate)
      val record = purchaseOrderDao.findById(id).await.get
      record.status ==== ReceivingObjectStatus.Created
    }

    def assertUpdate(id: UUID, update: PurchaseOrderUpdate) = {
      val record = purchaseOrderDao.findById(id).await.get

      record.id ==== id
      if (update.supplierId.isDefined) update.supplierId ==== Some(record.supplierId)
      if (update.locationId.isDefined) update.locationId ==== Some(record.locationId)
      if (update.expectedDeliveryDate.isDefined)
        update.expectedDeliveryDate ==== record.expectedDeliveryDate.map(_.withZoneSameLocal(ZoneId.of("UTC")))
      if (update.notes.isDefined) update.notes ==== record.notes

      update.products.map { purchaseOrderProducts =>
        val products = purchaseOrderProductDao.findByPurchaseOrderId(id).await
        products.size ==== purchaseOrderProducts.size

        purchaseOrderProducts.map(purchaseOrderProduct => assertPurchaseOrderProduct(id, purchaseOrderProduct))
      }
    }

    def assertPurchaseOrderProduct(purchaseOrderId: UUID, update: PurchaseOrderProductUpsertion) = {
      val record =
        purchaseOrderProductDao.findOneByPurchaseOrderIdAndProductId(purchaseOrderId, update.productId).await.get

      update.productId ==== record.productId
      purchaseOrderId ==== record.purchaseOrderId
      update.quantity ==== record.quantity
      update.cost ==== record.costAmount
    }
  }
}
