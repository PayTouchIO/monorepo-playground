package io.paytouch.ordering.resources.carts

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.entities._
import io.paytouch.ordering.stubs.PtCoreStubData
import io.paytouch.ordering.utils.{ FixtureDaoFactory => Factory }

class CartsAddProductSimpleWithModifiersFSpec extends CartItemOpsFSpec {

  trait SimpleProductAndModifiersFSpecContext extends CartItemOpsFSpecContext with ProductFixtures {

    val modifierOptions = random[ModifierOption](4).zipWithIndex.map {
      case (modifierOpt, idx) =>
        val active = idx % 2 == 0
        modifierOpt.copy(active = active)
    }
    val modifierOptionsActive = modifierOptions.filter(_.active)
    modifierOptionsActive.size ==== 2

    @scala.annotation.nowarn("msg=Auto-application")
    override lazy val modifiers = Seq(
      random[ModifierSet]
        .copy(
          locationOverrides = Map(romeId -> ItemLocation(active = true)),
          options = modifierOptions,
        ),
    )

    override lazy val modifierOptionCreations = Seq(
      random[CartItemModifierOptionCreation].copy(modifierOptionId = modifierOptionsActive.head.id),
    )

    implicit val coreAuthToken = ptCoreClient.generateAuthHeaderForCore
    PtCoreStubData.recordProduct(product)
  }

  "POST /v1/carts.add_product" in {

    "if request has valid token" in {

      "with simple product with modifiers" in {

        "if product exists and is not part of the cart" should {

          "adds item to the cart" in new SimpleProductAndModifiersFSpecContext {
            Post(s"/v1/carts.add_product", creation)
              .addHeader(storeAuthorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.Created)

              val entity = responseAs[ApiResponse[Cart]].data
              assertNewItemAdded(
                entity,
                creation,
                product,
                modifierOptions = Seq(modifierOptionsActive.head),
                taxRates = taxRatesActive,
                variantOptions = Seq(variantOption),
              )
            }
          }
        }

        "if product exists and is already part of the cart" should {

          "merge item to the cart" in new SimpleProductAndModifiersFSpecContext {
            val cartItem = Factory
              .cartItem(
                cart,
                productId = Some(product.id),
                quantity = Some(3),
                priceAmount = Some(10),
                calculatedPriceAmount = Some(10),
              )
              .create
            Factory.cartItemModifierOption(cartItem, modifierOptionId = Some(modifierOptionsActive.head.id)).create

            Post(s"/v1/carts.add_product", creation)
              .addHeader(storeAuthorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val entity = responseAs[ApiResponse[Cart]].data
              assertItemMerged(entity, creation, cartItem)
            }
          }
        }

        "if product exists and is already part of the cart with different options" should {
          "adds item to the cart" in new SimpleProductAndModifiersFSpecContext {
            val cartItem = Factory
              .cartItem(
                cart,
                productId = Some(product.id),
                quantity = Some(3),
                priceAmount = Some(10),
                calculatedPriceAmount = Some(10),
              )
              .create
            Factory.cartItemModifierOption(cartItem, modifierOptionId = Some(modifierOptionsActive.last.id)).create

            Post(s"/v1/carts.add_product", creation)
              .addHeader(storeAuthorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.Created)

              val entity = responseAs[ApiResponse[Cart]].data
              assertNewItemAdded(
                entity,
                creation,
                product,
                modifierOptions = Seq(modifierOptionsActive.head),
                taxRates = taxRatesActive,
                variantOptions = Seq(variantOption),
                itemsSize = 2,
              )
            }
          }
        }
      }
    }
  }
}
