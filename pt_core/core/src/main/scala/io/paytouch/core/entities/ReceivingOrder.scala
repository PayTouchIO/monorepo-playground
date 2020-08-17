package io.paytouch.core.entities

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums._
import io.paytouch.core.entities.enums.ExposedName

final case class ReceivingOrder(
    id: UUID,
    locationId: UUID,
    location: Option[Location],
    user: Option[UserInfo],
    receivingObjectType: Option[ReceivingOrderObjectType],
    purchaseOrder: Option[PurchaseOrder],
    transferOrder: Option[TransferOrder],
    status: ReceivingOrderStatus,
    number: String,
    synced: Boolean,
    productsCount: Option[BigDecimal],
    stockValue: Option[MonetaryAmount],
    invoiceNumber: Option[String],
    paymentMethod: Option[ReceivingOrderPaymentMethod],
    paymentStatus: Option[ReceivingOrderPaymentStatus],
    paymentDueDate: Option[ZonedDateTime],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends ExposedEntity {
  val classShortName = ExposedName.ReceivingOrder
}

final case class ReceivingOrderCreation(
    locationId: UUID,
    receivingObjectType: Option[ReceivingOrderObjectType],
    receivingObjectId: Option[UUID],
    status: ReceivingOrderStatus,
    invoiceNumber: Option[String],
    paymentMethod: Option[ReceivingOrderPaymentMethod],
    paymentStatus: Option[ReceivingOrderPaymentStatus],
    paymentDueDate: Option[ZonedDateTime],
    products: Seq[ReceivingOrderProductUpsertion],
  ) extends CreationEntity[ReceivingOrder, ReceivingOrderUpdate] {
  def asUpdate =
    ReceivingOrderUpdate(
      locationId = Some(locationId),
      receivingObjectType = receivingObjectType,
      receivingObjectId = receivingObjectId,
      status = Some(status),
      invoiceNumber = invoiceNumber,
      paymentMethod = paymentMethod,
      paymentStatus = paymentStatus,
      paymentDueDate = paymentDueDate,
      products = Some(products),
    )
}

final case class ReceivingOrderUpdate(
    locationId: Option[UUID],
    receivingObjectType: Option[ReceivingOrderObjectType],
    receivingObjectId: Option[UUID],
    status: Option[ReceivingOrderStatus],
    invoiceNumber: Option[String],
    paymentMethod: Option[ReceivingOrderPaymentMethod],
    paymentStatus: Option[ReceivingOrderPaymentStatus],
    paymentDueDate: Option[ZonedDateTime],
    products: Option[Seq[ReceivingOrderProductUpsertion]],
  ) extends UpdateEntity[ReceivingOrder]

object ReceivingOrderUpdate {
  def extractAfterSyncAllowedFields(upsertion: ReceivingOrderUpdate): ReceivingOrderUpdate =
    ReceivingOrderUpdate(
      locationId = None,
      receivingObjectType = None,
      receivingObjectId = None,
      status = None,
      invoiceNumber = None,
      paymentMethod = upsertion.paymentMethod,
      paymentStatus = upsertion.paymentStatus,
      paymentDueDate = upsertion.paymentDueDate,
      products = None,
    )
}
