package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model.ProductVariantOptionUpdate
import io.paytouch.core.entities.{ UserContext, VariantArticleUpdate }

trait ProductVariantOptionConversions {

  def toProductVariantOptionUpdates(
      variantProducts: Seq[VariantArticleUpdate],
    )(implicit
      user: UserContext,
    ): Seq[ProductVariantOptionUpdate] =
    variantProducts.flatMap(toProductVariantOptionUpdates)

  def toProductVariantOptionUpdates(
      variantProduct: VariantArticleUpdate,
    )(implicit
      user: UserContext,
    ): Seq[ProductVariantOptionUpdate] =
    variantProduct.optionIds.map { variantOptionId =>
      toProductVariantOptionUpdate(productId = variantProduct.id, variantOptionId = variantOptionId)
    }

  def toProductVariantOptionUpdates(
      productIds: Seq[UUID],
      variantOptionIds: Seq[UUID],
    )(implicit
      user: UserContext,
    ): Seq[ProductVariantOptionUpdate] =
    productIds.flatMap { productId =>
      variantOptionIds.map(variantOptionId => toProductVariantOptionUpdate(productId, variantOptionId))
    }

  def toProductVariantOptionUpdate(
      productId: UUID,
      variantOptionId: UUID,
    )(implicit
      user: UserContext,
    ): ProductVariantOptionUpdate =
    ProductVariantOptionUpdate(
      id = None,
      merchantId = Some(user.merchantId),
      productId = Some(productId),
      variantOptionId = Some(variantOptionId),
    )
}
