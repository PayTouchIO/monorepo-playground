package io.paytouch.core.data.model.upsertions

import io.paytouch.core.data.model._

final case class ProductCategoryUpsertion(
    productCategory: ProductCategoryUpdate,
    productCategoryOption: Option[ProductCategoryOptionUpdate],
  ) extends UpsertionModel[ProductCategoryRecord]
