package io.paytouch.core.resources.catalogs

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection

import io.paytouch.core.entities._

class CatalogsCreateFSpec extends CatalogsFSpec {

  abstract class CatalogsCreateFSpecContext extends CatalogResourceFSpecContext

  "POST /v1/catalogs.create?catalog_id=$" in {
    "if request has valid token" in {
      "create catalog and return 201" in new CatalogsCreateFSpecContext {
        val newCatalogId = UUID.randomUUID

        val creation = random[CatalogCreation].copy(availabilities = Some(buildAvailability))

        Post(s"/v1/catalogs.create?catalog_id=$newCatalogId", creation)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()

          val catalog = responseAs[ApiResponse[Catalog]].data
          assertCreation(catalog.id, creation)
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new CatalogsCreateFSpecContext {
        val newCatalogId = UUID.randomUUID
        val creation = random[CatalogCreation]
        Post(s"/v1/catalogs.create?catalog_id=$newCatalogId", creation)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
