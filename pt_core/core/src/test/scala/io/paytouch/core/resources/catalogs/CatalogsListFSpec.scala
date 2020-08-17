package io.paytouch.core.resources.catalogs

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes

import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class CatalogsListFSpec extends CatalogsFSpec {
  abstract class CatalogsListFSpecContext extends CatalogResourceFSpecContext

  "GET /v1/catalogs.list" in {
    "if request has valid token" should {
      "return a paginated list of catalogs" in new CatalogResourceFSpecContext {
        val catalog1 = Factory.catalog(merchant, name = Some("Alphabetically")).create
        val catalog2 = Factory.catalog(merchant, name = Some("Ordered")).create
        val expectedIds = Seq(catalog1.id, defaultMenuCatalog.id, catalog2.id)

        Get("/v1/catalogs.list").addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val entities = responseAs[PaginatedApiResponse[Seq[Catalog]]].data
          entities.map(_.id) ==== expectedIds

          assertResponse(entities.find(_.id == defaultMenuCatalog.id).get, defaultMenuCatalog)
          assertResponse(entities.find(_.id == catalog1.id).get, catalog1)
          assertResponse(entities.find(_.id == catalog2.id).get, catalog2)
        }
      }

      "return a paginated list of catalogs filtered by id" in new CatalogResourceFSpecContext {
        val catalog1 = Factory.catalog(merchant, name = Some("Alphabetically")).create
        val catalog2 = Factory.catalog(merchant, name = Some("Ordered")).create
        val expectedIds = Seq(catalog1.id)

        Get(s"/v1/catalogs.list?id[]=${catalog1.id}").addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val entities = responseAs[PaginatedApiResponse[Seq[Catalog]]].data
          entities.map(_.id) ==== expectedIds

          assertResponse(entities.find(_.id == catalog1.id).get, catalog1)
        }
      }
    }

    "with expand[]=categories_count" should {
      "return a paginated list of catalogs with expanded categories_count" in new CatalogResourceFSpecContext {
        val record = Factory.catalog(merchant, name = Some("Alphabetically")).create
        val catalogCategory1 = Factory.catalogCategory(record, id = Some(UUID.randomUUID)).create
        val catalogCategory2 = Factory.catalogCategory(record, id = Some(UUID.randomUUID)).create

        Get("/v1/catalogs.list?expand[]=categories_count").addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val entities = responseAs[PaginatedApiResponse[Seq[Catalog]]].data
          assertResponse(entities.find(_.id == record.id).get, record, categoriesCount = Some(2))
        }
      }
    }

    "with expand[]=products_count" should {
      "return a paginated list of catalogs with expanded products_count" in new CatalogResourceFSpecContext {
        val record = Factory.catalog(merchant, name = Some("Alphabetically")).create

        val simpleProduct = Factory.simpleProduct(merchant).create
        val catalogCategory = Factory.catalogCategory(record, id = Some(UUID.randomUUID)).create
        val productCategory = Factory.productCategory(simpleProduct, catalogCategory).create

        val simpleProduct2 = Factory.simpleProduct(merchant).create
        val catalogCategory2 = Factory.catalogCategory(record, id = Some(UUID.randomUUID)).create
        val productCategory2 = Factory.productCategory(simpleProduct2, catalogCategory2).create

        Get("/v1/catalogs.list?expand[]=products_count").addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val entities = responseAs[PaginatedApiResponse[Seq[Catalog]]].data
          assertResponse(entities.find(_.id == record.id).get, record, Some(2))
        }
      }
    }
  }
}
