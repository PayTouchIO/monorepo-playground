package io.paytouch.core.resources.catalogs

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ AppTokenFixtures, FixtureDaoFactory => Factory }

class CatalogsGetFSpec extends CatalogsFSpec {

  abstract class CatalogsGetFSpecContext extends CatalogResourceFSpecContext {
    val catalog = Factory.catalog(merchant).create
    val catalogCategory1 = Factory.catalogCategory(catalog, id = Some(UUID.randomUUID)).create
    val catalogCategory2 = Factory.catalogCategory(catalog, id = Some(UUID.randomUUID)).create
  }

  "GET /v1/catalogs.get?catalog_id=$" in {
    "if request has valid token" in {

      "if the catalog exists" should {

        "with no parameters" should {
          "return the catalog" in new CatalogsGetFSpecContext {
            Get(s"/v1/catalogs.get?catalog_id=${catalog.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val entity = responseAs[ApiResponse[Catalog]].data
              assertResponse(entity, catalog)
            }
          }
        }

        "with expand[]=availabilities" should {
          "if type=menu" should {
            "return the catalog with expanded availabilities" in new CatalogsGetFSpecContext {
              val catalogAvailability =
                Factory.catalogAvailability(catalog, Seq(Weekdays.Monday, Weekdays.Tuesday)).create
              val expectedAvailabilityMap = Map(
                Weekdays.Monday -> Seq(Availability(catalogAvailability.start, catalogAvailability.end)),
                Weekdays.Tuesday -> Seq(Availability(catalogAvailability.start, catalogAvailability.end)),
              )

              Get(s"/v1/catalogs.get?catalog_id=${catalog.id}&expand[]=availabilities")
                .addHeader(authorizationHeader) ~> routes ~> check {

                assertStatusOK()

                val entity = responseAs[ApiResponse[Catalog]].data
                assertResponse(entity, catalog)
                entity.availabilities ==== Some(expectedAvailabilityMap)
              }
            }
          }

          "if type=default_menu" should {
            "return the catalog ignoring the availabilities one set on catalog" in new CatalogsGetFSpecContext {
              val locationAvailability = Factory.locationAvailability(london, Seq(Weekdays.Monday)).create
              val expectedAvailabilityMap = Map(
                Weekdays.Monday -> Seq(Availability(locationAvailability.start, locationAvailability.end)),
              )

              // This shouldn't happen anyway in production, but leaving here for extra checks.
              val catalogAvailability =
                Factory.catalogAvailability(defaultMenuCatalog, Seq(Weekdays.Monday, Weekdays.Tuesday)).create

              val expectedLocationOverrides = Map(
                london.id -> expectedAvailabilityMap,
              )

              Get(s"/v1/catalogs.get?catalog_id=${defaultMenuCatalog.id}&expand[]=availabilities")
                .addHeader(authorizationHeader) ~> routes ~> check {

                assertStatusOK()

                val entity = responseAs[ApiResponse[Catalog]].data
                assertResponse(entity, defaultMenuCatalog)
                entity.availabilities ==== Some(Map.empty)
              }
            }
          }
        }

        "with expand[]=location_overrides" should {
          "if type=menu" should {
            "return the catalog with expanded location_overrides denormalizing availabilities from catalog to locations" in new CatalogsGetFSpecContext {
              val catalogAvailability =
                Factory.catalogAvailability(catalog, Seq(Weekdays.Monday, Weekdays.Tuesday)).create
              val expectedAvailabilityMap = Map(
                Weekdays.Monday -> Seq(Availability(catalogAvailability.start, catalogAvailability.end)),
                Weekdays.Tuesday -> Seq(Availability(catalogAvailability.start, catalogAvailability.end)),
              )
              val expectedLocationOverrides = Map(
                london.id -> CatalogLocation(availabilities = expectedAvailabilityMap),
              )

              Get(s"/v1/catalogs.get?catalog_id=${catalog.id}&expand[]=location_overrides")
                .addHeader(authorizationHeader) ~> routes ~> check {

                assertStatusOK()

                val entity = responseAs[ApiResponse[Catalog]].data
                assertResponse(entity, catalog)
                entity.locationOverrides ==== Some(expectedLocationOverrides)
              }
            }
          }

          "if type=default_menu" should {
            "return the catalog ignoring the location_overrides set on catalog and copying over the ones from location" in new CatalogsGetFSpecContext {
              val locationAvailability = Factory.locationAvailability(london, Seq(Weekdays.Monday)).create
              val expectedAvailabilityMap = Map(
                Weekdays.Monday -> Seq(Availability(locationAvailability.start, locationAvailability.end)),
              )

              // This shouldn't happen anyway in production, but leaving here for extra checks.
              val catalogAvailability =
                Factory.catalogAvailability(defaultMenuCatalog, Seq(Weekdays.Monday, Weekdays.Tuesday)).create

              val expectedLocationOverrides = Map(
                london.id -> CatalogLocation(availabilities = expectedAvailabilityMap),
              )

              Get(s"/v1/catalogs.get?catalog_id=${defaultMenuCatalog.id}&expand[]=location_overrides")
                .addHeader(authorizationHeader) ~> routes ~> check {

                assertStatusOK()

                val entity = responseAs[ApiResponse[Catalog]].data
                assertResponse(entity, defaultMenuCatalog)
                entity.locationOverrides ==== Some(expectedLocationOverrides)
              }
            }
          }
        }

        "with expand[]=categories_count" should {
          "return the catalog with expanded categories_count" in new CatalogsGetFSpecContext {

            Get(s"/v1/catalogs.get?catalog_id=${catalog.id}&expand[]=categories_count")
              .addHeader(authorizationHeader) ~> routes ~> check {

              assertStatusOK()

              val entity = responseAs[ApiResponse[Catalog]].data
              assertResponse(entity, catalog, categoriesCount = Some(2))
            }
          }
        }

        "with expand[]=products_count" should {
          "return the catalog with expanded products_count" in new CatalogsGetFSpecContext {
            val simpleProduct = Factory.simpleProduct(merchant).create
            val productCategory = Factory.productCategory(simpleProduct, catalogCategory1).create

            val simpleProduct2 = Factory.simpleProduct(merchant).create
            val productCategory2 = Factory.productCategory(simpleProduct2, catalogCategory2).create

            Get(s"/v1/catalogs.get?catalog_id=${catalog.id}&expand[]=products_count")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val entity = responseAs[ApiResponse[Catalog]].data
              assertResponse(entity, catalog, Some(2))
            }
          }
        }

      }

      "if the catalog does not belong to the merchant" should {
        "return 404" in new CatalogsGetFSpecContext {
          val competitor = Factory.merchant.create
          val catalogCompetitor = Factory.catalog(competitor).create

          Get(s"/v1/catalogs.get?catalog_id=${catalogCompetitor.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if the catalog does not exist" should {
        "return 404" in new CatalogsGetFSpecContext {
          Get(s"/v1/catalogs.get?catalog_id=${UUID.randomUUID}").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }

    "if request has valid app token" should {
      "return the catalog" in new CatalogsGetFSpecContext with AppTokenFixtures {
        Get(s"/v1/catalogs.get?catalog_id=${catalog.id}").addHeader(appAuthorizationHeader) ~> routes ~> check {
          assertStatusOK()
          val entity = responseAs[ApiResponse[Catalog]].data

          assertResponse(entity, catalog)
        }
      }
    }

    "if request has invalid token" should {
      "reject request" in new CatalogsGetFSpecContext {
        val record = Factory.catalog(merchant).create

        Get(s"/v1/catalogs.get?catalog_id=${record.id}").addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
