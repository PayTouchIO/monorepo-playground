package io.paytouch.core.resources.catalogcategories

import java.time.ZonedDateTime

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{ AuthenticationFailedRejection, MissingQueryParamRejection }
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, _ }

class CatalogCategoriesListFSpec extends CatalogCategoriesFSpec {

  abstract class CatalogCategoriesListFSpecContext extends CatalogCategoryResourceFSpecContext {
    lazy val anotherCatalog = Factory.catalog(merchant).create

    val anotherCatalogCategory = Factory
      .catalogCategory(anotherCatalog, name = Some("Another catalog category"), position = Some(3))
      .create
  }

  "GET /v1/catalog_categories.list" in {
    "if request has valid token" in {

      "with catalog_id parameter" should {

        "with no extra parameter" should {
          "return a paginated list of all catalog categories sorted by position and name" in new CatalogCategoriesListFSpecContext {
            val catalogCategory1 =
              Factory.catalogCategory(catalog, name = Some("A catalog category"), position = Some(1)).create
            val catalogCategory2 =
              Factory.catalogCategory(catalog, name = Some("B catalog category"), position = Some(2)).create
            val catalogCategory3 =
              Factory.catalogCategory(catalog, name = Some("B catalog category"), position = Some(1)).create
            val catalogCategory4 = Factory
              .catalogCategory(catalog, name = Some("Other location catalog category"), position = Some(3))
              .create

            Get(s"/v1/catalog_categories.list?catalog_id=${catalog.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val catalogCategories = responseAs[PaginatedApiResponse[Seq[Category]]]
              catalogCategories.data.map(_.id) ==== Seq(
                catalogCategory1.id,
                catalogCategory3.id,
                catalogCategory2.id,
                catalogCategory4.id,
              )
            }
          }
        }

        "with location_id,q parameter" should {
          "return a paginated list of all catalog categories sorted by position and name" in new CatalogCategoriesListFSpecContext {
            val catalogCategory1 =
              Factory.catalogCategory(catalog, name = Some("A catalog category"), position = Some(1)).create
            val catalogCategory2 =
              Factory.catalogCategory(catalog, name = Some("B catalog category"), position = Some(2)).create

            Get(s"/v1/catalog_categories.list?catalog_id=${catalog.id}&q=B")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val catalogCategories = responseAs[PaginatedApiResponse[Seq[Category]]]
              catalogCategories.data.map(_.id) ==== Seq(catalogCategory2.id)
            }
          }
        }

        "filtered by updated_since date-time" should {
          "return a paginated list of all catalog categories sorted by position and name and filtered by updated_since date-time" in new CatalogCategoriesListFSpecContext {
            val now = ZonedDateTime.parse("2015-12-03T20:15:30Z")

            val catalogCategory1 =
              Factory
                .catalogCategory(
                  catalog,
                  name = Some("A catalog category"),
                  position = Some(0),
                  overrideNow = Some(now.minusDays(1)),
                )
                .create
            val catalogCategory2 =
              Factory
                .catalogCategory(
                  catalog,
                  name = Some("B catalog category"),
                  position = Some(0),
                  overrideNow = Some(now),
                )
                .create
            val catalogCategory3 =
              Factory
                .catalogCategory(
                  catalog,
                  name = Some("C catalog category"),
                  position = Some(0),
                  overrideNow = Some(now.plusDays(1)),
                )
                .create

            Get(s"/v1/catalog_categories.list?catalog_id=${catalog.id}&updated_since=2015-12-03")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val catalogCategories = responseAs[PaginatedApiResponse[Seq[Category]]]
              catalogCategories.data.map(_.id) ==== Seq(catalogCategory2.id, catalogCategory3.id)
            }
          }
        }

        "with expand[]=products_count" should {
          "return catalog categories with products count expanded from accessible locations" in new CatalogCategoriesListFSpecContext {
            val newYork = Factory.location(merchant).create

            val catalogCategory = Factory.catalogCategory(catalog).create

            val product1 =
              Factory.simpleProduct(merchant, locations = Seq(rome), categories = Seq(catalogCategory)).create
            val product2 =
              Factory.simpleProduct(merchant, locations = Seq(rome), categories = Seq(catalogCategory)).create
            val productNewYork =
              Factory.simpleProduct(merchant, locations = Seq(newYork), categories = Seq(catalogCategory)).create

            Get(s"/v1/catalog_categories.list?catalog_id=${catalog.id}&expand[]=products_count")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val catalogCategories = responseAs[PaginatedApiResponse[Seq[Category]]]
              catalogCategories.data.map(_.id) ==== Seq(catalogCategory.id)
              catalogCategories.data.map(_.productsCount.get) ==== Seq(2)
            }
          }
        }
      }

      "with no parameters" should {
        "reject the request" in new CatalogCategoriesListFSpecContext {
          Get(s"/v1/catalog_categories.list")
            .addHeader(authorizationHeader) ~> routes ~> check {
            rejection ==== MissingQueryParamRejection("catalog_id")
          }
        }
      }
    }

    "if request has valid app token" should {
      "return the product" in new CatalogCategoriesListFSpecContext with AppTokenFixtures {
        val catalogCategory1 =
          Factory.catalogCategory(catalog, name = Some("A catalog category"), position = Some(1)).create
        val catalogCategory2 =
          Factory.catalogCategory(catalog, name = Some("B catalog category"), position = Some(2)).create
        val catalogCategory3 =
          Factory.catalogCategory(catalog, name = Some("B catalog category"), position = Some(1)).create
        val catalogCategory4 = Factory
          .catalogCategory(catalog, name = Some("Other location catalog category"), position = Some(3))
          .create

        Get(s"/v1/catalog_categories.list?catalog_id=${catalog.id}")
          .addHeader(appAuthorizationHeader) ~> routes ~> check {
          assertStatusOK()
          val catalogCategories = responseAs[PaginatedApiResponse[Seq[Category]]]
          catalogCategories.data.map(_.id) ==== Seq(
            catalogCategory1.id,
            catalogCategory3.id,
            catalogCategory2.id,
            catalogCategory4.id,
          )
        }
      }
    }

    "if request has invalid token" should {
      "reject request" in new CatalogCategoriesListFSpecContext {
        Get(s"/v1/catalog_categories.list?catalog_id=${catalog.id}")
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
