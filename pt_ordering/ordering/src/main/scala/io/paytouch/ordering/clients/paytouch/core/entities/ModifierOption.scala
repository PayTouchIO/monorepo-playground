package io.paytouch.ordering.clients.paytouch.core.entities

import java.util.UUID

import io.paytouch.ordering.entities.MonetaryAmount

final case class ModifierOption(
    id: UUID,
    name: String,
    price: MonetaryAmount,
    maximumCount: Option[Int],
    position: Int,
    active: Boolean,
  )
