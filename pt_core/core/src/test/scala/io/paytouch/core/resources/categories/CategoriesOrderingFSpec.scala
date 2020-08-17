package io.paytouch.core.resources.categories

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class CategoriesOrderingFSpec extends CategoriesFSpec {

  lazy val categoryDao = daos.categoryDao

  "POST /v1/categories.update_ordering" in {
    "if request has valid token" in {
      "if all ids are valid" should {
        "return 204" in new CategoryResourceFSpecContext {
          val category = Factory.systemCategory(defaultMenuCatalog).create
          val ordering = Seq(EntityOrdering(category.id, 7))

          Post(s"/v1/categories.update_ordering", ordering).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            val entityOrdering = ordering.head
            val updatedCategory = categoryDao.findById(entityOrdering.id).await.get
            updatedCategory.position ==== entityOrdering.position
            category.updatedAt !=== updatedCategory.updatedAt
          }
        }
      }
      "if an id is invalid" should {
        "return 400" in new CategoryResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorDefaultMenuCatalog = Factory.defaultMenuCatalog(competitor).create
          val category = Factory.systemCategory(competitorDefaultMenuCatalog).create
          val ordering = Seq(EntityOrdering(category.id, 7))

          Post(s"/v1/categories.update_ordering", ordering).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new CategoryResourceFSpecContext {
        val ordering = Seq(random[EntityOrdering])
        Post(s"/v1/categories.update_ordering", ordering).addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
