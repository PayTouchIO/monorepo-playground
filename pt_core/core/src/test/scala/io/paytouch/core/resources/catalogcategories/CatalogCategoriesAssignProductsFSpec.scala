package io.paytouch.core.resources.catalogcategories

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.data.model
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ UtcTime, FixtureDaoFactory => Factory }

class CatalogCategoriesAssignProductsFSpec extends CatalogCategoriesFSpec {

  class CatalogCategoriesAssignProductsFSpecContext extends CatalogCategoryResourceFSpecContext {

    def assertAssignments(catalogCategory: model.CategoryRecord, assignments: Seq[CatalogCategoryProductAssignment]) = {
      val productCategories = productCategoryDao.findByCategoryId(catalogCategory.id).await
      val productCategoryOptions =
        productCategoryOptionDao.findByProductCategoryIds(productCategories.map(_.id)).await
      productCategories.size ==== 3
      productCategoryOptions.size ==== 3

      assignments.map { assignment =>
        val maybeProductCategory = productCategories.find(_.productId == assignment.productId)
        maybeProductCategory must beSome
        val productCategory = maybeProductCategory.get

        val maybeProductCategoryOption = productCategoryOptions.find(_.productCategoryId == productCategory.id)
        maybeProductCategoryOption must beSome
        val productCategoryOption = maybeProductCategoryOption.get

        productCategoryOption.deliveryEnabled ==== assignment.deliveryEnabled
        productCategoryOption.takeAwayEnabled ==== assignment.takeAwayEnabled
      }
    }

    def assertProductUpdted(productRecord: model.ArticleRecord) = {
      val updatedProduct = articleDao.findById(productRecord.id).await
      productRecord.updatedAt !=== updatedProduct.get.updatedAt
    }

    def assertCategoryUpdated(categoryRecord: model.CategoryRecord) = {
      val updatedCatalogCategory = categoryDao.findById(categoryRecord.id).await.get
      categoryRecord.updatedAt !=== updatedCatalogCategory.updatedAt
    }
  }

  "POST /v1/catalog_categories.assign_products?catalog_category_id=<catalog-category-id>" in {

    "if request has valid token" in {

      "if catalog category belongs to same merchant" should {

        "change category assignments" in new CatalogCategoriesAssignProductsFSpecContext {
          val catalogCategory = Factory.catalogCategory(catalog).create

          val product0 = Factory.simpleProduct(merchant).create
          val productCategory = Factory.productCategory(product0, catalogCategory).create
          Factory.productCategoryOption(productCategory).create

          val product1 = Factory.simpleProduct(merchant).create
          val product2 = Factory.simpleProduct(merchant).create
          val product3 = Factory.simpleProduct(merchant).create
          val productIds = Seq(product1.id, product2.id, product3.id)
          val catalogCategoryAssignments = Seq(
            CatalogCategoryProductAssignment(product1.id, deliveryEnabled = true, takeAwayEnabled = false),
            CatalogCategoryProductAssignment(product2.id, deliveryEnabled = false, takeAwayEnabled = false),
            CatalogCategoryProductAssignment(product3.id, deliveryEnabled = true, takeAwayEnabled = true),
          )

          Post(
            s"/v1/catalog_categories.assign_products?catalog_category_id=${catalogCategory.id}",
            catalogCategoryAssignments,
          ).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            assertAssignments(catalogCategory, catalogCategoryAssignments)
            assertProductUpdted(product0)
            assertProductUpdted(product1)
            assertProductUpdted(product2)
            assertProductUpdted(product3)

            assertCategoryUpdated(catalogCategory)
          }
        }
      }

      "if default menu category belongs to same merchant" should {

        "change category assignments" in new CatalogCategoriesAssignProductsFSpecContext {
          val catalogCategory = Factory.catalogCategory(defaultMenuCatalog).create

          val product0 = Factory.simpleProduct(merchant).create
          val productCategory = Factory.productCategory(product0, catalogCategory).create
          Factory.productCategoryOption(productCategory).create

          val product1 = Factory.simpleProduct(merchant).create
          val product2 = Factory.simpleProduct(merchant).create
          val product3 = Factory.simpleProduct(merchant).create
          val productIds = Seq(product1.id, product2.id, product3.id)
          val catalogCategoryAssignments = Seq(
            CatalogCategoryProductAssignment(product1.id, deliveryEnabled = true, takeAwayEnabled = false),
            CatalogCategoryProductAssignment(product2.id, deliveryEnabled = false, takeAwayEnabled = false),
            CatalogCategoryProductAssignment(product3.id, deliveryEnabled = true, takeAwayEnabled = true),
          )

          Post(
            s"/v1/catalog_categories.assign_products?catalog_category_id=${catalogCategory.id}",
            catalogCategoryAssignments,
          ).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            assertAssignments(catalogCategory, catalogCategoryAssignments)

            assertProductUpdted(product0)
            assertProductUpdted(product1)
            assertProductUpdted(product2)
            assertProductUpdted(product3)

            assertCategoryUpdated(catalogCategory)
          }
        }
      }

      "if one of the products is not a main product" should {

        "return 404" in new CatalogCategoriesAssignProductsFSpecContext {
          val catalogCategory = Factory.catalogCategory(catalog).create

          val product0 = Factory.simpleProduct(merchant).create
          Factory.productCategory(product0, catalogCategory).create

          val simple = Factory.simpleProduct(merchant).create
          val template = Factory.templateProduct(merchant).create
          val variant = Factory.variantProduct(merchant, template).create
          val productIds = Seq(simple.id, template.id, variant.id)
          val catalogCategoryAssignments = Seq(
            CatalogCategoryProductAssignment(simple.id, deliveryEnabled = true, takeAwayEnabled = false),
            CatalogCategoryProductAssignment(template.id, deliveryEnabled = false, takeAwayEnabled = false),
            CatalogCategoryProductAssignment(variant.id, deliveryEnabled = true, takeAwayEnabled = true),
          )

          Post(
            s"/v1/catalog_categories.assign_products?catalog_category_id=${catalogCategory.id}",
            catalogCategoryAssignments,
          ).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
            assertErrorCode("NonAccessibleMainProductIds")
          }
        }
      }

      "if any product id in the patch doesn't belong to same merchant" should {

        "return 404" in new CatalogCategoriesAssignProductsFSpecContext {
          val competitor = Factory.merchant.create

          val catalogCategory = Factory.catalogCategory(catalog).create
          val competitorProduct = Factory.simpleProduct(competitor).create
          val product = Factory.simpleProduct(merchant).create

          val productIds = Seq(competitorProduct.id, product.id)
          val catalogCategoryAssignments = Seq(
            CatalogCategoryProductAssignment(competitorProduct.id, deliveryEnabled = true, takeAwayEnabled = false),
            CatalogCategoryProductAssignment(product.id, deliveryEnabled = false, takeAwayEnabled = false),
          )

          Post(
            s"/v1/catalog_categories.assign_products?catalog_category_id=${catalogCategory.id}",
            catalogCategoryAssignments,
          ).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
            assertErrorCode("NonAccessibleMainProductIds")
          }
        }
      }

      "if catalog category doesn't belong to same merchant" should {

        "return 404" in new CatalogCategoriesAssignProductsFSpecContext {
          val competitor = Factory.merchant.create
          val competitorCatalog = Factory.catalog(competitor).create
          val competitorCatalogCategory = Factory.catalogCategory(competitorCatalog).create
          val catalogCategoryAssignments = Seq(random[CatalogCategoryProductAssignment])

          Post(
            s"/v1/catalog_categories.assign_products?catalog_category_id=${competitorCatalogCategory.id}",
            catalogCategoryAssignments,
          ).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }

    "if request has invalid token" should {

      "be rejected" in new CatalogCategoriesAssignProductsFSpecContext {
        val catalogCategoryId = UUID.randomUUID
        val catalogCategoryAssignments = Seq(random[CatalogCategoryProductAssignment])
        Post(
          s"/v1/catalog_categories.assign_products?catalog_category_id=$catalogCategoryId",
          catalogCategoryAssignments,
        ).addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
