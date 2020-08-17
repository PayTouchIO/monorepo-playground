package io.paytouch.ordering.clients.paytouch.core.entities

import java.util.UUID

final case class VariantOptionType(
    id: UUID,
    name: String,
    position: Int,
    options: Seq[VariantOption],
  )

final case class VariantOption(
    id: UUID,
    name: String,
    position: Int,
  )

final case class VariantOptionWithType(
    id: UUID,
    name: String,
    typeName: String,
    position: Int,
    typePosition: Int,
  )
