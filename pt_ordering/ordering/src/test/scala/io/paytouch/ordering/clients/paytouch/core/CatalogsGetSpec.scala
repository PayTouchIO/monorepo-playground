package io.paytouch.ordering.clients.paytouch.core

import java.time.LocalTime
import java.util.UUID

import akka.http.scaladsl.model._

import cats.implicits._

import io.paytouch.implicits._

import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.clients.paytouch.core.entities.enums.Day
import io.paytouch.ordering.errors.ClientError

class CatalogsGetSpec extends PtCoreClientSpec {
  abstract class CatalogsGetSpecContext extends CoreClientSpecContext {
    val id: UUID = "60f0b330-2116-4d50-8e55-fab0ddc4e7b0"

    val params = s"catalog_id=$id&expand[]=products_count,location_overrides"

    def assertCatalogsGet(implicit request: HttpRequest, authToken: HttpHeader) =
      assertUserAuthRequest(HttpMethods.GET, "/v1/catalogs.get", authToken, queryParams = Some(params))
  }

  "CoreClient" should {
    "call catalogs.get" should {
      "parse a catalog" in new CatalogsGetSpecContext with CatalogFixture {
        val response = when(catalogsGet(id))
          .expectRequest(implicit request => assertCatalogsGet)
          .respondWith(catalogFileName)
        response.await.map(_.data) ==== Right(expectedCatalog)
      }

      "parse rejection" in new CatalogsGetSpecContext {
        val endpoint =
          completeUri(s"/v1/catalogs.get?$params")
        val expectedRejection = ClientError(endpoint, "The supplied authentication is invalid")
        val response = when(catalogsGet(id))
          .expectRequest(implicit request => assertCatalogsGet)
          .respondWithRejection("/core/responses/auth_rejection.json")
        response.await ==== Left(expectedRejection)
      }
    }
  }

  trait CatalogFixture { self: CatalogsGetSpecContext =>
    val catalogFileName = "/core/responses/catalogs_get.json"

    val locationId: UUID =
      "4a886481-d516-4f4c-b54d-1e534468b22d"

    val expectedLocationOverrides: Map[UUID, CatalogLocation] =
      Map(
        locationId ->
          CatalogLocation(
            Day
              .values
              .map(_ -> Seq(Availability(start = LocalTime.NOON, end = LocalTime.MIDNIGHT)))
              .toMap,
          ),
      )

    val expectedCatalog: Catalog =
      Catalog(
        id = id,
        name = "Basico",
        productsCount = 42.some,
        locationOverrides = expectedLocationOverrides.some,
      )
  }
}
