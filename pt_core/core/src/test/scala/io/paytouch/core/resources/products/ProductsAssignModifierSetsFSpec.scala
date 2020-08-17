package io.paytouch.core.resources.products

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class ProductsAssignModifierSetsFSpec extends ProductsFSpec {

  abstract class ProductsAssignModifierSetsFSpecContext extends ProductResourceFSpecContext

  "POST /v1/products.assign_modifier_sets?product_id=<product-id>" in {

    "if request has valid token" in {

      "if product belongs to same merchant" should {

        "post the product with modifier sets (legacy version)" in new ProductsAssignModifierSetsFSpecContext {
          val product = Factory.simpleProduct(merchant).create

          val modifierSet0 = Factory.modifierSet(merchant).create
          Factory.modifierSetProduct(modifierSet0, product).create

          val modifierSet1 = Factory.modifierSet(merchant).create
          val modifierSet2 = Factory.modifierSet(merchant).create
          val modifierSet3 = Factory.modifierSet(merchant).create
          val modifierSetIds = Seq(modifierSet1.id, modifierSet2.id, modifierSet3.id)
          val productAssignment = ProductModifierSetsAssignment(modifierSetIds = Some(modifierSetIds))

          Post(s"/v1/products.assign_modifier_sets?product_id=${product.id}", productAssignment)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            modifierSetProductDao.findByProductId(product.id).await.map(_.modifierSetId).toSet ==== modifierSetIds.toSet

            val allModifierSetIds = Seq(modifierSet0.id, modifierSet1.id, modifierSet2.id, modifierSet3.id)
            val updatedModifierSets = modifierSetDao.findByIds(allModifierSetIds).await
            modifierSet0.updatedAt !=== updatedModifierSets.find(_.id == modifierSet0.id).get.updatedAt
            modifierSet1.updatedAt !=== updatedModifierSets.find(_.id == modifierSet1.id).get.updatedAt
            modifierSet2.updatedAt !=== updatedModifierSets.find(_.id == modifierSet2.id).get.updatedAt
            modifierSet3.updatedAt !=== updatedModifierSets.find(_.id == modifierSet3.id).get.updatedAt

            product.updatedAt !=== productDao.findById(product.id).await.get.updatedAt
          }
        }

        "post the product with modifier sets (entity ordering version)" in new ProductsAssignModifierSetsFSpecContext {
          val product = Factory.simpleProduct(merchant).create

          val modifierSet0 = Factory.modifierSet(merchant).create
          Factory.modifierSetProduct(modifierSet0, product).create

          val modifierSet1 = Factory.modifierSet(merchant).create
          val modifierSet2 = Factory.modifierSet(merchant).create
          val modifierSet3 = Factory.modifierSet(merchant).create

          val modifierOrderings = Seq(
            EntityOrdering(modifierSet1.id, 1),
            EntityOrdering(modifierSet2.id, 3),
            EntityOrdering(modifierSet3.id, 2),
          )

          val productAssignment = ProductModifierSetsAssignment(modifierSets = Some(modifierOrderings))

          Post(s"/v1/products.assign_modifier_sets?product_id=${product.id}", productAssignment)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            val allModifierSetIds = Seq(modifierSet0.id, modifierSet1.id, modifierSet2.id, modifierSet3.id)
            val updatedModifierSets = modifierSetDao.findByIds(allModifierSetIds).await
            modifierSet0.updatedAt !=== updatedModifierSets.find(_.id == modifierSet0.id).get.updatedAt
            modifierSet1.updatedAt !=== updatedModifierSets.find(_.id == modifierSet1.id).get.updatedAt
            modifierSet2.updatedAt !=== updatedModifierSets.find(_.id == modifierSet2.id).get.updatedAt
            modifierSet3.updatedAt !=== updatedModifierSets.find(_.id == modifierSet3.id).get.updatedAt

            product.updatedAt !=== productDao.findById(product.id).await.get.updatedAt

            val modifierSetProducts = modifierSetProductDao.findByProductId(product.id).await
            modifierSetProducts.find(_.modifierSetId == modifierSet0.id).getOrElse(None) ==== None
            modifierSetProducts.find(_.modifierSetId == modifierSet1.id).get.position ==== Some(1)
            modifierSetProducts.find(_.modifierSetId == modifierSet2.id).get.position ==== Some(3)
            modifierSetProducts.find(_.modifierSetId == modifierSet3.id).get.position ==== Some(2)
          }
        }
      }

      "if product doesn't belong to same merchant" should {

        "return 404" in new ProductsAssignModifierSetsFSpecContext {
          val competitor = Factory.merchant.create
          val competitorProduct = Factory.simpleProduct(competitor).create

          Post(s"/v1/products.assign_modifier_sets?product_id=${competitorProduct.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }

    "if request has invalid token" should {

      "be rejected" in new ProductsAssignModifierSetsFSpecContext {
        val productId = UUID.randomUUID
        val productAssignment = random[ProductModifierSetsAssignment]
        Post(s"/v1/products.assign_modifier_sets?product_id=$productId", productAssignment)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
