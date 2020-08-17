package io.paytouch.ordering.clients.paytouch.core.entities

import java.util.UUID

final case class BundleOption(
    id: UUID,
    article: ArticleInfo,
    priceAdjustment: BigDecimal,
    position: Int,
  )
