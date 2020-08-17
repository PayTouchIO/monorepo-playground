package io.paytouch.core.expansions

final case class ArticleExpansions(
    withSystemCategories: Boolean,
    withSystemCategoryIds: Boolean,
    withVariants: Boolean,
    withModifiers: Boolean,
    withModifierIds: Boolean,
    withModifierPositions: Boolean,
    withTaxRates: Boolean,
    withTaxRateLocations: Boolean,
    withTaxRateIds: Boolean,
    withStockLevel: Boolean,
    withSuppliers: Boolean,
    withRecipeDetails: Boolean,
    withSystemCategoryPositions: Boolean,
    withReorderAmount: Boolean,
    withPriceRanges: Boolean,
    withCostRanges: Boolean,
    withCatalogCategories: Boolean,
    withCatalogCategoryPositions: Boolean,
    withCatalogCategoryOptions: Boolean,
  ) extends BaseExpansions

object ArticleExpansions {

  val empty =
    new ArticleExpansions(
      withSystemCategories = false,
      withSystemCategoryIds = false,
      withVariants = false,
      withModifiers = false,
      withModifierIds = false,
      withModifierPositions = false,
      withTaxRates = false,
      withTaxRateLocations = false,
      withTaxRateIds = false,
      withStockLevel = false,
      withSuppliers = false,
      withRecipeDetails = false,
      withSystemCategoryPositions = false,
      withReorderAmount = false,
      withPriceRanges = false,
      withCostRanges = false,
      withCatalogCategories = false,
      withCatalogCategoryPositions = false,
      withCatalogCategoryOptions = false,
    )

  val withVariantsOnly = empty.copy(withVariants = true)
}
