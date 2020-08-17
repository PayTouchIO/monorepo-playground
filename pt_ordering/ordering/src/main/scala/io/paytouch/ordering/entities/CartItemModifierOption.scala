package io.paytouch.ordering.entities

import java.util.UUID

import io.paytouch.ordering.clients.paytouch.core.entities.{ ModifierOption, ModifierSet }
import io.paytouch.ordering.entities.enums.ModifierSetType

final case class CartItemModifierOption(
    id: UUID,
    modifierOptionId: UUID,
    name: String,
    `type`: ModifierSetType,
    price: MonetaryAmount,
    quantity: BigDecimal,
  )

final case class CartItemModifierOptionCreation(modifierOptionId: UUID, quantity: BigDecimal) {
  def asUpsert =
    CartItemModifierOptionUpsertion(
      modifierOptionId = modifierOptionId,
      quantity = Some(quantity),
    )
}

final case class CartItemModifierOptionUpsertion(modifierOptionId: UUID, quantity: Option[BigDecimal])

final case class ValidCartItemModifierOptionUpsertion(
    upsertion: CartItemModifierOptionUpsertion,
    coreData: (ModifierSet, ModifierOption),
  )
