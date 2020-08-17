package io.paytouch.core.resources.catalogcategories

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class CatalogCategoriesOrderingFSpec extends CatalogCategoriesFSpec {

  lazy val catalogCategoryDao = daos.catalogCategoryDao

  "POST /v1/catalog_categories.update_ordering" in {
    "if request has valid token" in {
      "if all ids are valid" should {
        "return 204" in new CatalogCategoryResourceFSpecContext {
          val catalogCategory = Factory.catalogCategory(catalog).create
          val ordering = Seq(EntityOrdering(catalogCategory.id, 7))

          Post(s"/v1/catalog_categories.update_ordering", ordering).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            val entityOrdering = ordering.head
            val updatedCatalogCategory = catalogCategoryDao.findById(entityOrdering.id).await.get
            updatedCatalogCategory.position ==== entityOrdering.position
            catalogCategory.updatedAt !=== updatedCatalogCategory.updatedAt
          }
        }
      }
      "if an id is invalid" should {
        "return 400" in new CatalogCategoryResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorCatalog = Factory.catalog(competitor).create
          val competitorCatalogCategory = Factory.catalogCategory(competitorCatalog).create
          val ordering = Seq(EntityOrdering(competitorCatalogCategory.id, 7))

          Post(s"/v1/catalog_categories.update_ordering", ordering).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new CatalogCategoryResourceFSpecContext {
        val ordering = Seq(random[EntityOrdering])
        Post(s"/v1/catalog_categories.update_ordering", ordering)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
