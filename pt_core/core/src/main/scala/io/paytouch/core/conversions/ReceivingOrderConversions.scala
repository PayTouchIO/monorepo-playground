package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model.{ ReceivingOrderUpdate => ReceivingOrderUpdateModel, _ }
import io.paytouch.core.entities.{
  ReceivingOrder => ReceivingOrderEntity,
  ReceivingOrderUpdate => ReceivingOrderUpdateEntity,
  _,
}

trait ReceivingOrderConversions
    extends EntityConversion[ReceivingOrderRecord, ReceivingOrderEntity]
       with ModelConversion[ReceivingOrderUpdateEntity, ReceivingOrderUpdateModel] {

  def fromRecordsAndOptionsToEntities(
      receivingOrders: Seq[ReceivingOrderRecord],
      locationPerReceivingOrders: Option[Map[ReceivingOrderRecord, Location]],
      userPerReceivingOrders: Option[Map[ReceivingOrderRecord, UserInfo]],
      purchaseOrderPerReceivingOrders: Option[Map[ReceivingOrderRecord, PurchaseOrder]],
      transferOrderPerReceivingOrders: Option[Map[ReceivingOrderRecord, TransferOrder]],
      productsCountPerReceivingOrder: Option[Map[ReceivingOrderRecord, BigDecimal]],
      stockValuePerReceivingOrder: Option[Map[ReceivingOrderRecord, MonetaryAmount]],
    ) =
    receivingOrders.map { receivingOrder =>
      val location = locationPerReceivingOrders.flatMap(_.get(receivingOrder))
      val user = userPerReceivingOrders.flatMap(_.get(receivingOrder))
      val purchaseOrder = purchaseOrderPerReceivingOrders.flatMap(_.get(receivingOrder))
      val transferOrder = transferOrderPerReceivingOrders.flatMap(_.get(receivingOrder))
      val productsCount = productsCountPerReceivingOrder.map(_.getOrElse[BigDecimal](receivingOrder, 0))
      val stockValue = stockValuePerReceivingOrder.flatMap(_.get(receivingOrder))
      fromRecordAndOptionsToEntity(
        receivingOrder,
        location,
        user,
        purchaseOrder,
        transferOrder,
        productsCount,
        stockValue,
      )
    }

  def fromRecordToEntity(receivingOrder: ReceivingOrderRecord)(implicit user: UserContext): ReceivingOrderEntity =
    fromRecordAndOptionsToEntity(receivingOrder, None, None, None, None, None, None)

  def fromRecordAndOptionsToEntity(
      receivingOrder: ReceivingOrderRecord,
      location: Option[Location],
      user: Option[UserInfo],
      purchaseOrder: Option[PurchaseOrder],
      transferOrder: Option[TransferOrder],
      productsCount: Option[BigDecimal],
      stockValue: Option[MonetaryAmount],
    ) =
    ReceivingOrderEntity(
      id = receivingOrder.id,
      locationId = receivingOrder.locationId,
      location = location,
      user = user,
      receivingObjectType = receivingOrder.receivingObjectType,
      purchaseOrder = purchaseOrder,
      transferOrder = transferOrder,
      status = receivingOrder.status,
      number = receivingOrder.number,
      productsCount = productsCount,
      stockValue = stockValue,
      synced = receivingOrder.synced,
      invoiceNumber = receivingOrder.invoiceNumber,
      paymentMethod = receivingOrder.paymentMethod,
      paymentStatus = receivingOrder.paymentStatus,
      paymentDueDate = receivingOrder.paymentDueDate,
      createdAt = receivingOrder.createdAt,
      updatedAt = receivingOrder.updatedAt,
    )

  def fromUpsertionToUpdate(
      id: UUID,
      upsertion: ReceivingOrderUpdateEntity,
    )(implicit
      user: UserContext,
    ): ReceivingOrderUpdateModel =
    ReceivingOrderUpdateModel(
      id = Some(id),
      merchantId = Some(user.merchantId),
      locationId = upsertion.locationId,
      userId = Some(user.id),
      receivingObjectType = upsertion.receivingObjectType,
      receivingObjectId = upsertion.receivingObjectId,
      status = upsertion.status,
      synced = None,
      invoiceNumber = upsertion.invoiceNumber,
      paymentMethod = upsertion.paymentMethod,
      paymentStatus = upsertion.paymentStatus,
      paymentDueDate = upsertion.paymentDueDate,
    )
}
