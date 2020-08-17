package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.data.model.enums._
import io.paytouch.core.entities.enums.ExposedName

final case class OrderItemModifierOption(
    id: UUID,
    orderItemId: UUID,
    modifierOptionId: Option[UUID],
    name: String,
    modifierSetName: Option[String],
    `type`: ModifierSetType,
    price: MonetaryAmount,
    quantity: BigDecimal,
  ) extends ExposedEntity {
  val classShortName = ExposedName.OrderItemModifierOption
}

final case class OrderItemModifierOptionUpsertion(
    modifierOptionId: Option[UUID], // Temp HOT-FIX for bug register PR-1488
    name: String,
    `type`: ModifierSetType,
    price: BigDecimal,
    quantity: BigDecimal,
  )
