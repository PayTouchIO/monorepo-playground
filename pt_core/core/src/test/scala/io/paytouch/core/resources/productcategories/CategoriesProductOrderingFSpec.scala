package io.paytouch.core.resources.productcategories

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ UtcTime, FixtureDaoFactory => Factory }

class CategoriesProductOrderingFSpec extends ProductCategoriesFSpec {

  lazy val categoryDao = daos.categoryDao

  "POST /v1/categories.update_products_ordering" in {
    "if request has valid token" in {
      "if all ids are valid" should {
        "return 204" in new ProductCategoryResourceFSpecContext {
          val yesterday = UtcTime.now.minusDays(1)

          val category = Factory.systemCategory(defaultMenuCatalog, overrideNow = Some(yesterday)).create
          val product = Factory.simpleProduct(merchant, overrideNow = Some(yesterday)).create
          val productCategory = Factory.productCategory(product, category, position = Some(2)).create

          val ordering = Seq(EntityOrdering(product.id, 7))

          Post(s"/v1/categories.update_products_ordering?category_id=${category.id}", ordering)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            val updatedCategory = categoryDao.findById(category.id).await.get
            category.updatedAt !=== updatedCategory.updatedAt

            val entityOrdering = ordering.head
            val updatedProductCategory = productCategoryDao.findById(productCategory.id).await.get
            updatedProductCategory.position ==== entityOrdering.position
            val updatedProduct = articleDao.findById(entityOrdering.id).await.get
            product.updatedAt !=== updatedProduct.updatedAt
          }
        }
      }
      "if the category id is invalid" should {
        "return 400" in new ProductCategoryResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorDefaultMenuCatalog = Factory.defaultMenuCatalog(competitor).create
          val competitorCategory = Factory.systemCategory(competitorDefaultMenuCatalog).create
          val product = Factory.simpleProduct(merchant).create
          val ordering = Seq(EntityOrdering(product.id, 7))

          Post(s"/v1/categories.update_products_ordering?category_id=${competitorCategory.id}", ordering)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if any product id is invalid" should {
        "return 400" in new ProductCategoryResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorDefaultMenuCatalog = Factory.defaultMenuCatalog(competitor).create
          val category = Factory.systemCategory(competitorDefaultMenuCatalog).create
          val competitorProduct = Factory.simpleProduct(competitor).create
          val ordering = Seq(EntityOrdering(competitorProduct.id, 7))

          Post(s"/v1/categories.update_products_ordering?category_id=${category.id}", ordering)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new ProductCategoryResourceFSpecContext {
        val category = Factory.systemCategory(defaultMenuCatalog).create
        val ordering = Seq(random[EntityOrdering])
        Post(s"/v1/categories.update_products_ordering?category_id=${category.id}", ordering)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
