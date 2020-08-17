package io.paytouch.core.resources.catalogs

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.Ids
import io.paytouch.core.stubs.PtOrderingStubData
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class CatalogsDeleteFSpec extends CatalogsFSpec {

  abstract class CatalogDeleteResourceFSpecContext extends CatalogResourceFSpecContext {
    val categoryDao = daos.categoryDao

    def assertCatalogDeleted(id: UUID) = {
      catalogDao.findById(id).await should beEmpty
      categoryDao.findByCatalogId(id).await ==== Seq.empty
    }

    def assertCatalogNotDeleted(id: UUID) =
      catalogDao.findById(id).await should beSome
  }

  "POST /v1/catalogs.delete" in {

    "if request has valid token" in {
      "if catalog doesn't exist" should {
        "do nothing and return 204" in new CatalogDeleteResourceFSpecContext {
          val nonExistinCatalogId = UUID.randomUUID

          Post(s"/v1/catalogs.delete", Ids(ids = Seq(nonExistinCatalogId)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)
            assertCatalogDeleted(nonExistinCatalogId)
          }
        }
      }

      "if catalog belongs to the merchant" should {
        "delete the catalog not in use and its relations" in new CatalogDeleteResourceFSpecContext {
          val catalog = Factory.catalog(merchant).create
          Factory.catalogCategory(catalog).create

          Post(s"/v1/catalogs.delete", Ids(ids = Seq(catalog.id)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            assertCatalogDeleted(catalog.id)
          }
        }

        "reject request if the catalog is in use" in new CatalogDeleteResourceFSpecContext {
          val catalog = Factory.catalog(merchant).create
          PtOrderingStubData.recordCatalogIds(Seq(catalog.id), merchant)

          val competitor = Factory.merchant.create
          val competitorCatalog = Factory.catalog(competitor).create
          PtOrderingStubData.recordCatalogIds(Seq(competitorCatalog.id), competitor)

          Post(s"/v1/catalogs.delete", Ids(ids = Seq(catalog.id)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)

            assertErrorCode("DeletionCatalogsInUse")

            assertCatalogNotDeleted(catalog.id)
            assertCatalogNotDeleted(competitorCatalog.id)
          }
        }

        "reject request if the catalog is default menu" in new CatalogDeleteResourceFSpecContext {
          Post(s"/v1/catalogs.delete", Ids(ids = Seq(defaultMenuCatalog.id)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)

            assertErrorCode("DefaultMenuDeletionIsNotAllowed")

            assertCatalogNotDeleted(defaultMenuCatalog.id)
          }
        }
      }

      "if catalog belongs to a different merchant" should {
        "do not delete the catalog and return 204" in new CatalogDeleteResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorCatalog = Factory.catalog(competitor).create

          PtOrderingStubData.recordCatalogIds(Seq(competitorCatalog.id), competitor)

          Post(s"/v1/catalogs.delete", Ids(ids = Seq(competitorCatalog.id)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            assertCatalogNotDeleted(competitorCatalog.id)
          }
        }
      }
    }
  }
}
