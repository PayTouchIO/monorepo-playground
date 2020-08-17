package io.paytouch.core.data.model.upsertions

import io.paytouch.core.data.model._

final case class ProductPartUpsertion(productParts: Seq[ProductPartUpdate], product: ArticleUpdate)
    extends UpsertionModel[ProductPartRecord]
