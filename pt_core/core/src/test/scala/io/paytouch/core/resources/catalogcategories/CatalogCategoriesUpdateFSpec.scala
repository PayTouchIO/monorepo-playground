package io.paytouch.core.resources.catalogcategories

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, _ }

class CatalogCategoriesUpdateFSpec extends CatalogCategoriesFSpec {

  abstract class CatalogCategoriesUpdateFSpecContext extends CatalogCategoryResourceFSpecContext with Fixtures

  trait Fixtures extends MultipleLocationFixtures { self: CatalogCategoryResourceFSpecContext =>
    val catalogCategory = Factory.catalogCategory(catalog).create
    val newYork = Factory.location(merchant).create
  }

  "POST /v1/catalog_categories.update?catalog_category_id<catalogCategory-id>" in {
    "if request has valid token" in {
      "if entity is a root catalog category" in {
        "if catalog category exists" should {
          "if catalog category belongs to current merchant" should {
            "update catalogCategory and return 200" in new CatalogCategoriesUpdateFSpecContext {
              val catalogCategoryUpdate =
                random[CatalogCategoryUpdate].copy(position = Some(0), catalogId = None, availabilities = None)

              Post(s"/v1/catalog_categories.update?catalog_category_id=${catalogCategory.id}", catalogCategoryUpdate)
                .addHeader(authorizationHeader) ~> routes ~> check {
                assertStatusOK()
                assertUpdate(catalogCategoryUpdate, catalogCategory.id)
                val catalogCategoryEntity = responseAs[ApiResponse[Category]].data
                assertResponse(catalogCategoryEntity, catalogCategory.id)
              }
            }

            "update catalog category with location overrides and return 200" in new CatalogCategoriesUpdateFSpecContext {
              val catalogCategoryUpdate = random[CatalogCategoryUpdate].copy(
                catalogId = None,
                locationOverrides = Some(
                  Map(
                    rome.id -> Some(CategoryLocationUpdate(active = Some(true), availabilities = None)),
                    london.id -> Some(CategoryLocationUpdate(active = Some(false), availabilities = None)),
                  ),
                ),
                availabilities = None,
              )

              Post(s"/v1/catalog_categories.update?catalog_category_id=${catalogCategory.id}", catalogCategoryUpdate)
                .addHeader(authorizationHeader) ~> routes ~> check {
                assertStatusOK()
                assertUpdate(catalogCategoryUpdate, catalogCategory.id)
                val catalogCategoryEntity = responseAs[ApiResponse[Category]].data
                assertResponse(catalogCategoryEntity, catalogCategory.id)
                assertItemLocationExists(catalogCategory.id, rome.id)
                assertItemLocationExists(catalogCategory.id, london.id)
              }
            }
          }

          "if catalog category doesn't belong to current merchant" should {
            "not update catalogCategory and return 404" in new CatalogCategoriesUpdateFSpecContext {
              val competitor = Factory.merchant.create
              val competitorCatalog = Factory.catalog(competitor).create
              val competitorCatalogCategory = Factory.catalogCategory(competitorCatalog).create
              val catalogCategoryUpdate = random[CatalogCategoryUpdate]

              Post(
                s"/v1/catalog_categories.update?catalog_category_id=${competitorCatalogCategory.id}",
                catalogCategoryUpdate,
              ).addHeader(authorizationHeader) ~> routes ~> check {
                assertStatus(StatusCodes.NotFound)

                catalogCategoryDao.findById(competitorCatalogCategory.id).await.get ==== competitorCatalogCategory
              }
            }
          }
          "if catalog id does not exist" should {
            "reject the request" in new CatalogCategoriesUpdateFSpecContext {
              val randomCatalogId = UUID.randomUUID

              val catalogCategoryUpdate =
                random[CatalogCategoryUpdate].copy(
                  position = Some(0),
                  catalogId = Some(randomCatalogId),
                  availabilities = None,
                )

              Post(s"/v1/catalog_categories.update?catalog_category_id=${catalogCategory.id}", catalogCategoryUpdate)
                .addHeader(authorizationHeader) ~> routes ~> check {
                assertStatus(StatusCodes.NotFound)
              }
            }
          }
        }
      }
    }
  }
}
