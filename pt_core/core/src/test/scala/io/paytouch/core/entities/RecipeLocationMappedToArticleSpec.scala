package io.paytouch.core.entities

class RecipeLocationMappedToArticleSpec extends ConvertionSpec {

  "RecipeLocationUpdate" should {
    "convert to ArticleLocationUpdate without information loss" ! prop { recipeLocation: RecipeLocationUpdate =>
      val articleLocation = RecipeLocationUpdate.convert(recipeLocation)

      recipeLocation.cost ==== articleLocation.cost
      recipeLocation.unit ==== articleLocation.unit
      recipeLocation.active ==== articleLocation.active
    }
  }

}
