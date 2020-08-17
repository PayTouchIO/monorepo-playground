package io.paytouch.core.resources.products

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ UtcTime, FixtureDaoFactory => Factory }

class RecipesUpdateFSpec extends RecipesFSpec {

  abstract class RecipesUpdateFSpecContext extends RecipeResourceFSpecContext

  "POST /v1/recipes.update?recipe_id=<recipe-id>" in {

    "if request has valid token" in {

      "if recipe and its relations belong to same merchant" should {

        "update recipe, its relations and return 200" in new RecipeResourceFSpecContext {
          val category1 = Factory.systemCategory(defaultMenuCatalog).create
          val recipe = Factory.comboPart(merchant, categories = Seq(category1)).create
          val supplier = Factory.supplier(merchant).create

          val categoryIds = Seq(category1.id)
          val supplierIds = Seq(supplier.id)

          val update = random[RecipeUpdate].copy(categoryIds = Some(categoryIds), supplierIds = Some(supplierIds))

          Post(s"/v1/recipes.update?recipe_id=${recipe.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertUpdate(update, recipe.id, Some(categoryIds), Some(supplierIds))
            assertComboType(recipe.id)
          }
        }

        "update recipe and update location settings, their relations and return 200" in new RecipeResourceFSpecContext {
          val category1 = Factory.systemCategory(defaultMenuCatalog).create
          val category2 = Factory.systemCategory(defaultMenuCatalog).create
          val categoryIds = Seq(category1.id, category2.id)
          val supplier = Factory.supplier(merchant).create
          val supplierIds = Seq(supplier.id)

          val recipe = Factory.comboPart(merchant, categories = Seq(category1), locations = Seq(rome)).create

          val locationOverrides = Map(rome.id -> Some(random[RecipeLocationUpdate]))

          val update = random[RecipeUpdate].copy(
            categoryIds = Some(categoryIds),
            description = Some("description"),
            locationOverrides = locationOverrides,
            supplierIds = Some(supplierIds),
          )

          Post(s"/v1/recipes.update?recipe_id=${recipe.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertUpdate(update, recipe.id, Some(categoryIds), Some(supplierIds))
            assertComboType(recipe.id)
          }
        }

        "update recipe and update location settings, location tax rates, their relations and return 200" in new RecipeResourceFSpecContext {
          val category1 = Factory.systemCategory(defaultMenuCatalog).create
          val category2 = Factory.systemCategory(defaultMenuCatalog).create
          val categoryIds = Seq(category1.id, category2.id)
          val supplier = Factory.supplier(merchant).create
          val supplierIds = Seq(supplier.id)
          val taxRate = Factory.taxRate(merchant).create
          Factory.taxRateLocation(taxRate, rome).create

          val recipe = Factory.comboPart(merchant, categories = Seq(category1), locations = Seq(rome)).create

          val locationOverrides = Map(rome.id -> Some(random[RecipeLocationUpdate]))

          val update = random[RecipeUpdate].copy(
            categoryIds = Some(categoryIds),
            description = Some("description"),
            locationOverrides = locationOverrides,
            supplierIds = Some(supplierIds),
          )

          Post(s"/v1/recipes.update?recipe_id=${recipe.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertUpdate(update, recipe.id, Some(categoryIds), Some(supplierIds))
            assertComboType(recipe.id)
          }
        }

        "update recipe with no location settings" should {
          "leave the recipe location data intact and return 200" in new RecipeResourceFSpecContext {
            val recipe = Factory.comboPart(merchant).create
            val productLocation = Factory.productLocation(recipe, rome).create
            val update = random[RecipeUpdate]

            Post(s"/v1/recipes.update?recipe_id=${recipe.id}", update)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              assertUpdate(update, recipe.id)
              assertComboType(recipe.id)

              productLocationDao.findById(productLocation.id).await.get ==== productLocation
            }
          }
        }

        "update recipe with a null location overrides" should {
          "remove the recipe location data and return 200" in new RecipeResourceFSpecContext {
            val recipe = Factory.comboPart(merchant).create
            val productLocation = Factory.productLocation(recipe, rome).create
            val locationOverrides = Map(rome.id -> None)
            val update =
              random[RecipeUpdate].copy(description = Some("description"), locationOverrides = locationOverrides)

            Post(s"/v1/recipes.update?recipe_id=${recipe.id}", update)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              assertUpdate(update, recipe.id)
              assertComboType(recipe.id)

              productLocationDao.findById(productLocation.id).await ==== None
            }
          }
        }
      }

      "if recipe doesn't belong to current user's merchant" should {

        "not update recipe, its relations and return 404" in new RecipeResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorDefaultMenuCatalog =
            Factory.defaultMenuCatalog(competitor).create
          val competitorCategory1 = Factory.systemCategory(competitorDefaultMenuCatalog).create
          val competitorRecipe = Factory.comboPart(competitor, categories = Seq(competitorCategory1)).create

          val category1 = Factory.systemCategory(defaultMenuCatalog).create
          val categoryIds = Seq(category1.id)

          val update = random[RecipeUpdate].copy(categoryIds = Some(categoryIds))

          Post(s"/v1/recipes.update?recipe_id=${competitorRecipe.id}", update)
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

      "if category doesn't belong to current user's merchant" should {

        "update recipe, update own categories, skip spurious categories and return 200" in new RecipeResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorDefaultMenuCatalog =
            Factory.defaultMenuCatalog(competitor).create
          val competitorCategory1 = Factory.systemCategory(competitorDefaultMenuCatalog).create

          val category1 = Factory.systemCategory(defaultMenuCatalog).create
          val recipe = Factory.comboPart(merchant, categories = Seq(category1)).create

          val update = random[RecipeUpdate].copy(categoryIds = Some(Seq(category1.id, competitorCategory1.id)))

          Post(s"/v1/recipes.update?recipe_id=${recipe.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertUpdate(update, recipe.id, categories = Some(Seq(category1.id)))
            assertComboType(recipe.id)
          }
        }
      }

      "if a location id does not belong to the merchant" should {
        "reject the request with a 404" in new RecipeResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorRecipe = Factory.comboPart(competitor).create

          val locationOverrides = Map(rome.id -> Some(random[RecipeLocationUpdate]))

          val newRecipeId = UUID.randomUUID

          val update = random[RecipeUpdate].copy(locationOverrides = locationOverrides)

          Post(s"/v1/recipes.update?recipe_id=$newRecipeId", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)

            partDao.findById(newRecipeId).await ==== None
          }
        }
      }
    }

    "if recipe has been deleted" should {

      "reject the request with a 404" in new RecipeResourceFSpecContext {
        val deletedRecipe = Factory.comboPart(merchant, deletedAt = Some(UtcTime.now)).create
        val update = random[RecipeUpdate]
        Post(s"/v1/recipes.update?recipe_id=${deletedRecipe.id}", update)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NotFound)
        }
      }
    }

    "if request has invalid token" should {

      "be rejected" in new RecipeResourceFSpecContext {
        val newRecipeId = UUID.randomUUID
        val update = random[RecipeUpdate]
        Post(s"/v1/recipes.update?recipe_id=$newRecipeId", update)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
