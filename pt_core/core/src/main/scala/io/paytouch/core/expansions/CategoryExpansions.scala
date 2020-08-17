package io.paytouch.core.expansions

final case class CategoryExpansions(
    withSubcategories: Boolean,
    withLocations: Boolean,
    withProductsCount: Boolean,
    withAvailabilities: Boolean,
  ) extends BaseExpansions

object CategoryExpansions {

  def apply(withProductsCount: Boolean): CategoryExpansions =
    CategoryExpansions(
      withSubcategories = false,
      withLocations = false,
      withProductsCount = withProductsCount,
      withAvailabilities = false,
    )

  def withSubcategoriesOnly =
    CategoryExpansions(
      withSubcategories = true,
      withLocations = false,
      withProductsCount = false,
      withAvailabilities = false,
    )
}
