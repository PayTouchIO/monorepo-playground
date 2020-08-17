package io.paytouch.core.entities

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.ReturnOrderStatus
import io.paytouch.core.entities.enums.ExposedName

final case class ReturnOrder(
    id: UUID,
    user: Option[UserInfo],
    supplier: Option[Supplier],
    locationId: UUID,
    location: Option[Location],
    purchaseOrder: Option[PurchaseOrder],
    number: String,
    notes: Option[String],
    productsCount: Option[BigDecimal],
    status: ReturnOrderStatus,
    stockValue: Option[MonetaryAmount],
    synced: Boolean,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends ExposedEntity {
  val classShortName = ExposedName.ReturnOrder
}

final case class ReturnOrderCreation(
    userId: UUID,
    supplierId: UUID,
    locationId: UUID,
    purchaseOrderId: Option[UUID],
    notes: Option[String],
    status: ReturnOrderStatus,
    products: Seq[ReturnOrderProductUpsertion],
  ) extends CreationEntity[ReturnOrder, ReturnOrderUpdate] {

  def asUpdate: ReturnOrderUpdate =
    ReturnOrderUpdate(
      userId = Some(userId),
      supplierId = Some(supplierId),
      locationId = Some(locationId),
      purchaseOrderId = purchaseOrderId,
      notes = notes,
      status = Some(status),
      products = Some(products),
    )
}

final case class ReturnOrderUpdate(
    userId: Option[UUID],
    supplierId: Option[UUID],
    locationId: Option[UUID],
    purchaseOrderId: Option[UUID],
    notes: Option[String],
    status: Option[ReturnOrderStatus],
    products: Option[Seq[ReturnOrderProductUpsertion]],
  ) extends UpdateEntity[ReturnOrder]
