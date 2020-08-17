package io.paytouch.core.resources.catalogcategories

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.MerchantSetupSteps
import io.paytouch.core.utils.{ SetupStepsAssertions, FixtureDaoFactory => Factory }

class CatalogCategoriesCreateFSpec extends CatalogCategoriesFSpec {

  abstract class CatalogCategoryCreateFSpecContext extends CatalogCategoryResourceFSpecContext with SetupStepsAssertions

  "POST /v1/catalog_categories.create?catalog_category_id" in {

    "if request has valid token" in {
      "if entity is a root catalog category" in {
        "if catalog category doesn't exist" should {
          "create catalog category and return 201" in new CatalogCategoryCreateFSpecContext {
            val newCatalogCategoryId = UUID.randomUUID
            val creation =
              random[CatalogCategoryCreation].copy(position = None, catalogId = catalog.id, availabilities = None)

            Post(s"/v1/catalog_categories.create?catalog_category_id=$newCatalogCategoryId", creation)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusCreated()
              val creationWithAppliedDefault = creation.copy(position = None)
              val catalogCategoryEntity = responseAs[ApiResponse[Category]].data
              val newCatalogCategoryId = catalogCategoryEntity.id
              assertCreation(creationWithAppliedDefault, newCatalogCategoryId)
              assertResponse(catalogCategoryEntity, newCatalogCategoryId)
              assertSetupStepCompleted(merchant, MerchantSetupSteps.SetupMenus)
            }
          }

          "create catalog category with availabilities and return 201" in new CatalogCategoryCreateFSpecContext {
            val newCatalogCategoryId = UUID.randomUUID
            val creation =
              random[CatalogCategoryCreation].copy(
                position = None,
                catalogId = catalog.id,
                availabilities = Some(buildAvailability),
              )

            Post(s"/v1/catalog_categories.create?catalog_category_id=$newCatalogCategoryId", creation)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusCreated()
              val creationWithAppliedDefault = creation.copy(position = None, catalogId = catalog.id)
              val catalogCategoryEntity = responseAs[ApiResponse[Category]].data
              val newCatalogCategoryId = catalogCategoryEntity.id
              assertCreation(creationWithAppliedDefault, newCatalogCategoryId)
              assertResponse(catalogCategoryEntity, newCatalogCategoryId)
            }
          }

          "create catalog category with location overrides and return 201" in new CatalogCategoryCreateFSpecContext {
            val newCatalogCategoryId = UUID.randomUUID
            val creation = random[CatalogCategoryCreation].copy(
              position = None,
              catalogId = catalog.id,
              locationOverrides = Some(
                Map(
                  rome.id -> Some(CategoryLocationUpdate(active = Some(true), availabilities = None)),
                  london.id -> Some(CategoryLocationUpdate(active = Some(false), availabilities = None)),
                ),
              ),
              availabilities = None,
            )

            Post(s"/v1/catalog_categories.create?catalog_category_id=$newCatalogCategoryId", creation)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusCreated()
              val creationWithAppliedDefault = creation.copy(position = None, catalogId = catalog.id)
              val catalogCategoryEntity = responseAs[ApiResponse[Category]].data
              val newCatalogCategoryId = catalogCategoryEntity.id
              assertCreation(creationWithAppliedDefault, newCatalogCategoryId)
              assertResponse(catalogCategoryEntity, newCatalogCategoryId)
              assertItemLocationExists(newCatalogCategoryId, rome.id)
              assertItemLocationExists(newCatalogCategoryId, london.id)
            }
          }

          "reject request if catalog id does not exist" in new CatalogCategoryCreateFSpecContext {
            val newCatalogCategoryId = UUID.randomUUID
            val randomCatalogId = UUID.randomUUID

            val creation =
              random[CatalogCategoryCreation].copy(position = None, catalogId = randomCatalogId, availabilities = None)

            Post(s"/v1/catalog_categories.create?catalog_category_id=$newCatalogCategoryId", creation)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.NotFound)
            }
          }
        }
      }
    }
  }
}
