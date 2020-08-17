package io.paytouch.core.resources.products

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, _ }

class ProductsDeleteFSpec extends FSpec {

  lazy val productDao = daos.productDao
  lazy val variantProductDao = daos.variantProductDao

  abstract class ProductResourceFSpecContext extends FSpecContext with MultipleLocationFixtures {

    def assertProductIsMarkedAsDeleted(id: UUID) = productDao.findDeletedById(id).await should beSome

    def assertProductDoesntExist(id: UUID) = {
      productDao.findDeletedById(id).await should beNone
      productDao.findById(id).await should beNone
    }

    def assertProductExists(id: UUID) = productDao.findById(id).await should beSome
  }

  "POST /v1/products.delete" in {

    "if request has valid token" in {
      "if product doesn't exist" should {
        "do nothing and return 204" in new ProductResourceFSpecContext {
          val nonExistingProductId = UUID.randomUUID

          Post(s"/v1/products.delete", Ids(ids = Seq(nonExistingProductId)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)
            assertProductDoesntExist(nonExistingProductId)
          }
        }
      }

      "if product belongs to the merchant" should {
        "delete the product and return 204" in new ProductResourceFSpecContext {
          val product = Factory.simpleProduct(merchant).create

          Post(s"/v1/products.delete", Ids(ids = Seq(product.id))).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            assertProductIsMarkedAsDeleted(product.id)
          }
        }

        "delete products and its variants" in new ProductResourceFSpecContext {
          val simpleProduct = Factory.simpleProduct(merchant).create
          val templateProduct = Factory.templateProduct(merchant).create
          val variantProduct1 = Factory.variantProduct(merchant, templateProduct).create
          val variantProduct2 = Factory.variantProduct(merchant, templateProduct).create
          val variantProduct3 = Factory.variantProduct(merchant, templateProduct).create

          val productIds = Seq(simpleProduct.id, templateProduct.id)

          Post(s"/v1/products.delete", Ids(productIds)).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            productDao.findByIds(productIds).await.isEmpty should beTrue
            variantProductDao.findVariantByParentId(simpleProduct.id).await.isEmpty should beTrue
            variantProductDao.findVariantByParentId(templateProduct.id).await.isEmpty should beTrue
          }
        }
      }

      "if product belongs to a different merchant" should {
        "do not delete the product and return 204" in new ProductResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorProduct = Factory.simpleProduct(competitor).create

          Post(s"/v1/products.delete", Ids(ids = Seq(competitorProduct.id)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            assertProductExists(competitorProduct.id)
          }
        }
      }

    }

  }
}
