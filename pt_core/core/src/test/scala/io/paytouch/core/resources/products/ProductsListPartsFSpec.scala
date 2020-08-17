package io.paytouch.core.resources.products

import java.util.UUID

import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.data.model.ProductPartRecord
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class ProductsListPartsFSpec extends ProductsFSpec {

  abstract class ProductsAddPartsFSpecContext extends ProductResourceFSpecContext {
    val productPartDao = daos.productPartDao

    def assertResponse(
        productId: UUID,
        entity: ProductPart,
        record: ProductPartRecord,
      ) = {
      productId ==== record.productId
      entity.part.id ==== record.partId
      entity.quantityNeeded ==== record.quantityNeeded
    }
  }

  "GET /v1/products.list_parts?product_id=<product-id>" in {

    "if request has valid token" in {

      "if product belongs to same merchant" in {

        "return parts of a product" in new ProductsAddPartsFSpecContext {
          val product = Factory.simpleProduct(merchant).create

          val part1 = Factory.simplePart(merchant).create
          val productPart1 = Factory.productPart(product, part1).create

          val part2 = Factory.simplePart(merchant).create
          val productPart2 = Factory.productPart(product, part2).create

          val part3 = Factory.simplePart(merchant).create
          val productPart3 = Factory.productPart(product, part3).create

          val part4 = Factory.simplePart(merchant).create
          val productPart4 = Factory.productPart(product, part4).create

          Get(s"/v1/products.list_parts?product_id=${product.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val productParts = responseAs[PaginatedApiResponse[Seq[ProductPart]]].data
            productParts.map(_.part.id) should containTheSameElementsAs(Seq(part1.id, part2.id, part3.id, part4.id))

            assertResponse(product.id, productParts.find(_.part.id == part1.id).get, productPart1)
            assertResponse(product.id, productParts.find(_.part.id == part2.id).get, productPart2)
            assertResponse(product.id, productParts.find(_.part.id == part3.id).get, productPart3)
            assertResponse(product.id, productParts.find(_.part.id == part4.id).get, productPart4)
          }
        }
      }
    }

    "if request has invalid token" should {

      "be rejected" in new ProductsAddPartsFSpecContext {
        val productId = UUID.randomUUID
        Get(s"/v1/products.list_parts?product_id=$productId")
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
