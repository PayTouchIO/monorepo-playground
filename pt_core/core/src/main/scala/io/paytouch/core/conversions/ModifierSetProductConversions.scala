package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model.ModifierSetProductUpdate
import io.paytouch.core.entities.{ EntityOrdering, UserContext }

trait ModifierSetProductConversions {

  def toModifierSetProductsUpdates(
      productId: UUID,
      modifierSets: Seq[EntityOrdering],
    )(implicit
      user: UserContext,
    ): Seq[ModifierSetProductUpdate] =
    modifierSets.map { eo =>
      toModifierSetProduct(productId = productId, modifierSetId = eo.id, position = Some(eo.position))
    }

  def toModifierSetProducts(
      productIds: Seq[UUID],
      modifierSetIds: Seq[UUID],
    )(implicit
      user: UserContext,
    ): Seq[ModifierSetProductUpdate] =
    productIds.flatMap { productId =>
      modifierSetIds.map(modifierSetId => toModifierSetProduct(productId = productId, modifierSetId = modifierSetId))
    }

  def toModifierSetProduct(
      productId: UUID,
      modifierSetId: UUID,
      position: Option[Int] = None,
    )(implicit
      user: UserContext,
    ): ModifierSetProductUpdate =
    ModifierSetProductUpdate(
      id = None,
      merchantId = Some(user.merchantId),
      productId = Some(productId),
      modifierSetId = Some(modifierSetId),
      position = position,
    )
}
