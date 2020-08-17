package io.paytouch.ordering.resources.carts

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes

import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.entities._
import io.paytouch.ordering.stubs.PtCoreStubData
import io.paytouch.ordering.utils.{ MockedRestApi, FixtureDaoFactory => Factory }

class CartsUpdateProductFSpec extends CartItemOpsFSpec {
  abstract class CartUpdateProductSimpleFSpecContext extends CartItemOpsFSpecContext with ProductFixtures {
    @scala.annotation.nowarn("msg=Auto-application")
    val baseUpdate =
      random[CartItemUpdate]
        .copy(
          productId = None,
          quantity = Some(2),
          modifierOptions = None,
          bundleSets = None,
        )

    lazy val cartItem = Factory.cartItem(cart, productId = Some(product.id), calculatedPriceAmount = Some(10)).create

    def assertRelationsDeleted(
        modifierOptionIds: Seq[UUID],
        taxRateIds: Seq[UUID],
        variantOptionIds: Seq[UUID],
      ) = {
      cartItemModifierOptionDao.findByCartItemRelIds(modifierOptionIds).await must beEmpty
      cartItemTaxRateDao.findByCartItemRelIds(taxRateIds).await must beEmpty
      cartItemVariantOptionDao.findByCartItemRelIds(variantOptionIds).await must beEmpty
    }
  }

  "POST /v1/carts.update_product_item" in {
    "if request has valid token" in {
      "without modifiers" in {
        "changes data" in new CartUpdateProductSimpleFSpecContext {
          Post(s"/v1/carts.update_product_item?cart_id=${cart.id}&cart_item_id=${cartItem.id}", baseUpdate)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val entity = responseAs[ApiResponse[Cart]].data
            assertItemUpdated(entity, baseUpdate, cartItem, modifierOptionIds = Seq.empty)
          }
        }
      }

      "with modifiers" in {
        "changes data and refresh data from core product" in new CartUpdateProductSimpleFSpecContext {
          val modifierOption = random[ModifierOption].copy(active = true)

          @scala.annotation.nowarn("msg=Auto-application")
          override lazy val modifiers =
            Seq(
              random[ModifierSet]
                .copy(
                  locationOverrides = Map(romeId -> ItemLocation(active = true)),
                  options = Seq(modifierOption),
                ),
            )

          override lazy val modifierOptionCreations =
            Seq(random[CartItemModifierOptionCreation].copy(modifierOptionId = modifierOption.id))

          PtCoreStubData.recordProduct(product)(MockedRestApi.ptCoreClient.generateAuthHeaderForCore)

          val update = baseUpdate.copy(modifierOptions = Some(modifierOptionCreations))

          val existingCartItemModifierOption = Factory.cartItemModifierOption(cartItem).create
          val existingCartItemTaxRate = Factory.cartItemTaxRate(cartItem).create
          val existingCartItemVariantOption = Factory.cartItemVariantOption(cartItem).create

          Post(s"/v1/carts.update_product_item?cart_id=${cart.id}&cart_item_id=${cartItem.id}", update)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val entity = responseAs[ApiResponse[Cart]].data
            assertItemUpdated(
              entity,
              update,
              cartItem,
              modifierOptionIds = Seq(modifierOption.id),
              taxRateIds = taxRatesActive.map(_.id),
              variantOptionIds = Seq(variantOption.id),
            )
            assertRelationsDeleted(
              Seq(existingCartItemModifierOption.id),
              Seq(existingCartItemTaxRate.id),
              Seq(existingCartItemVariantOption.id),
            )
          }
        }
      }

      "with product id" in {
        "changes data and refresh data from core product" in new CartUpdateProductSimpleFSpecContext {
          val modifierOption = random[ModifierOption].copy(active = true)

          @scala.annotation.nowarn("msg=Auto-application")
          override lazy val modifiers = Seq(
            random[ModifierSet]
              .copy(locationOverrides = Map(romeId -> ItemLocation(active = true)), options = Seq(modifierOption)),
          )

          PtCoreStubData.recordProduct(product)(MockedRestApi.ptCoreClient.generateAuthHeaderForCore)

          val update = baseUpdate.copy(productId = Some(product.id))

          val existingCartItemModifierOption = Factory.cartItemModifierOption(cartItem).create
          val existingCartItemTaxRate = Factory.cartItemTaxRate(cartItem).create
          val existingCartItemVariantOption = Factory.cartItemVariantOption(cartItem).create

          Post(s"/v1/carts.update_product_item?cart_id=${cart.id}&cart_item_id=${cartItem.id}", update)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val entity = responseAs[ApiResponse[Cart]].data
            assertItemUpdated(
              entity,
              update,
              cartItem,
              modifierOptionIds = Seq(existingCartItemModifierOption.modifierOptionId),
              taxRateIds = taxRatesActive.map(_.id),
              variantOptionIds = Seq(variantOption.id),
            )
            assertRelationsDeleted(Seq.empty, Seq(existingCartItemTaxRate.id), Seq(existingCartItemVariantOption.id))
          }
        }
      }
    }
  }
}
