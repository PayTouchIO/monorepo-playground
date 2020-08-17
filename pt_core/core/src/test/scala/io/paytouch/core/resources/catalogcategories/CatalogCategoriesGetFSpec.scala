package io.paytouch.core.resources.catalogcategories

import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, _ }

class CatalogCategoriesGetFSpec extends CatalogCategoriesFSpec {

  abstract class CatalogCategoriesGetFSpecContext extends CatalogCategoryResourceFSpecContext

  "GET /v1/catalog_categories.get?catalog_category_id=<catalogCategory-id>" in {
    "if request has valid token" in {

      "if catalog category is a main catalog category" in {
        "with no parameter" should {
          "return a catalog category" in new CatalogCategoriesGetFSpecContext {
            val catalogCategory = Factory.catalogCategory(catalog).create

            Get(s"/v1/catalog_categories.get?catalog_category_id=${catalogCategory.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val catalogCategoryEntity = responseAs[ApiResponse[Category]].data
              assertResponse(catalogCategoryEntity, catalogCategory.id)
            }
          }
          "return a catalog category with image uploads" in new CatalogCategoriesGetFSpecContext {
            val catalogCategory = Factory.catalogCategory(catalog).create

            Get(s"/v1/catalog_categories.get?catalog_category_id=${catalogCategory.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val catalogCategoryEntity = responseAs[ApiResponse[Category]].data
              assertResponse(catalogCategoryEntity, catalogCategory.id)
            }
          }
        }

        "with expand[]=availabilities,products_count" should {
          "return catalog category with availabilities expanded only for accessible locations" in new CatalogCategoriesGetFSpecContext {
            val catalogCategory = Factory.catalogCategory(catalog).create
            val availability = Factory.categoryAvailability(catalogCategory, Seq(Weekdays.Friday)).create

            val newYork = Factory.location(merchant).create

            val product = Factory.simpleProduct(merchant, locations = Seq(rome, london, newYork)).create
            val deletedProduct = Factory
              .simpleProduct(merchant, locations = Seq(rome, london, newYork), deletedAt = Some(UtcTime.now))
              .create
            Factory.productCategory(product, catalogCategory).create
            Factory.productCategory(deletedProduct, catalogCategory).create

            val expectedAvailabilityMap = Map(
              Weekdays.Friday -> Seq(Availability(availability.start, availability.end)),
            )

            Get(
              s"/v1/catalog_categories.get?catalog_category_id=${catalogCategory.id}&expand[]=availabilities,locations,products_count",
            ).addHeader(authorizationHeader) ~> routes ~> check {
              val entity = responseAs[ApiResponse[Category]].data
              assertResponse(entity, catalogCategory.id)

              entity.productsCount ==== Some(1)
              entity.availabilities ==== Some(expectedAvailabilityMap)
            }
          }
        }
      }
    }
  }
}
