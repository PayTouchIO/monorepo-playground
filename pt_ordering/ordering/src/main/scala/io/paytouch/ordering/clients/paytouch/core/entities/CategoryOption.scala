package io.paytouch.ordering.clients.paytouch.core.entities

import java.util.UUID

final case class CategoryOption(
    categoryId: UUID,
    deliveryEnabled: Boolean,
    takeAwayEnabled: Boolean,
  )
