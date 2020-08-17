package io.paytouch.core.resources.returnorders

import java.util.UUID

import io.paytouch.core.data.model._
import io.paytouch.core.entities.{
  MonetaryAmount,
  ReturnOrderCreation,
  ReturnOrderProductUpsertion,
  ReturnOrderUpdate,
  ReturnOrder => ReturnOrderEntity,
}
import io.paytouch.core.utils._

abstract class ReturnOrdersFSpec extends FSpec {

  abstract class ReturnOrderResourceFSpecContext extends FSpecContext with MultipleLocationFixtures {
    val returnOrderDao = daos.returnOrderDao
    val returnOrderProductDao = daos.returnOrderProductDao

    def assertResponse(
        entity: ReturnOrderEntity,
        record: ReturnOrderRecord,
        location: Option[LocationRecord] = None,
        purchaseOrder: Option[PurchaseOrderRecord] = None,
        productsCount: Option[BigDecimal] = None,
        user: Option[UserRecord] = None,
        supplier: Option[SupplierRecord] = None,
        stockValue: Option[MonetaryAmount] = None,
      ) = {
      entity.id ==== record.id
      entity.user.map(_.id) ==== user.map(_.id)
      entity.supplier.map(_.id) ==== supplier.map(_.id)
      entity.location.map(_.id) ==== location.map(_.id)
      entity.purchaseOrder.map(_.id) ==== purchaseOrder.map(_.id)
      entity.productsCount ==== productsCount
      entity.number ==== record.number
      entity.notes ==== record.notes
      entity.status ==== record.status

      entity.stockValue ==== stockValue
    }

    def assertCreation(id: UUID, creation: ReturnOrderCreation) = assertUpdate(id, creation.asUpdate)

    def assertUpdate(id: UUID, update: ReturnOrderUpdate) = {
      val record = returnOrderDao.findById(id).await.get

      record.id ==== id
      if (update.userId.isDefined) update.userId ==== Some(record.userId)
      if (update.supplierId.isDefined) update.supplierId ==== Some(record.supplierId)
      if (update.locationId.isDefined) update.locationId ==== Some(record.locationId)
      if (update.purchaseOrderId.isDefined) update.purchaseOrderId ==== record.purchaseOrderId
      if (update.notes.isDefined) update.notes ==== record.notes
      if (update.status.isDefined) update.status ==== Some(record.status)

      update.products.map { returnOrderProducts =>
        val products = returnOrderProductDao.findByReturnOrderIds(Seq(id)).await
        products.size ==== returnOrderProducts.size

        returnOrderProducts.map(assertReturnOrderProduct(id, _))
      }
    }

    def assertReturnOrderProduct(returnOrderId: UUID, update: ReturnOrderProductUpsertion) = {
      val record = returnOrderProductDao.findByProductIdAndReturnOrderId(update.productId, returnOrderId).await.get

      update.productId ==== record.productId
      returnOrderId ==== record.returnOrderId
      update.quantity ==== record.quantity
      update.reason ==== record.reason
    }

  }
}
