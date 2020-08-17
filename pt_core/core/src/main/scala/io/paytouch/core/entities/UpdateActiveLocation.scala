package io.paytouch.core.entities

import java.util.UUID

final case class UpdateActiveLocation(locationId: UUID, active: Boolean)

final case class UpdateActiveItem(itemId: UUID, active: Boolean)
