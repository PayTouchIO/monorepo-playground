package io.paytouch.ordering.entities

import java.util.UUID

final case class UpdateActiveItem(itemId: UUID, active: Boolean)
