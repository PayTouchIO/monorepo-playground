package io.paytouch.core.expansions

final case class CatalogExpansions(
    withProductsCount: Boolean,
    withCategoriesCount: Boolean,
    withAvailabilities: Boolean,
    withLocationOverrides: Boolean,
  ) extends BaseExpansions

object CatalogExpansions {
  def empty: CatalogExpansions =
    CatalogExpansions(
      withProductsCount = false,
      withCategoriesCount = false,
      withAvailabilities = false,
      withLocationOverrides = false,
    )
}
