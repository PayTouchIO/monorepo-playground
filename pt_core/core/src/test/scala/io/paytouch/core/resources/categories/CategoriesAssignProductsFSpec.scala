package io.paytouch.core.resources.categories

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ UtcTime, FixtureDaoFactory => Factory }

class CategoriesAssignProductsFSpec extends CategoriesFSpec {

  "POST /v1/categories.assign_products?category_id=<category-id>" in {

    "if request has valid token" in {

      "if category belongs to same merchant" should {

        "patch the product" in new CategoryResourceFSpecContext {
          val yesterday = UtcTime.now.minusDays(1)
          val category = Factory.systemCategory(defaultMenuCatalog, overrideNow = Some(yesterday)).create

          val product0 = Factory.simpleProduct(merchant, overrideNow = Some(yesterday)).create
          Factory.productCategory(product0, category).create

          val product1 = Factory.simpleProduct(merchant, overrideNow = Some(yesterday)).create
          val product2 = Factory.simpleProduct(merchant, overrideNow = Some(yesterday)).create
          val product3 = Factory.simpleProduct(merchant, overrideNow = Some(yesterday)).create
          val productIds = Seq(product1.id, product2.id, product3.id)
          val categoryAssignment = ProductsAssignment(productIds = productIds)

          Post(s"/v1/categories.assign_products?category_id=${category.id}", categoryAssignment)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            productCategoryDao.findByCategoryId(category.id).await.map(_.productId).toSet ==== productIds.toSet

            val allProductIds = Seq(product0.id, product1.id, product2.id, product3.id)
            val updatedProducts = articleDao.findByIds(allProductIds).await
            product0.updatedAt !=== updatedProducts.find(_.id == product0.id).get.updatedAt
            product1.updatedAt !=== updatedProducts.find(_.id == product1.id).get.updatedAt
            product2.updatedAt !=== updatedProducts.find(_.id == product2.id).get.updatedAt
            product3.updatedAt !=== updatedProducts.find(_.id == product3.id).get.updatedAt

            val updatedCategory = categoryDao.findById(category.id).await.get
            category.updatedAt !=== updatedCategory.updatedAt

          }
        }
      }

      "if one of the products is not a main product" should {

        "return 404" in new CategoryResourceFSpecContext {
          val category = Factory.systemCategory(defaultMenuCatalog).create

          val product0 = Factory.simpleProduct(merchant).create
          Factory.productCategory(product0, category).create

          val simple = Factory.simpleProduct(merchant).create
          val template = Factory.templateProduct(merchant).create
          val variant = Factory.variantProduct(merchant, template).create
          val productIds = Seq(simple.id, template.id, variant.id)
          val categoryAssignment = ProductsAssignment(productIds = productIds)

          Post(s"/v1/categories.assign_products?category_id=${category.id}", categoryAssignment)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if any product id in the patch doesn't belong to same merchant" should {

        "return 404" in new CategoryResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorDefaultMenuCatalog =
            Factory.defaultMenuCatalog(competitor).create
          val category = Factory.systemCategory(competitorDefaultMenuCatalog).create
          val competitorProduct = Factory.simpleProduct(competitor).create
          val product = Factory.simpleProduct(merchant).create

          val productIds = Seq(competitorProduct.id, product.id)
          val categoryAssignment = ProductsAssignment(productIds = productIds)

          Post(s"/v1/categories.assign_products?category_id=${category.id}", categoryAssignment)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if category doesn't belong to same merchant" should {

        "return 404" in new CategoryResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorDefaultMenuCatalog =
            Factory.defaultMenuCatalog(competitor).create
          val competitorCategory = Factory.systemCategory(competitorDefaultMenuCatalog).create

          Post(s"/v1/categories.assign_products?category_id=${competitorCategory.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }

    "if request has invalid token" should {

      "be rejected" in new CategoryResourceFSpecContext {
        val categoryId = UUID.randomUUID
        val categoryAssignment = random[ProductsAssignment]
        Post(s"/v1/categories.assign_products?category_id=$categoryId", categoryAssignment)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
