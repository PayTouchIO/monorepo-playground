package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model.VariantOptionTypeUpdate
import io.paytouch.core.entities.{ UserContext, VariantOptionTypeUpsertion }

trait VariantOptionTypeConversions {

  def toVariantOptionTypeUpdates(
      mainProductId: UUID,
      variantOptionTypes: Seq[VariantOptionTypeUpsertion],
    )(implicit
      user: UserContext,
    ): Seq[VariantOptionTypeUpdate] =
    variantOptionTypes.zipWithIndex.map {
      case (variantOptionType, index) => toVariantOptionTypeUpdate(mainProductId, variantOptionType, index)
    }

  def toVariantOptionTypeUpdate(
      mainProductId: UUID,
      variantOptionType: VariantOptionTypeUpsertion,
      index: Int,
    )(implicit
      user: UserContext,
    ): VariantOptionTypeUpdate =
    VariantOptionTypeUpdate(
      id = Some(variantOptionType.id),
      merchantId = Some(user.merchantId),
      productId = Some(mainProductId),
      name = Some(variantOptionType.name),
      position = Some(variantOptionType.position.getOrElse(index)),
    )
}
