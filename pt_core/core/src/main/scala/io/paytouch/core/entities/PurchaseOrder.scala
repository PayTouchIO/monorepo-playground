package io.paytouch.core.entities

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.{ PurchaseOrderPaymentStatus, ReceivingObjectStatus }
import io.paytouch.core.entities.enums.ExposedName

final case class PurchaseOrder(
    id: UUID,
    supplier: Option[Supplier],
    location: Option[Location],
    receivingOrders: Option[Seq[ReceivingOrder]],
    user: Option[UserInfo],
    number: String,
    paymentStatus: Option[PurchaseOrderPaymentStatus],
    expectedDeliveryDate: Option[ZonedDateTime],
    status: ReceivingObjectStatus,
    sent: Boolean,
    orderedProductsCount: Option[BigDecimal],
    receivedProductsCount: Option[BigDecimal],
    returnedProductsCount: Option[BigDecimal],
    notes: Option[String],
    createdAt: ZonedDateTime,
  ) extends ExposedEntity {
  val classShortName = ExposedName.PurchaseOrder
}

final case class PurchaseOrderCreation(
    supplierId: UUID,
    locationId: UUID,
    expectedDeliveryDate: Option[ZonedDateTime],
    notes: Option[String],
    products: Seq[PurchaseOrderProductUpsertion],
  ) extends CreationEntity[PurchaseOrder, PurchaseOrderUpdate] {
  def asUpdate: PurchaseOrderUpdate =
    PurchaseOrderUpdate(
      supplierId = Some(supplierId),
      locationId = Some(locationId),
      expectedDeliveryDate = expectedDeliveryDate,
      notes = notes,
      products = Some(products),
    )
}

final case class PurchaseOrderUpdate(
    supplierId: Option[UUID],
    locationId: Option[UUID],
    expectedDeliveryDate: Option[ZonedDateTime],
    notes: Option[String],
    products: Option[Seq[PurchaseOrderProductUpsertion]],
  ) extends UpdateEntity[PurchaseOrder]
