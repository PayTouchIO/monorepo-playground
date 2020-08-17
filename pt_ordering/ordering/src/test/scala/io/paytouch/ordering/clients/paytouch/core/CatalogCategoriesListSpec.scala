package io.paytouch.ordering.clients.paytouch.core

import java.time.LocalTime
import java.util.UUID

import akka.http.scaladsl.model._

import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.clients.paytouch.core.entities.enums._
import io.paytouch.ordering.errors.ClientError

class CatalogCategoriesListSpec extends PtCoreClientSpec {
  abstract class CatalogCategoriesListSpecContext extends CoreClientSpecContext {
    val locationId: UUID = "0876e07d-3cbc-3916-b6e0-bda7cbe66112"
    val catalogId: UUID = "60f0b330-2116-4d50-8e55-fab0ddc4e7b0"

    val catalogCategoriesParams: String = params(s"catalog_id=$catalogId")

    def params(filters: String) = {
      val expansions = "expand[]=subcatalog_categories,locations,availabilities"
      val paging = "per_page=100"

      s"$filters&$expansions&$paging"
    }

    def assertCatalogCategoriesList(implicit request: HttpRequest, authToken: HttpHeader) =
      assertUserAuthRequest(
        HttpMethods.GET,
        "/v1/catalog_categories.list",
        authToken,
        queryParams = Some(catalogCategoriesParams),
      )
  }

  "CoreClient" should {
    "call catalog_categories.list" should {
      "parse a catalog" in new CatalogCategoriesListSpecContext with CatalogCategoryFixture {
        lazy val expectedCategories: Seq[Category] = Seq(category)
        val response = when(catalogCategoriesList(catalogId))
          .expectRequest(implicit request => assertCatalogCategoriesList)
          .respondWith(catalogCategoriesFileName)
        response.await.map(_.data) ==== Right(expectedCategories)
      }

      "parse rejection" in new CatalogCategoriesListSpecContext {
        val endpoint = completeUri(s"/v1/catalog_categories.list?$catalogCategoriesParams")
        val expectedRejection = ClientError(endpoint, "The supplied authentication is invalid")

        val response = when(catalogCategoriesList(catalogId))
          .expectRequest(implicit request => assertCatalogCategoriesList)
          .respondWithRejection("/core/responses/auth_rejection.json")
        response.await ==== Left(expectedRejection)
      }
    }
  }

  trait CatalogCategoryFixture { self: CatalogCategoriesListSpecContext =>
    val catalogCategoriesFileName = "/core/responses/catalog_categories_list.json"

    private val imageUrls = Seq(
      ImageUrls(
        imageUploadId = "152444e7-168f-4b1b-8e28-a33fc3f5f92a",
        urls = ImageSize.values.map(size => size -> s"http://my-image-url-${size.entryName}").toMap,
      ),
    )

    private val availabilities: Map[Day, Seq[Availability]] = Day
      .values
      .map(day => day -> Seq(Availability(start = LocalTime.NOON, end = LocalTime.MIDNIGHT)))
      .toMap

    private val locationOverrides: Map[UUID, CategoryLocation] = Map(
      locationId -> CategoryLocation(true, availabilities),
    )

    lazy val category: Category = Category(
      id = "80bd48e6-3dd8-41e9-bb36-4b69c9b0cb18",
      name = "Appetizers",
      catalogId = catalogId,
      merchantId = "152444e7-168f-4b1b-8e28-a33fc3f5f92a",
      description = Some("Nice description here"),
      avatarBgColor = Some("2b98f0"),
      avatarImageUrls = imageUrls,
      position = 0,
      active = Some(true),
      subcategories = Seq.empty,
      locationOverrides = Some(locationOverrides),
      availabilities = Some(availabilities),
    )
  }
}
