package io.paytouch.ordering.resources.carts

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes

import cats.implicits._

import io.paytouch.implicits._

import io.paytouch.ordering.clients.paytouch.core.entities.Product
import io.paytouch.ordering.entities._
import io.paytouch.ordering.entities.enums.CartStatus
import io.paytouch.ordering.stubs.PtCoreStubData
import io.paytouch.ordering.utils.{ FixtureDaoFactory => Factory }

class CartsAddProductSimpleFSpec extends CartItemOpsFSpec {
  abstract class CartAddProductSimpleFSpecContext extends CartItemOpsFSpecContext with ProductFixtures {
    implicit val coreAuthToken = ptCoreClient.generateAuthHeaderForCore
    PtCoreStubData.recordProduct(product)
  }

  "POST /v1/carts.add_product" in {
    "if request has valid token" in {
      "with simple product" in {
        "if product exists and is not part of the cart" should {
          "adds item to the cart" in new CartAddProductSimpleFSpecContext {
            Post(s"/v1/carts.add_product", creation)
              .addHeader(storeAuthorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.Created)

              val entity = responseAs[ApiResponse[Cart]].data

              assertNewItemAdded(
                entity,
                creation,
                product,
                taxRates = taxRatesActive,
                variantOptions = Seq(variantOption),
              )

              assertCartTotals(
                entity,
                subtotal = (9.85 * cartItemCreationQuantity).somew,
                tax = (0.9 * cartItemCreationQuantity).somew,
                total = (10.75 * cartItemCreationQuantity).somew,
              )
            }
          }

          "doesn't sync the cart to core" in new CartAddProductSimpleFSpecContext {
            Post(s"/v1/carts.add_product", creation)
              .addHeader(storeAuthorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.Created)
              assertCartStatus(cart.id, status = CartStatus.New, synced = Some(false))
            }
          }
        }

        "if product exists and is already part of the cart" should {
          "merge item to the cart" in new CartAddProductSimpleFSpecContext {
            val cartItem =
              Factory
                .cartItem(
                  cart,
                  productId = product.id.some,
                  quantity = 3.somew,
                  priceAmount = 10.somew,
                  calculatedPriceAmount = 10.somew,
                )
                .create

            Post(s"/v1/carts.add_product", creation)
              .addHeader(storeAuthorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val entity = responseAs[ApiResponse[Cart]].data

              assertItemMerged(entity, creation, cartItem)

              assertCartTotals(
                entity,
                subtotal = 49.26.somew,
                tax = 4.49.somew,
                total = 53.75.somew,
              )
            }
          }
        }
      }
    }
  }
}
