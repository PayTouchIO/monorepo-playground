package io.paytouch.core.resources.catalogs

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class CatalogsUpdateFSpec extends CatalogsFSpec {
  abstract class CatalogsUpdateFSpecContext extends CatalogResourceFSpecContext

  "POST /v1/catalogs.update?catalog_id=$" in {
    "if request has valid token" in {
      "if catalog belong to same merchant" should {
        "update catalog and return 200" in new CatalogsUpdateFSpecContext {
          val catalog = Factory.catalog(merchant).create
          val update = random[CatalogUpdate].copy(availabilities = Some(buildAvailability))

          Post(s"/v1/catalogs.update?catalog_id=${catalog.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertUpdate(catalog.id, update.asUpsertion)
          }
        }
      }
      "if catalog doesn't belong to current user's merchant" in {
        "not update catalog and return 404" in new CatalogsUpdateFSpecContext {
          val competitor = Factory.merchant.create
          val competitorCatalog = Factory.catalog(competitor).create

          val update = random[CatalogUpdate]

          Post(s"/v1/catalogs.update?catalog_id=${competitorCatalog.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)

            val updatedCatalog = catalogDao.findById(competitorCatalog.id).await.get
            updatedCatalog ==== competitorCatalog
          }
        }
      }
    }
    "if request has invalid token" should {
      "be rejected" in new CatalogsUpdateFSpecContext {
        val catalogId = UUID.randomUUID
        val update = random[CatalogUpdate]
        Post(s"/v1/catalogs.update?catalog_id=$catalogId", update)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
