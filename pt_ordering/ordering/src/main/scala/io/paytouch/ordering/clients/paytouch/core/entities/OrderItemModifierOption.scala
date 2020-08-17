package io.paytouch.ordering.clients.paytouch.core.entities

import java.util.UUID

import io.paytouch.ordering.entities.MonetaryAmount
import io.paytouch.ordering.entities.enums.ModifierSetType

final case class OrderItemModifierOption(
    name: String,
    modifierSetName: Option[String],
    `type`: ModifierSetType,
    price: MonetaryAmount,
    quantity: BigDecimal,
  )

final case class OrderItemModifierOptionUpsertion(
    modifierOptionId: Option[UUID],
    name: String,
    `type`: ModifierSetType,
    price: BigDecimal,
    quantity: BigDecimal,
  )
