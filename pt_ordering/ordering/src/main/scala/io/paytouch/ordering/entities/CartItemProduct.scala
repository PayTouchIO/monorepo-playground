package io.paytouch.ordering.entities

import java.util.UUID

final case class CartItemProduct(
    id: UUID,
    name: String,
    description: Option[String],
  )
