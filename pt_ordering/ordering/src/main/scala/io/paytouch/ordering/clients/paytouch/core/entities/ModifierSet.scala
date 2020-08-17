package io.paytouch.ordering.clients.paytouch.core.entities

import java.util.UUID

import io.paytouch.ordering.entities.enums.ModifierSetType

final case class ModifierSet(
    id: UUID,
    `type`: ModifierSetType,
    name: String,
    minimumOptionCount: Int,
    maximumOptionCount: Option[Int],
    singleChoice: Boolean,
    force: Boolean,
    locationOverrides: Map[UUID, ItemLocation],
    options: Seq[ModifierOption],
  )
