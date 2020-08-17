package io.paytouch.core.entities

import io.paytouch.core.data.model.enums.{ ArticleScope, ArticleType }

class RecipeMappedToArticleSpec extends ConvertionSpec {

  "RecipeCreation" should {
    "convert to ArticleCreation without information loss" ! prop { recipe: RecipeCreation =>
      val article = RecipeCreation.convert(recipe)

      article.scope ==== ArticleScope.Part
      article.`type` ==== Some(ArticleType.Simple)
      article.isCombo should beTrue

      recipe.name ==== article.name
      recipe.description ==== article.description
      recipe.categoryIds ==== article.categoryIds
      recipe.brandId ==== article.brandId
      recipe.supplierIds ==== article.supplierIds
      recipe.sku ==== article.sku
      recipe.upc ==== article.upc
      recipe.cost ==== article.cost
      recipe.unit ==== article.unit
      recipe.trackInventory ==== article.trackInventory
      recipe.active ==== article.active
      recipe.applyPricingToAllLocations ==== article.applyPricingToAllLocations
      recipe.locationOverrides.transform { (_, v) =>
        v.map(RecipeLocationUpdate.convert)
      } ==== article.locationOverrides
      recipe.makesQuantity ==== article.makesQuantity
    }
  }

  "RecipeUpdate" should {
    "convert to ArticleUpdate without information loss" ! prop { recipe: RecipeUpdate =>
      val article = RecipeUpdate.convert(recipe)

      article.scope ==== Some(ArticleScope.Part)
      article.`type` ==== Some(ArticleType.Simple)
      article.isCombo ==== None

      recipe.name ==== article.name
      recipe.description ==== article.description
      recipe.categoryIds ==== article.categoryIds
      recipe.brandId ==== article.brandId
      recipe.supplierIds ==== article.supplierIds
      recipe.sku ==== article.sku
      recipe.upc ==== article.upc
      recipe.cost ==== article.cost
      recipe.unit ==== article.unit
      recipe.trackInventory ==== article.trackInventory
      recipe.active ==== article.active
      recipe.applyPricingToAllLocations ==== article.applyPricingToAllLocations
      recipe.locationOverrides.transform { (_, v) =>
        v.map(RecipeLocationUpdate.convert)
      } ==== article.locationOverrides
      recipe.reason ==== article.reason
      recipe.notes ==== article.notes
      recipe.makesQuantity ==== article.makesQuantity
    }
  }

}
