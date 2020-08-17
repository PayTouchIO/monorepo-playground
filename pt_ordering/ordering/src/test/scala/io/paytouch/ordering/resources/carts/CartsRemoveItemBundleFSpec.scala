package io.paytouch.ordering.resources.carts

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import org.scalacheck.Arbitrary
import io.paytouch.ordering.clients.paytouch.core.entities.{ ArticleInfo, BundleOption, BundleSet }
import io.paytouch.ordering.entities._
import io.paytouch.ordering.entities.MonetaryAmount._
import io.paytouch.ordering.stubs.PtCoreStubData
import io.paytouch.ordering.utils.{ FixtureDaoFactory => Factory }

class CartsRemoveItemBundleFSpec extends CartItemOpsFSpec {

  abstract class CartsRemoveItemBundleFSpecContext extends CartItemOpsFSpecContext with ProductFixtures {
    override lazy val isCombo = true
    val bundledProduct1 = randomBundledProduct()
    val bundleOption1 = randomBundleOption(bundledProduct1.id)
    val bundledProduct2 = randomBundledProduct()
    val bundleOption2 = randomBundleOption(bundledProduct2.id)
    val bundleSet1 = random[BundleSet].copy(
      id = UUID.randomUUID,
      minQuantity = 1,
      maxQuantity = 1,
      options = Seq(bundleOption1, bundleOption2),
    )

    implicit val coreAuthToken = ptCoreClient.generateAuthHeaderForCore

    PtCoreStubData.recordProduct(product)
    PtCoreStubData.recordProduct(bundledProduct1)
    PtCoreStubData.recordProduct(bundledProduct2)

    val cartItemProduct1 = CartItemProduct(
      id = bundledProduct1.id,
      name = bundledProduct1.name,
      description = bundledProduct1.description,
    )

    val cartItemBundleOptionItem1 = CartItemBundleOptionItem(
      product = cartItemProduct1,
      quantity = 1,
      unit = genUnitType.instance,
      cost = None,
      notes = None,
      modifierOptions = Seq.empty,
      variantOptions = Seq.empty,
    )

    val cartItemBundleOption1 = CartItemBundleOption(
      bundleOptionId = bundleOption1.id,
      item = cartItemBundleOptionItem1,
      priceAdjustment = 0,
      position = 0,
    )

    val cartItemBundleSet = CartItemBundleSet(
      bundleSetId = bundleSet1.id,
      name = None,
      position = 0,
      cartItemBundleOptions = Seq(cartItemBundleOption1),
    )

    val cartItem =
      Factory.cartItem(cart, productId = Some(product.id), bundleSets = Some(Seq(cartItemBundleSet))).create
  }

  "POST /v1/carts.remove_item?cart_id=$&cart_item_id=$" in {
    "if request has valid token" in {
      "if cart item id exists" should {
        "remove cart item" in new CartsRemoveItemBundleFSpecContext {
          Post(s"/v1/carts.remove_item?cart_id=${cart.id}&cart_item_id=${cartItem.id}")
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val entity = responseAs[ApiResponse[Cart]].data
            assertCartTotals(
              entity,
              subtotal = Some(BigDecimal(0)),
              tax = Some(BigDecimal(0)),
              total = Some(BigDecimal(0)),
            )

            cartItemDao.findById(cartItem.id).await must beNone
          }
        }
      }
    }
  }
}
