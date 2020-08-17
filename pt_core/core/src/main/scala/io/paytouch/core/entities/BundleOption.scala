package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.entities.enums.ExposedName

final case class BundleOption(
    id: UUID,
    article: ArticleInfo,
    priceAdjustment: BigDecimal,
    position: Int,
  )

final case class BundleOptionUpdate(
    id: UUID,
    articleId: UUID,
    priceAdjustment: BigDecimal,
    position: Option[Int],
  )
