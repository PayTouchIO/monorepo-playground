package io.paytouch.core.resources.products

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.data.model.ProductPartRecord
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ UtcTime, FixtureDaoFactory => Factory }

class ProductsAddPartsFSpec extends ProductsFSpec {

  abstract class ProductsAddPartsFSpecContext extends ProductResourceFSpecContext {
    val productPartDao = daos.productPartDao

    def assertProductPart(
        productPart: ProductPartRecord,
        productId: UUID,
        assignment: ProductPartAssignment,
      ) = {
      productPart.productId ==== productId
      productPart.partId ==== assignment.partId
      productPart.quantityNeeded ==== assignment.quantityNeeded
    }
  }

  "POST /v1/products.add_parts?product_id=<product-id>" in {

    "if request has valid token" in {

      "if product belongs to same merchant" in {

        "if product has scope Product" should {

          "if not all the part ids have scope Part" should {
            "reject the request" in new ProductsAddPartsFSpecContext {
              val product = Factory.simpleProduct(merchant).create
              val anotherProduct = Factory.simpleProduct(merchant).create
              val part1 = Factory.simplePart(merchant).create

              val productAssignments = Seq(
                ProductPartAssignment(anotherProduct.id, quantityNeeded = genBigDecimal.instance),
                ProductPartAssignment(part1.id, quantityNeeded = genBigDecimal.instance),
              )

              Post(s"/v1/products.add_parts?product_id=${product.id}", productAssignments)
                .addHeader(authorizationHeader) ~> routes ~> check {
                assertStatus(StatusCodes.NotFound)
              }
            }
          }

          "if all the part ids have scope Part" should {

            "associate product and parts" in new ProductsAddPartsFSpecContext {
              val yesterday = UtcTime.now.minusDays(1)
              val product = Factory.simpleProduct(merchant, overrideNow = Some(yesterday)).create

              val oldPart = Factory.simplePart(merchant, overrideNow = Some(yesterday)).create
              val oldProductPart = Factory.productPart(product, oldPart).create

              val part1 = Factory.simplePart(merchant, overrideNow = Some(yesterday)).create
              val part2 = Factory.simplePart(merchant, overrideNow = Some(yesterday)).create
              val part3 = Factory.simplePart(merchant, overrideNow = Some(yesterday)).create
              val part4 = Factory.simplePart(merchant, overrideNow = Some(yesterday)).create

              val productAssignments = Seq(
                ProductPartAssignment(part1.id, quantityNeeded = genBigDecimal.instance),
                ProductPartAssignment(part2.id, quantityNeeded = genBigDecimal.instance),
                ProductPartAssignment(part3.id, quantityNeeded = genBigDecimal.instance),
                ProductPartAssignment(part4.id, quantityNeeded = genBigDecimal.instance),
              )

              Post(s"/v1/products.add_parts?product_id=${product.id}", productAssignments)
                .addHeader(authorizationHeader) ~> routes ~> check {
                assertStatus(StatusCodes.NoContent)

                afterAWhile {

                  val productParts = productPartDao.findByProductId(product.id).await

                  productParts.size ==== 4
                  productPartDao.findById(oldProductPart.id).await should beNone

                  assertProductPart(productParts.find(_.partId == part1.id).get, product.id, productAssignments.head)
                  assertProductPart(productParts.find(_.partId == part2.id).get, product.id, productAssignments(1))
                  assertProductPart(productParts.find(_.partId == part3.id).get, product.id, productAssignments(2))
                  assertProductPart(productParts.find(_.partId == part4.id).get, product.id, productAssignments(3))

                  val allArticleIds = Seq(product.id, oldPart.id, part1.id, part2.id, part3.id, part4.id)

                  val updatedProducts = articleDao.findByIds(allArticleIds).await
                  updatedProducts.find(_.id == product.id).get.hasParts ==== true

                  product.updatedAt !=== updatedProducts.find(_.id == product.id).get.updatedAt
                  oldPart.updatedAt !=== updatedProducts.find(_.id == oldPart.id).get.updatedAt
                  part1.updatedAt !=== updatedProducts.find(_.id == part1.id).get.updatedAt
                  part2.updatedAt !=== updatedProducts.find(_.id == part2.id).get.updatedAt
                  part3.updatedAt !=== updatedProducts.find(_.id == part3.id).get.updatedAt
                  part4.updatedAt !=== updatedProducts.find(_.id == part4.id).get.updatedAt
                }
              }
            }

            "delete product parts associations if assignments are empty" in new ProductsAddPartsFSpecContext {
              val yesterday = UtcTime.now.minusDays(1)
              val product = Factory.simpleProduct(merchant, overrideNow = Some(yesterday)).create

              val part = Factory.simplePart(merchant, overrideNow = Some(yesterday)).create
              val productPart = Factory.productPart(product, part).create

              val productAssignments = Seq()

              Post(s"/v1/products.add_parts?product_id=${product.id}", productAssignments)
                .addHeader(authorizationHeader) ~> routes ~> check {
                assertStatus(StatusCodes.NoContent)

                afterAWhile {

                  val productParts = productPartDao.findByProductId(product.id).await

                  productParts.size ==== 0
                  productPartDao.findById(productPart.id).await should beNone

                  val updatedProducts = articleDao.findByIds(Seq(product.id, part.id)).await

                  updatedProducts.find(_.id == product.id).get.hasParts ==== false
                  product.updatedAt !=== updatedProducts.find(_.id == product.id).get.updatedAt
                  part.updatedAt !=== updatedProducts.find(_.id == part.id).get.updatedAt
                }
              }
            }
          }
        }

        "if product has not scope Product" should {
          "if not all the part ids have scope Part" should {
            "reject the request" in new ProductsAddPartsFSpecContext {
              val comboPart = Factory.comboPart(merchant).create
              val anotherProduct = Factory.simpleProduct(merchant).create
              val part1 = Factory.simplePart(merchant).create

              val productAssignments = Seq(
                ProductPartAssignment(anotherProduct.id, quantityNeeded = genBigDecimal.instance),
                ProductPartAssignment(part1.id, quantityNeeded = genBigDecimal.instance),
              )

              Post(s"/v1/products.add_parts?product_id=${comboPart.id}", productAssignments)
                .addHeader(authorizationHeader) ~> routes ~> check {
                assertStatus(StatusCodes.NotFound)
              }
            }
          }

          "if all the part ids have scope Part" should {

            "associate products and parts" in new ProductsAddPartsFSpecContext {
              val comboPart = Factory.comboPart(merchant).create

              val oldPart = Factory.simplePart(merchant).create
              val oldProductPart = Factory.productPart(comboPart, oldPart).create

              val part1 = Factory.simplePart(merchant).create
              val part2 = Factory.simplePart(merchant).create
              val part3 = Factory.simplePart(merchant).create
              val part4 = Factory.simplePart(merchant).create

              val productAssignments = Seq(
                ProductPartAssignment(part1.id, quantityNeeded = genBigDecimal.instance),
                ProductPartAssignment(part2.id, quantityNeeded = genBigDecimal.instance),
                ProductPartAssignment(part3.id, quantityNeeded = genBigDecimal.instance),
                ProductPartAssignment(part4.id, quantityNeeded = genBigDecimal.instance),
              )

              Post(s"/v1/products.add_parts?product_id=${comboPart.id}", productAssignments)
                .addHeader(authorizationHeader) ~> routes ~> check {
                assertStatus(StatusCodes.NoContent)

                afterAWhile {

                  val productParts = productPartDao.findByProductId(comboPart.id).await

                  productParts.size ==== 4
                  productPartDao.findById(oldProductPart.id).await should beNone

                  assertProductPart(productParts.find(_.partId == part1.id).get, comboPart.id, productAssignments.head)
                  assertProductPart(productParts.find(_.partId == part2.id).get, comboPart.id, productAssignments(1))
                  assertProductPart(productParts.find(_.partId == part3.id).get, comboPart.id, productAssignments(2))
                  assertProductPart(productParts.find(_.partId == part4.id).get, comboPart.id, productAssignments(3))
                }
              }
            }
          }
        }

      }

      "if product doesn't belong to same merchant" should {

        "return 404" in new ProductsAddPartsFSpecContext {
          val competitor = Factory.merchant.create
          val competitorProduct = Factory.simpleProduct(competitor).create

          Post(s"/v1/products.add_parts?product_id=${competitorProduct.id}", Seq.empty)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }

    "if request has invalid token" should {

      "be rejected" in new ProductsAddPartsFSpecContext {
        val productId = UUID.randomUUID
        val productAssignments = random[ProductPartAssignment](2)
        Post(s"/v1/products.add_parts?product_id=$productId", productAssignments)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
