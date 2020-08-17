package io.paytouch.core.resources.modifiersets

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ UtcTime, FixtureDaoFactory => Factory }

class ModifierSetsAssignProductsFSpec extends ModifierSetsFSpec {

  "POST /v1/modifier_sets.assign_products?modifier_set_id=<modifier-set-id>" in {

    "if request has valid token" in {

      "if modifierSet belongs to same merchant" should {

        "patch the product" in new ModifierSetResourceFSpecContext {
          val yesterday = UtcTime.now.minusDays(1)

          val modifierSet = Factory.modifierSet(merchant, overrideNow = Some(yesterday)).create

          val product0 = Factory.simpleProduct(merchant, overrideNow = Some(yesterday)).create
          Factory.modifierSetProduct(modifierSet, product0).create

          val product1 = Factory.simpleProduct(merchant, overrideNow = Some(yesterday)).create
          val product2 = Factory.simpleProduct(merchant, overrideNow = Some(yesterday)).create
          val product3 = Factory.simpleProduct(merchant, overrideNow = Some(yesterday)).create
          val productIds = Seq(product1.id, product2.id, product3.id)
          val modifierSetAssignment = ModifierSetProductsAssignment(productIds = productIds)

          Post(s"/v1/modifier_sets.assign_products?modifier_set_id=${modifierSet.id}", modifierSetAssignment)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            modifierSetProductDao
              .findByModifierSetId(modifierSet.id)
              .await
              .map(_.productId)
              .toSet ==== productIds.toSet

            val allProductIds = Seq(product0.id, product1.id, product2.id, product3.id)
            val updatedProducts = articleDao.findByIds(allProductIds).await
            product0.updatedAt !=== updatedProducts.find(_.id == product0.id).get.updatedAt
            product1.updatedAt !=== updatedProducts.find(_.id == product1.id).get.updatedAt
            product2.updatedAt !=== updatedProducts.find(_.id == product2.id).get.updatedAt
            product3.updatedAt !=== updatedProducts.find(_.id == product3.id).get.updatedAt

            val updatedModifierSet = modifierSetDao.findById(modifierSet.id).await.get
            modifierSet.updatedAt !=== updatedModifierSet.updatedAt
          }
        }
      }

      "if any product id in the patch doesn't belong to same merchant" should {

        "return 404" in new ModifierSetResourceFSpecContext {
          val competitor = Factory.merchant.create

          val modifierSet = Factory.modifierSet(merchant).create
          val competitorProduct = Factory.simpleProduct(competitor).create
          val product = Factory.simpleProduct(merchant).create

          val productIds = Seq(competitorProduct.id, product.id)
          val modifierSetAssignment = ModifierSetProductsAssignment(productIds = productIds)

          Post(s"/v1/modifier_sets.assign_products?modifier_set_id=${modifierSet.id}", modifierSetAssignment)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if modifierSet doesn't belong to same merchant" should {

        "return 404" in new ModifierSetResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorModifierSet = Factory.modifierSet(competitor).create

          Post(s"/v1/modifier_sets.assign_products?modifier_set_id=${competitorModifierSet.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }

    "if request has invalid token" should {

      "be rejected" in new ModifierSetResourceFSpecContext {
        val modifierSetId = UUID.randomUUID
        val modifierSetAssignment = random[ModifierSetProductsAssignment]
        Post(s"/v1/modifier_sets.assign_products?modifier_set_id=$modifierSetId", modifierSetAssignment)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
