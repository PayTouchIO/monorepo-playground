package io.paytouch.ordering.clients.paytouch.core.entities

import java.util.UUID

final case class ArticleInfo(
    id: UUID,
    name: String,
    sku: Option[String],
    upc: Option[String],
    options: Option[Seq[VariantOptionWithType]],
  )
