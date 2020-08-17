package io.paytouch.core.resources.receivingorders

import java.util.UUID

import io.paytouch.core.data.model._
import io.paytouch.core.entities.{
  MonetaryAmount,
  ReceivingOrderCreation,
  ReceivingOrderProductUpsertion,
  ReceivingOrderUpdate,
  ReceivingOrder => ReceivingOrderEntity,
}
import io.paytouch.core.utils._

abstract class ReceivingOrdersFSpec extends FSpec {

  abstract class ReceivingOrderResourceFSpecContext extends FSpecContext with MultipleLocationFixtures {
    val receivingOrderDao = daos.receivingOrderDao
    val receivingOrderProductDao = daos.receivingOrderProductDao

    def assertResponse(
        entity: ReceivingOrderEntity,
        record: ReceivingOrderRecord,
        location: Option[LocationRecord] = None,
        user: Option[UserRecord] = None,
        purchaseOrder: Option[PurchaseOrderRecord] = None,
        transferOrder: Option[TransferOrderRecord] = None,
        productsCount: Option[BigDecimal] = None,
        stockValue: Option[MonetaryAmount] = None,
      ) = {
      entity.id ==== record.id
      entity.status ==== record.status
      entity.number ==== record.number
      entity.receivingObjectType ==== record.receivingObjectType
      entity.synced ==== record.synced
      entity.invoiceNumber ==== record.invoiceNumber
      entity.paymentMethod ==== record.paymentMethod
      entity.paymentStatus ==== record.paymentStatus
      entity.paymentDueDate ==== record.paymentDueDate
      entity.createdAt ==== record.createdAt

      entity.location.map(_.id) ==== location.map(_.id)
      entity.user.map(_.id) ==== user.map(_.id)
      entity.purchaseOrder.map(_.id) ==== purchaseOrder.map(_.id)
      entity.transferOrder.map(_.id) ==== transferOrder.map(_.id)
      entity.productsCount ==== productsCount
      entity.stockValue ==== stockValue
    }

    def assertCreation(id: UUID, creation: ReceivingOrderCreation) = assertUpdate(id, creation.asUpdate)

    def assertUpdate(id: UUID, update: ReceivingOrderUpdate) = {
      val record = receivingOrderDao.findById(id).await.get

      record.id ==== id
      if (update.locationId.isDefined) update.locationId ==== Some(record.locationId)
      if (update.receivingObjectType.isDefined) update.receivingObjectType ==== record.receivingObjectType
      if (update.receivingObjectId.isDefined) update.receivingObjectId ==== record.receivingObjectId
      if (update.status.isDefined) update.status ==== Some(record.status)
      if (update.invoiceNumber.isDefined) update.invoiceNumber ==== record.invoiceNumber
      if (update.paymentMethod.isDefined) update.paymentMethod ==== record.paymentMethod
      if (update.paymentStatus.isDefined) update.paymentStatus ==== record.paymentStatus
      if (update.paymentDueDate.isDefined) update.paymentDueDate.map(_.withFixedOffsetZone) ==== record.paymentDueDate

      update.products.map { receivingOrderProducts =>
        val products = receivingOrderProductDao.findByReceivingOrderId(id).await
        products.size ==== receivingOrderProducts.size

        receivingOrderProducts.map(receivingOrderProduct => assertReceivingOrderProduct(id, receivingOrderProduct))
      }
    }

    def assertReceivingOrderProduct(receivingOrderId: UUID, update: ReceivingOrderProductUpsertion) = {
      val record =
        receivingOrderProductDao.findOneByReceivingOrderIdAndProductId(receivingOrderId, update.productId).await.get

      update.productId ==== record.productId
      receivingOrderId ==== record.receivingOrderId
      update.quantity ==== record.quantity
      update.cost ==== record.costAmount
    }
  }
}
