package io.paytouch.core.resources.products

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class RecipesCreateFSpec extends RecipesFSpec {

  abstract class RecipesCreateFSpecContext extends RecipeResourceFSpecContext

  "POST /v1/recipes.create" in {

    "if request has valid token" in {

      "if relations belong to same merchant" should {

        "create simple recipe without variants, its relations and return 201" in new RecipesCreateFSpecContext {
          val category1 = Factory.systemCategory(defaultMenuCatalog).create
          val category2 = Factory.systemCategory(defaultMenuCatalog).create
          val supplier = Factory.supplier(merchant).create

          val newRecipeId = UUID.randomUUID
          val categoryIds = Seq(category1.id, category2.id)
          val supplierIds = Seq(supplier.id)

          val creation = random[RecipeCreation].copy(categoryIds = categoryIds, supplierIds = supplierIds)

          Post(s"/v1/recipes.create?recipe_id=$newRecipeId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            assertCreation(creation, newRecipeId, Some(categoryIds), Some(supplierIds))
            assertComboType(newRecipeId)
          }
        }

        "create recipe with location settings, their relations and return 201" in new RecipesCreateFSpecContext {
          val category1 = Factory.systemCategory(defaultMenuCatalog).create
          val category2 = Factory.systemCategory(defaultMenuCatalog).create
          val supplier = Factory.supplier(merchant).create

          val newRecipeId = UUID.randomUUID
          val categoryIds = Seq(category1.id, category2.id)
          val supplierIds = Seq(supplier.id)

          val locationOverrides = Map(rome.id -> Some(random[RecipeLocationUpdate]))

          val creation = random[RecipeCreation].copy(
            categoryIds = categoryIds,
            locationOverrides = locationOverrides,
            supplierIds = supplierIds,
          )

          Post(s"/v1/recipes.create?recipe_id=$newRecipeId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            assertCreation(creation, newRecipeId, Some(categoryIds), Some(supplierIds))
            assertComboType(newRecipeId)
          }
        }

        "create recipe with location settings, location tax rates, their relations and return 201" in new RecipesCreateFSpecContext {
          val category1 = Factory.systemCategory(defaultMenuCatalog).create
          val category2 = Factory.systemCategory(defaultMenuCatalog).create
          val supplier = Factory.supplier(merchant).create
          val taxRate = Factory.taxRate(merchant).create
          Factory.taxRateLocation(taxRate, rome).create

          val newRecipeId = UUID.randomUUID
          val categoryIds = Seq(category1.id, category2.id)
          val supplierIds = Seq(supplier.id)

          val locationOverrides = Map(rome.id -> Some(random[RecipeLocationUpdate]))

          val creation = random[RecipeCreation].copy(
            categoryIds = categoryIds,
            locationOverrides = locationOverrides,
            supplierIds = supplierIds,
          )

          Post(s"/v1/recipes.create?recipe_id=$newRecipeId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            assertCreation(creation, newRecipeId, Some(categoryIds), Some(supplierIds))
            assertComboType(newRecipeId)
          }
        }
      }

      "if recipe doesn't belong to current user's merchant" should {

        "not create recipe, its relations and return 404" in new RecipeResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorDefaultMenuCatalog =
            Factory.defaultMenuCatalog(competitor).create
          val competitorCategory1 = Factory.systemCategory(competitorDefaultMenuCatalog).create
          val competitorRecipe = Factory.comboPart(competitor, categories = Seq(competitorCategory1)).create

          val category1 = Factory.systemCategory(defaultMenuCatalog).create
          val categoryIds = Seq(category1.id)

          val recipeUpdate = random[RecipeCreation].copy(categoryIds = categoryIds)

          Post(s"/v1/recipes.create?recipe_id=${competitorRecipe.id}", recipeUpdate)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)

            val updatedRecipe = partDao.findById(competitorRecipe.id).await.get
            updatedRecipe ==== competitorRecipe

            productCategoryDao.findByProductId(competitorRecipe.id).await.map(_.categoryId) ==== Seq(
              competitorCategory1.id,
            )
          }
        }
      }
    }

    "if request has invalid token" should {

      "be rejected" in new RecipeResourceFSpecContext {
        val newRecipeId = UUID.randomUUID
        val creation = random[RecipeCreation]
        Post(s"/v1/recipes.create?recipe_id=$newRecipeId", creation)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
