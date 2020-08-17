package io.paytouch.core.resources.categories

import java.time.ZonedDateTime

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, _ }

class CategoriesListFSpec extends CategoriesFSpec {

  abstract class CategoriesListFSpecContext extends CategoryResourceFSpecContext

  "GET /v1/categories.list" in {
    "if request has valid token" in {

      "with no parameter" should {
        "return a paginated list of all categories sorted by position and name" in new CategoriesListFSpecContext {
          val newYork = Factory.location(merchant).create

          val category1Rome =
            Factory
              .systemCategory(defaultMenuCatalog, name = Some("A category"), position = Some(1), locations = Seq(rome))
              .create
          val category2Rome =
            Factory
              .systemCategory(defaultMenuCatalog, name = Some("B category"), position = Some(2), locations = Seq(rome))
              .create
          val category3Rome =
            Factory
              .systemCategory(defaultMenuCatalog, name = Some("B category"), position = Some(1), locations = Seq(rome))
              .create
          val category4NewYork = Factory
            .systemCategory(
              defaultMenuCatalog,
              name = Some("Other location category"),
              position = Some(3),
              locations = Seq(newYork),
            )
            .create

          val catalog = Factory.catalog(merchant).create
          Factory.catalogCategory(catalog).create

          Get("/v1/categories.list").addHeader(authorizationHeader) ~> routes ~> check {
            val categories = responseAs[PaginatedApiResponse[Seq[Category]]].data
            categories.map(_.id) ==== Seq(category1Rome.id, category3Rome.id, category2Rome.id, category4NewYork.id)
          }
        }
      }

      "with location_id,q parameter" should {
        "return a paginated list of all categories sorted by position and name" in new CategoriesListFSpecContext {
          val category1 =
            Factory.systemCategory(defaultMenuCatalog, name = Some("A category"), position = Some(1)).create
          val category2 =
            Factory.systemCategory(defaultMenuCatalog, name = Some("B category"), position = Some(2)).create
          val category3 =
            Factory.systemCategory(defaultMenuCatalog, name = Some("B category"), position = Some(1)).create
          val category1Location = Factory.categoryLocation(category1, rome).create
          val category2Location = Factory.categoryLocation(category2, rome).create

          Get(s"/v1/categories.list?location_id=${rome.id}&q=B").addHeader(authorizationHeader) ~> routes ~> check {
            val categories = responseAs[PaginatedApiResponse[Seq[Category]]].data
            categories.map(_.id) ==== Seq(category2.id)
          }
        }
      }

      "filtered by updated_since date-time" should {
        "return a paginated list of all categories sorted by position and name and filtered by updated_since date-time" in new CategoriesListFSpecContext {
          val now = ZonedDateTime.parse("2015-12-03T20:15:30Z")

          val category1 =
            Factory
              .systemCategory(
                defaultMenuCatalog,
                name = Some("A category"),
                position = Some(0),
                overrideNow = Some(now.minusDays(1)),
              )
              .create
          val category2 =
            Factory
              .systemCategory(
                defaultMenuCatalog,
                name = Some("B category"),
                position = Some(0),
                overrideNow = Some(now),
              )
              .create
          val category3 =
            Factory
              .systemCategory(
                defaultMenuCatalog,
                name = Some("C category"),
                position = Some(0),
                overrideNow = Some(now.plusDays(1)),
              )
              .create

          Get(s"/v1/categories.list?updated_since=2015-12-03").addHeader(authorizationHeader) ~> routes ~> check {
            val categories = responseAs[PaginatedApiResponse[Seq[Category]]].data
            categories.map(_.id) ==== Seq(category2.id, category3.id)
          }
        }
      }

      "with expand[]=subcategories" should {
        "return categories with subcategories expanded" in new CategoriesListFSpecContext {
          val category1 =
            Factory
              .systemCategory(defaultMenuCatalog, name = Some("A category"), position = Some(1), locations = Seq(rome))
              .create
          val category1A = Factory
            .systemSubcategory(
              defaultMenuCatalog,
              category1,
              name = Some("A subcategory"),
              position = Some(2),
              locations = Seq(rome),
            )
            .create
          val category2 =
            Factory
              .systemCategory(defaultMenuCatalog, name = Some("B category"), position = Some(1), locations = Seq(rome))
              .create

          Get("/v1/categories.list?expand[]=subcategories").addHeader(authorizationHeader) ~> routes ~> check {
            val categories = responseAs[PaginatedApiResponse[Seq[Category]]].data
            categories.map(_.id) ==== Seq(category1.id, category2.id)
            categories.find(_.id == category1.id).get.subcategories.map(_.map(_.id)) ==== Some(Seq(category1A.id))
            categories.find(_.id == category2.id).get.subcategories ==== Some(Seq.empty)
          }
        }
      }

      "with expand[]=locations" should {
        "return categories with only accessible and non deleted locations expanded" in new CategoriesListFSpecContext {
          val category = Factory.systemCategory(defaultMenuCatalog).create
          val newYork = Factory.location(merchant).create
          val deletedLocation = Factory.location(merchant, deletedAt = Some(UtcTime.now)).create

          Factory.categoryLocation(category, london, active = Some(true)).create
          Factory.categoryLocation(category, rome, active = Some(false)).create
          Factory.categoryLocation(category, newYork).create
          Factory.categoryLocation(category, deletedLocation).create

          Get("/v1/categories.list?expand[]=locations").addHeader(authorizationHeader) ~> routes ~> check {
            val categories = responseAs[PaginatedApiResponse[Seq[Category]]].data
            categories.map(_.id) ==== Seq(category.id)
            val categoryEntity = categories.head
            categoryEntity.locationOverrides should beSome
            val locationOverrides = categoryEntity.locationOverrides.get
            locationOverrides.keys ==== Set(london.id, rome.id)
            locationOverrides.get(london.id).flatMap(_.active) ==== Some(true)
            locationOverrides.get(rome.id).flatMap(_.active) ==== Some(false)
          }
        }
      }

      "with expand[]=availabilities&location_id" should {
        "return categories with availabilities expanded and filtered by location" in new CategoriesListFSpecContext {
          val category = Factory.systemCategory(defaultMenuCatalog).create

          val categoryRome = Factory.categoryLocation(category, rome, active = Some(true)).create
          val catRomeAvailability =
            Factory.categoryLocationAvailability(categoryRome, Seq(Weekdays.Monday, Weekdays.Tuesday)).create

          val categoryLondon = Factory.categoryLocation(category, london, active = Some(false)).create
          val catLondonAvailability =
            Factory.categoryLocationAvailability(categoryLondon, Seq(Weekdays.Monday, Weekdays.Tuesday)).create

          val expectedAvailabilityMap = Map(
            Weekdays.Monday -> Seq(Availability(catRomeAvailability.start, catRomeAvailability.end)),
            Weekdays.Tuesday -> Seq(Availability(catRomeAvailability.start, catRomeAvailability.end)),
          )

          Get(s"/v1/categories.list?expand[]=availabilities&location_id=${rome.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val categories = responseAs[PaginatedApiResponse[Seq[Category]]].data
            categories.map(_.id) ==== Seq(category.id)

            val categoryEntity = categories.head
            categoryEntity.locationOverrides should beSome
            val locationOverrides = categoryEntity.locationOverrides.get
            locationOverrides.get(rome.id).flatMap(_.active) ==== Some(true)
            locationOverrides.keys.toSeq ==== Seq(rome.id)
            locationOverrides.get(rome.id).flatMap(_.availabilities) ==== Some(expectedAvailabilityMap)
          }
        }
      }

      "with expand[]=availabilities" should {
        "return categories with availabilities expanded only for accessible locations" in new CategoriesListFSpecContext {
          val category = Factory.systemCategory(defaultMenuCatalog).create
          val newYork = Factory.location(merchant).create

          val categoryRome = Factory.categoryLocation(category, rome, active = Some(true)).create
          val catRomeAvailability =
            Factory.categoryLocationAvailability(categoryRome, Seq(Weekdays.Monday, Weekdays.Tuesday)).create

          val categoryLondon = Factory.categoryLocation(category, london, active = Some(false)).create
          val catLondonAvailability =
            Factory.categoryLocationAvailability(categoryLondon, Seq(Weekdays.Monday, Weekdays.Tuesday)).create

          val categoryNewYork = Factory.categoryLocation(category, newYork, active = Some(true)).create
          val catNewYorkAvailability =
            Factory.categoryLocationAvailability(categoryNewYork, Seq(Weekdays.Monday, Weekdays.Tuesday)).create

          val expectedAvailabilityMap = Map(
            Weekdays.Monday -> Seq(Availability(catRomeAvailability.start, catRomeAvailability.end)),
            Weekdays.Tuesday -> Seq(Availability(catRomeAvailability.start, catRomeAvailability.end)),
          )

          Get(s"/v1/categories.list?expand[]=availabilities").addHeader(authorizationHeader) ~> routes ~> check {
            val categories = responseAs[PaginatedApiResponse[Seq[Category]]].data
            categories.map(_.id) ==== Seq(category.id)

            val categoryEntity = categories.head
            categoryEntity.locationOverrides should beSome
            val locationOverrides = categoryEntity.locationOverrides.get
            locationOverrides.get(rome.id).flatMap(_.active) ==== Some(true)
            locationOverrides.keys.toSet ==== Set(rome.id, london.id)
            locationOverrides.get(rome.id).flatMap(_.availabilities) ==== Some(expectedAvailabilityMap)
            locationOverrides.get(london.id).flatMap(_.availabilities) ==== Some(expectedAvailabilityMap)
          }
        }
      }

      "with expand[]=products_count" should {
        "return categories with products count expanded from accessible locations" in new CategoriesListFSpecContext {
          val newYork = Factory.location(merchant).create

          val category = Factory.systemCategory(defaultMenuCatalog).create

          val product1 = Factory.simpleProduct(merchant, locations = Seq(rome), categories = Seq(category)).create
          val product2 = Factory.simpleProduct(merchant, locations = Seq(rome), categories = Seq(category)).create
          val productNewYork =
            Factory.simpleProduct(merchant, locations = Seq(newYork), categories = Seq(category)).create

          Get("/v1/categories.list?expand[]=products_count").addHeader(authorizationHeader) ~> routes ~> check {
            val categories = responseAs[PaginatedApiResponse[Seq[Category]]].data
            categories.map(_.id) ==== Seq(category.id)
            categories.map(_.productsCount.get) ==== Seq(2)
          }
        }
      }

      "with expand[]=products_count and location_id param" should {
        "return categories with products count for products in the given location" in new CategoriesListFSpecContext {
          val category1 = Factory.systemCategory(defaultMenuCatalog, position = Some(1), locations = Seq(rome)).create
          val category2 = Factory.systemCategory(defaultMenuCatalog, position = Some(2), locations = Seq(rome)).create

          val product1 = Factory.simpleProduct(merchant, categories = Seq(category1), locations = Seq(rome)).create
          val product2 = Factory.simpleProduct(merchant, categories = Seq(category1), locations = Seq(london)).create
          val product3 = Factory.simpleProduct(merchant, categories = Seq(category2), locations = Seq(rome)).create
          val product4 = Factory.simpleProduct(merchant, categories = Seq(category2), locations = Seq(london)).create

          Get(s"/v1/categories.list?expand[]=products_count&location_id=${rome.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val categories = responseAs[PaginatedApiResponse[Seq[Category]]].data
            categories.map(_.id) ==== Seq(category1.id, category2.id)
            categories.map(_.productsCount.get) ==== Seq(1, 1)
          }
        }
      }

      "with expand[]=products_count and an unaccessible location_id param" should {
        "return matching list with count set to 0" in new CategoriesListFSpecContext {
          val newYork = Factory.location(merchant).create

          val categoryNewYork =
            Factory.systemCategory(defaultMenuCatalog, position = Some(1), locations = Seq(newYork)).create
          val categoryRome =
            Factory.systemCategory(defaultMenuCatalog, position = Some(1), locations = Seq(rome)).create

          Get(s"/v1/categories.list?expand[]=products_count&location_id=${newYork.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val categories = responseAs[PaginatedApiResponse[Seq[Category]]].data
            categories.map(_.id) ==== Seq(categoryNewYork.id)
            categories.map(_.productsCount.get) ==== Seq(0)
          }
        }
      }

      "with expand[]=subcategories and products_count" should {
        "return categories with subcategories expanded and a products count for subcategories" in new CategoriesListFSpecContext {
          val category = Factory.systemCategory(defaultMenuCatalog, locations = Seq(rome)).create
          val subcategory = Factory.systemSubcategory(defaultMenuCatalog, category, locations = Seq(rome)).create

          val product1 = Factory.simpleProduct(merchant, locations = Seq(rome), categories = Seq(category)).create
          val product2 = Factory.simpleProduct(merchant, locations = Seq(rome), categories = Seq(subcategory)).create
          val product3 = Factory.simpleProduct(merchant, locations = Seq(rome), categories = Seq(subcategory)).create
          val product4 = Factory.simpleProduct(merchant, locations = Seq(london), categories = Seq(subcategory)).create
          val deletedProduct =
            Factory
              .simpleProduct(
                merchant,
                locations = Seq(rome),
                deletedAt = Some(UtcTime.now),
                categories = Seq(subcategory),
              )
              .create

          Get(s"/v1/categories.list?expand[]=subcategories,products_count&location_id=${rome.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val categories = responseAs[PaginatedApiResponse[Seq[Category]]].data
            categories.map(_.id) ==== Seq(category.id)
            categories.map(_.productsCount.get) ==== Seq(1)
            categories.find(_.id == category.id).get.subcategories.get.map(_.id) ==== Seq(subcategory.id)
            categories.find(_.id == category.id).get.subcategories.get.flatMap(_.productsCount) ==== Seq(2)
          }
        }
      }
    }

    "if request has valid app token" should {
      "return categories" in new CategoriesListFSpecContext with AppTokenFixtures {
        Get(s"/v1/categories.list").addHeader(appAuthorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val categories = responseAs[PaginatedApiResponse[Seq[Category]]].data
          categories ==== Seq.empty
        }
      }
    }

    "if request has invalid token" should {
      "reject request" in new CategoriesListFSpecContext {
        Get(s"/v1/categories.list").addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
