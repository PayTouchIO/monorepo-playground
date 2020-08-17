package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model.{ PurchaseOrderRecord, PurchaseOrderUpdate => PurchaseOrderUpdateModel }
import io.paytouch.core.entities.{
  Location,
  ReceivingOrder,
  Supplier,
  UserContext,
  UserInfo,
  PurchaseOrder => PurchaseOrderEntity,
  PurchaseOrderUpdate => PurchaseOrderUpdateEntity,
}

trait PurchaseOrderConversions
    extends EntityConversion[PurchaseOrderRecord, PurchaseOrderEntity]
       with ModelConversion[PurchaseOrderUpdateEntity, PurchaseOrderUpdateModel] {

  def fromRecordToEntity(record: PurchaseOrderRecord)(implicit user: UserContext): PurchaseOrderEntity =
    fromRecordAndOptionsToEntity(record, None, None, None, None, None, None, None)

  def fromRecordsAndOptionsToEntities(
      records: Seq[PurchaseOrderRecord],
      supplierPerRecord: Option[Map[PurchaseOrderRecord, Supplier]],
      locationPerRecord: Option[Map[PurchaseOrderRecord, Location]],
      userInfoPerRecord: Option[Map[PurchaseOrderRecord, UserInfo]],
      receivingOrdersPerRecord: Option[Map[PurchaseOrderRecord, Seq[ReceivingOrder]]],
      orderedProductsCountPerRecord: Option[Map[PurchaseOrderRecord, BigDecimal]],
      receivedProductsCountPerRecord: Option[Map[PurchaseOrderRecord, BigDecimal]],
      returnedProductsCountPerRecord: Option[Map[PurchaseOrderRecord, BigDecimal]],
    ) =
    records.map { record =>
      val supplier = supplierPerRecord.flatMap(_.get(record))
      val location = locationPerRecord.flatMap(_.get(record))
      val userInfo = userInfoPerRecord.flatMap(_.get(record))
      val receivingOrders = receivingOrdersPerRecord.map(_.getOrElse(record, Seq.empty))
      val orderProductsCount = orderedProductsCountPerRecord.map(_.getOrElse[BigDecimal](record, 0))
      val receivedProductsCount = receivedProductsCountPerRecord.map(_.getOrElse[BigDecimal](record, 0))
      val returnedProductsCount = returnedProductsCountPerRecord.map(_.getOrElse[BigDecimal](record, 0))
      fromRecordAndOptionsToEntity(
        record,
        supplier,
        location,
        userInfo,
        receivingOrders,
        orderProductsCount,
        receivedProductsCount,
        returnedProductsCount,
      )
    }

  def fromRecordAndOptionsToEntity(
      record: PurchaseOrderRecord,
      supplier: Option[Supplier],
      location: Option[Location],
      userInfo: Option[UserInfo],
      receivingOrders: Option[Seq[ReceivingOrder]],
      orderedProductsCount: Option[BigDecimal],
      receivedProductsCount: Option[BigDecimal],
      returnedProductsCount: Option[BigDecimal],
    ): PurchaseOrderEntity =
    PurchaseOrderEntity(
      id = record.id,
      supplier = supplier,
      location = location,
      user = userInfo,
      receivingOrders = receivingOrders,
      number = record.number,
      paymentStatus = record.paymentStatus,
      expectedDeliveryDate = record.expectedDeliveryDate,
      status = record.status,
      sent = record.sent,
      orderedProductsCount = orderedProductsCount,
      receivedProductsCount = receivedProductsCount,
      returnedProductsCount = returnedProductsCount,
      notes = record.notes,
      createdAt = record.createdAt,
    )

  def fromUpsertionToUpdate(id: UUID, update: PurchaseOrderUpdateEntity)(implicit user: UserContext) =
    PurchaseOrderUpdateModel(
      id = Some(id),
      merchantId = Some(user.merchantId),
      supplierId = update.supplierId,
      locationId = update.locationId,
      userId = Some(user.id),
      paymentStatus = None,
      expectedDeliveryDate = update.expectedDeliveryDate,
      sent = None,
      notes = update.notes,
      deletedAt = None,
    )
}
