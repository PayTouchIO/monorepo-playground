package io.paytouch.core.data.model.upsertions

import java.util.UUID

import io.paytouch.core.data.model.{
  ImageUploadUpdate,
  ProductCategoryUpdate,
  ProductVariantOptionUpdate,
  SupplierProductUpdate,
  _,
}

final case class ArticleUpsertion(
    product: ArticleUpdate,
    variantProducts: Option[Seq[VariantArticleUpsertion]],
    variantOptionTypes: Option[Seq[VariantOptionTypeUpdate]],
    variantOptions: Option[Seq[VariantOptionUpdate]],
    productLocations: Map[UUID, Option[ProductLocationUpdate]],
    productLocationTaxRates: Map[UUID, Option[Seq[ProductLocationTaxRateUpdate]]],
    productCategories: Option[Seq[ProductCategoryUpsertion]],
    supplierProducts: Option[Seq[SupplierProductUpdate]],
    imageUploads: Option[Seq[ImageUploadUpdate]],
    recipeDetails: Option[RecipeDetailUpdate],
    bundleSets: Option[Seq[BundleSetUpsertion]],
  ) extends UpsertionModel[ArticleRecord]

final case class VariantArticleUpsertion(
    product: ArticleUpdate,
    productLocations: Map[UUID, Option[ProductLocationUpdate]],
    productLocationTaxRates: Map[UUID, Option[Seq[ProductLocationTaxRateUpdate]]],
    productVariantOptions: Option[Seq[ProductVariantOptionUpdate]],
  ) extends UpsertionModel[ArticleRecord]
