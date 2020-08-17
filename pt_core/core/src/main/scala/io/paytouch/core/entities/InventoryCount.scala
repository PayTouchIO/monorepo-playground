package io.paytouch.core.entities

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.InventoryCountStatus
import io.paytouch.core.entities.enums.ExposedName

final case class InventoryCount(
    id: UUID,
    user: Option[UserInfo],
    location: Option[Location],
    number: String,
    productsCount: Int,
    valueChange: Option[MonetaryAmount],
    status: InventoryCountStatus,
    synced: Boolean,
    createdAt: ZonedDateTime,
  ) extends ExposedEntity {
  val classShortName = ExposedName.InventoryCount
}

final case class InventoryCountCreation(locationId: UUID, products: Seq[InventoryCountProductUpsertion])
    extends CreationEntity[InventoryCount, InventoryCountUpdate] {
  def asUpdate: InventoryCountUpdate =
    InventoryCountUpdate(
      locationId = locationId,
      products = Some(products),
    )
}

final case class InventoryCountUpdate(locationId: UUID, products: Option[Seq[InventoryCountProductUpsertion]])
    extends UpdateEntity[InventoryCount]
