package io.paytouch.core.resources.transferorders

import java.util.UUID

import io.paytouch.core.data.model.enums.ReceivingObjectStatus
import io.paytouch.core.data.model.{ LocationRecord, TransferOrderRecord, UserRecord }
import io.paytouch.core.entities.{
  MonetaryAmount,
  TransferOrderCreation,
  TransferOrderProductUpsertion,
  TransferOrderUpdate,
  TransferOrder => TransferOrderEntity,
}
import io.paytouch.core.utils._

abstract class TransferOrdersFSpec extends FSpec {

  abstract class TransferOrderResourceFSpecContext extends FSpecContext with MultipleLocationFixtures {
    val transferOrderDao = daos.transferOrderDao
    val transferOrderProductDao = daos.transferOrderProductDao

    def assertResponse(
        entity: TransferOrderEntity,
        record: TransferOrderRecord,
        fromLocation: Option[LocationRecord] = None,
        toLocation: Option[LocationRecord] = None,
        user: Option[UserRecord] = None,
        productsCount: Option[BigDecimal] = None,
        stockValue: Option[MonetaryAmount] = None,
      ) = {
      entity.id ==== record.id
      entity.fromLocation.map(_.id) ==== fromLocation.map(_.id)
      entity.toLocation.map(_.id) ==== toLocation.map(_.id)
      entity.user.map(_.id) ==== user.map(_.id)
      entity.number ==== record.number
      entity.notes ==== record.notes
      entity.status ==== record.status
      entity.`type` ==== record.`type`
      entity.productsCount ==== productsCount
      entity.stockValue ==== stockValue
      entity.createdAt ==== record.createdAt
      entity.updatedAt ==== record.updatedAt
    }

    def assertCreation(id: UUID, creation: TransferOrderCreation) = {
      assertUpdate(id, creation.asUpdate)
      val record = transferOrderDao.findById(id).await.get
      record.status ==== ReceivingObjectStatus.Created
    }

    def assertUpdate(id: UUID, update: TransferOrderUpdate) = {
      val record = transferOrderDao.findById(id).await.get

      record.id ==== id
      if (update.fromLocationId.isDefined) update.fromLocationId ==== Some(record.fromLocationId)
      if (update.toLocationId.isDefined) update.toLocationId ==== Some(record.toLocationId)
      if (update.userId.isDefined) update.userId ==== Some(record.userId)
      if (update.notes.isDefined) update.notes ==== record.notes
      if (update.`type`.isDefined) update.`type` ==== Some(record.`type`)

      update.products.map { transferOrderProducts =>
        val products = transferOrderProductDao.findByTransferOrderId(id).await
        products.size ==== transferOrderProducts.size

        transferOrderProducts.map(assertTransferOrderProduct(id, _))
      }
    }

    def assertTransferOrderProduct(transferOrderId: UUID, update: TransferOrderProductUpsertion) = {
      val record =
        transferOrderProductDao.findOneByTransferOrderIdAndProductId(transferOrderId, update.productId).await.get

      update.productId ==== record.productId
      transferOrderId ==== record.transferOrderId
      update.quantity ==== record.quantity
    }
  }
}
