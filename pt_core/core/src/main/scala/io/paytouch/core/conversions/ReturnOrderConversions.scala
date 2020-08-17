package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model.{ ReturnOrderRecord, ReturnOrderUpdate => ReturnOrderUpdateModel }
import io.paytouch.core.entities.{
  Location,
  PurchaseOrder,
  MonetaryAmount,
  Supplier,
  UserContext,
  UserInfo,
  ReturnOrder => ReturnOrderEntity,
  ReturnOrderUpdate => ReturnOrderUpdateEntity,
}

trait ReturnOrderConversions extends EntityConversion[ReturnOrderRecord, ReturnOrderEntity] {

  def fromRecordToEntity(returnOrder: ReturnOrderRecord)(implicit user: UserContext): ReturnOrderEntity =
    fromRecordAndOptionsToEntity(returnOrder, None, None, None, None, None, None)

  def fromRecordsAndOptionsToEntities(
      records: Seq[ReturnOrderRecord],
      locationPerRecord: Option[Map[ReturnOrderRecord, Location]],
      purchaseOrderPerRecord: Option[Map[ReturnOrderRecord, PurchaseOrder]],
      productCountPerRecord: Option[Map[ReturnOrderRecord, BigDecimal]],
      userPerRecord: Option[Map[ReturnOrderRecord, UserInfo]],
      supplierPerRecord: Option[Map[ReturnOrderRecord, Supplier]],
      stockValuePerRecord: Option[Map[ReturnOrderRecord, MonetaryAmount]],
    ): Seq[ReturnOrderEntity] =
    records.map { record =>
      val location = locationPerRecord.flatMap(_.get(record))
      val purchaseOrder = purchaseOrderPerRecord.flatMap(_.get(record))
      val productCount = productCountPerRecord.flatMap(_.get(record))
      val user = userPerRecord.flatMap(_.get(record))
      val supplier = supplierPerRecord.flatMap(_.get(record))
      val stockValue = stockValuePerRecord.flatMap(_.get(record))
      fromRecordAndOptionsToEntity(record, location, purchaseOrder, productCount, user, supplier, stockValue)
    }

  def fromRecordAndOptionsToEntity(
      record: ReturnOrderRecord,
      location: Option[Location],
      purchaseOrder: Option[PurchaseOrder],
      productCount: Option[BigDecimal],
      user: Option[UserInfo],
      supplier: Option[Supplier],
      stockValue: Option[MonetaryAmount],
    ) =
    ReturnOrderEntity(
      id = record.id,
      user = user,
      supplier = supplier,
      locationId = record.locationId,
      location = location,
      purchaseOrder = purchaseOrder,
      number = record.number,
      notes = record.notes,
      productsCount = productCount,
      status = record.status,
      stockValue = stockValue,
      synced = record.synced,
      createdAt = record.createdAt,
      updatedAt = record.updatedAt,
    )

  def fromUpsertionToUpdate(
      id: UUID,
      update: ReturnOrderUpdateEntity,
    )(implicit
      user: UserContext,
    ): ReturnOrderUpdateModel =
    ReturnOrderUpdateModel(
      id = Some(id),
      merchantId = Some(user.merchantId),
      userId = update.userId,
      supplierId = update.supplierId,
      locationId = update.locationId,
      purchaseOrderId = update.purchaseOrderId,
      notes = update.notes,
      status = update.status,
      synced = None,
    )
}
