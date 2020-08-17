package io.paytouch.core.entities

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.{ ReceivingObjectStatus, TransferOrderType }
import io.paytouch.core.entities.enums.ExposedName

final case class TransferOrder(
    id: UUID,
    fromLocation: Option[Location],
    toLocation: Option[Location],
    user: Option[UserInfo],
    number: String,
    notes: Option[String],
    status: ReceivingObjectStatus,
    `type`: TransferOrderType,
    productsCount: Option[BigDecimal],
    stockValue: Option[MonetaryAmount],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends ExposedEntity {
  val classShortName = ExposedName.TransferOrder
}

final case class TransferOrderCreation(
    fromLocationId: UUID,
    toLocationId: UUID,
    userId: UUID,
    notes: Option[String],
    `type`: TransferOrderType,
    products: Seq[TransferOrderProductUpsertion],
  ) extends CreationEntity[TransferOrder, TransferOrderUpdate] {
  def asUpdate: TransferOrderUpdate =
    TransferOrderUpdate(
      fromLocationId = Some(fromLocationId),
      toLocationId = Some(toLocationId),
      userId = Some(userId),
      notes = notes,
      `type` = Some(`type`),
      products = Some(products),
    )
}

final case class TransferOrderUpdate(
    fromLocationId: Option[UUID],
    toLocationId: Option[UUID],
    userId: Option[UUID],
    notes: Option[String],
    `type`: Option[TransferOrderType],
    products: Option[Seq[TransferOrderProductUpsertion]],
  ) extends UpdateEntity[TransferOrder]
