package io.paytouch.ordering.clients.paytouch.core.entities

import java.util.UUID

final case class OrderItemVariantOption(
    optionName: String,
    optionTypeName: String,
    position: Int,
  )

final case class OrderItemVariantOptionUpsertion(
    variantOptionId: Option[UUID],
    optionName: Option[String],
    optionTypeName: Option[String],
  )
