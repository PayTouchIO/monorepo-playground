package io.paytouch.ordering.entities

import java.util.UUID

import io.paytouch.ordering.clients.paytouch.core.entities.VariantOptionWithType

final case class CartItemVariantOption(
    id: UUID,
    variantOptionId: UUID,
    optionName: String,
    optionTypeName: String,
  )

final case class ValidCartItemVariantOptionUpsertion(coreData: VariantOptionWithType)
