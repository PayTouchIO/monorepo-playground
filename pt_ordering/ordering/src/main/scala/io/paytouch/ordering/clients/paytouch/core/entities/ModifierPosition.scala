package io.paytouch.ordering.clients.paytouch.core.entities

import java.util.UUID

final case class ModifierPosition(modifierSetId: UUID, position: Option[Int])
