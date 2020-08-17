package io.paytouch.core.resources.categories

import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, _ }

class CategoriesGetFSpec extends CategoriesFSpec {

  abstract class CategoriesGetFSpecContext extends CategoryResourceFSpecContext

  "GET /v1/categories.get?category_id=<category-id>" in {
    "if request has valid token" in {

      "if category is a main category" in {
        "with no parameter" should {
          "return a category" in new CategoriesGetFSpecContext {
            val category = Factory.systemCategory(defaultMenuCatalog).create

            Get(s"/v1/categories.get?category_id=${category.id}").addHeader(authorizationHeader) ~> routes ~> check {
              val categoryEntity = responseAs[ApiResponse[Category]].data
              assertResponse(categoryEntity, category.id)
            }
          }
          "return a category with image uploads" in new CategoriesGetFSpecContext {
            val category = Factory.systemCategory(defaultMenuCatalog).create

            Get(s"/v1/categories.get?category_id=${category.id}").addHeader(authorizationHeader) ~> routes ~> check {
              val categoryEntity = responseAs[ApiResponse[Category]].data
              assertResponse(categoryEntity, category.id)
            }
          }
        }

        "with expand[]=subcategories" should {
          "return categories with subcategories expanded" in new CategoriesGetFSpecContext {
            val category =
              Factory.systemCategory(defaultMenuCatalog, name = Some("A category"), position = Some(1)).create
            val subcategory1 =
              Factory.systemSubcategory(defaultMenuCatalog, category, position = Some(2), active = Some(true)).create
            val subcategory2 =
              Factory.systemSubcategory(defaultMenuCatalog, category, position = Some(1), active = Some(true)).create
            val subcategory3 =
              Factory.systemSubcategory(defaultMenuCatalog, category, position = Some(0), active = Some(false)).create

            Get(s"/v1/categories.get?category_id=${category.id}&expand[]=subcategories")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val categoryEntity = responseAs[ApiResponse[Category]].data
              assertResponse(categoryEntity, category.id)
              val subcategoryEntities = categoryEntity.subcategories.get
              subcategoryEntities.map(_.id) ==== Seq(subcategory3.id, subcategory2.id, subcategory1.id)
              assertResponse(subcategoryEntities.find(_.id == subcategory1.id).get, subcategory1.id)
              assertResponse(subcategoryEntities.find(_.id == subcategory2.id).get, subcategory2.id)
              assertResponse(subcategoryEntities.find(_.id == subcategory3.id).get, subcategory3.id)

            }
          }
        }

        "with expand[]=locations" should {
          "return category with only accessible and non-deleted locations" in new CategoriesGetFSpecContext {
            val category = Factory.systemCategory(defaultMenuCatalog).create
            val newYork = Factory.location(merchant).create
            val paris = Factory.location(merchant, deletedAt = Some(UtcTime.now)).create
            Factory.userLocation(user, paris).create

            val categoryRome = Factory.categoryLocation(category, rome).create
            val categoryLondon = Factory.categoryLocation(category, london).create
            val categoryNewYork = Factory.categoryLocation(category, newYork).create
            val categoryParis = Factory.categoryLocation(category, paris).create

            Get(s"/v1/categories.get?category_id=${category.id}&expand[]=locations")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val categoryEntity = responseAs[ApiResponse[Category]]
              assertResponse(categoryEntity.data, category.id)

              val locations = categoryEntity.data.locationOverrides.get
              locations.keys.toSet ==== Set(rome.id, london.id)
            }
          }
        }

        "with expand[]=availabilities,locations,products_count" should {
          "return category with availabilities expanded only for accessible locations" in new CategoriesGetFSpecContext {
            val category = Factory.systemCategory(defaultMenuCatalog).create
            val newYork = Factory.location(merchant).create

            val product = Factory.simpleProduct(merchant, locations = Seq(rome, london, newYork)).create
            val deletedProduct = Factory
              .simpleProduct(merchant, locations = Seq(rome, london, newYork), deletedAt = Some(UtcTime.now))
              .create
            Factory.productCategory(product, category).create
            Factory.productCategory(deletedProduct, category).create

            val categoryRome = Factory.categoryLocation(category, rome).create
            val catRomeAvailability =
              Factory.categoryLocationAvailability(categoryRome, Seq(Weekdays.Monday, Weekdays.Tuesday)).create

            val categoryLondon = Factory.categoryLocation(category, london).create
            val catLondonAvailability =
              Factory
                .categoryLocationAvailability(categoryLondon, Seq(Weekdays.Monday, Weekdays.Wednesday))
                .create

            val categoryNewYork = Factory.categoryLocation(category, newYork).create
            val catNewYorkAvailability =
              Factory
                .categoryLocationAvailability(categoryNewYork, Seq(Weekdays.Monday, Weekdays.Wednesday))
                .create

            val expectedAvailabilityMap1 = Map(
              Weekdays.Monday -> Seq(Availability(catRomeAvailability.start, catRomeAvailability.end)),
              Weekdays.Tuesday -> Seq(Availability(catRomeAvailability.start, catRomeAvailability.end)),
            )

            val expectedAvailabilityMap2 = Map(
              Weekdays.Monday -> Seq(Availability(catLondonAvailability.start, catLondonAvailability.end)),
              Weekdays.Wednesday -> Seq(Availability(catLondonAvailability.start, catLondonAvailability.end)),
            )

            Get(s"/v1/categories.get?category_id=${category.id}&expand[]=availabilities,locations,products_count")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val categoryEntity = responseAs[ApiResponse[Category]].data
              assertResponse(categoryEntity, category.id)

              val locations = categoryEntity.locationOverrides.get
              locations.keys.toSeq ==== Seq(rome.id, london.id)
              locations.get(rome.id).flatMap(_.availabilities) ==== Some(expectedAvailabilityMap1)
              locations.get(london.id).flatMap(_.availabilities) ==== Some(expectedAvailabilityMap2)

              categoryEntity.productsCount ==== Some(1)
            }
          }
        }
      }

      "if category is a subcategory" in {
        "return a category" in new CategoriesGetFSpecContext {
          val subcategory = {
            val category = Factory.systemCategory(defaultMenuCatalog).create
            Factory.systemSubcategory(defaultMenuCatalog, category).create
          }

          Get(s"/v1/categories.get?category_id=${subcategory.id}").addHeader(authorizationHeader) ~> routes ~> check {
            val categoryEntity = responseAs[ApiResponse[Category]].data
            assertResponse(categoryEntity, subcategory.id)
          }
        }
      }
    }
  }
}
