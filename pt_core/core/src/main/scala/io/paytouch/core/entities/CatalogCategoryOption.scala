package io.paytouch.core.entities

import java.util.UUID

final case class CatalogCategoryOption(
    categoryId: UUID,
    deliveryEnabled: Boolean,
    takeAwayEnabled: Boolean,
  )

final case class CatalogCategoryOptionWithProductId(
    categoryId: UUID,
    productId: UUID,
    deliveryEnabled: Boolean,
    takeAwayEnabled: Boolean,
  ) {
  lazy val catalogCategoryOption =
    CatalogCategoryOption(categoryId = categoryId, deliveryEnabled = deliveryEnabled, takeAwayEnabled = takeAwayEnabled)
}
