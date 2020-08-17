package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.data.model.enums.KitchenType
import io.paytouch.core.entities.enums.ExposedName

final case class Kitchen(
    id: UUID,
    locationId: UUID,
    name: String,
    `type`: KitchenType,
    active: Boolean,
    kdsEnabled: Boolean,
  ) extends ExposedEntity {
  val classShortName = ExposedName.Kitchen
}

final case class KitchenCreation(
    locationId: UUID,
    name: String,
    `type`: KitchenType,
    active: Option[Boolean],
    kdsEnabled: Option[Boolean],
  ) extends CreationEntity[Kitchen, KitchenUpdate] {
  def asUpdate =
    KitchenUpdate(Some(locationId), Some(name), Some(`type`), active.orElse(Some(true)), kdsEnabled.orElse(Some(true)))
}

final case class KitchenUpdate(
    locationId: Option[UUID],
    name: Option[String],
    `type`: Option[KitchenType],
    active: Option[Boolean],
    kdsEnabled: Option[Boolean],
  ) extends UpdateEntity[Kitchen]
