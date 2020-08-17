package io.paytouch.ordering.calculations

import org.scalacheck._

import io.paytouch.implicits._

import io.paytouch.ordering.clients.paytouch.core.entities.enums.CartItemType
import io.paytouch.ordering.entities.enums.OrderType
import io.paytouch.ordering.entities._
import io.paytouch.ordering.entities.MonetaryAmount._
import io.paytouch.ordering.utils.CommonArbitraries
import io.paytouch.ordering.clients.paytouch.core.entities.GiftCard

class CartCalculationsSpec extends CartCalculationsOpsSpec {
  import CartCalculationsSpec._

  "CartCalculations" should {
    "compute as expected" should {
      "[SCENARIO A]" in {
        "single item with quantity > 1, addon + hold modifiers, only non included taxes" in {
          "if take out don't apply delivery fee" in new CartCalculationsSpecContext with CartFixtures {
            override lazy val tip = 1.50.USD

            override lazy val itemsDetails =
              Seq(
                ItemDetail(
                  productScenarioA,
                  quantity = 2,
                  modifierOptionQuantities = Map(
                    myAddA -> 2,
                    myHoldA -> 1,
                  ),
                ),
              )

            implicit val upsertion = calculationsUpdate(cart)

            assertCartTotals(tax = 2.78, subtotal = 7.50, total = 11.78)

            assertCartTaxRateTotals(applyA, total = 1.88)
            assertCartTaxRateTotals(applyB, total = 0.90)

            assertCartItemsSize(1)
            val cartItemUpsertion = upsertion.cartItems.head
            val cartItem = cart.items.head

            assertCartItemTotals(tax = 2.78, calculatedPrice = 1.00, total = 10.28)(cartItem, cartItemUpsertion)
            assertCartItemTaxRateTotals(applyA, total = 1.88)(cartItem, cartItemUpsertion)
            assertCartItemTaxRateTotals(applyB, total = 0.90)(cartItem, cartItemUpsertion)
          }

          "if delivery apply delivery fee" in new CartCalculationsSpecContext with CartFixtures {
            override lazy val tip = 1.50.USD

            override lazy val itemsDetails =
              Seq(
                ItemDetail(
                  productScenarioA,
                  quantity = 2,
                  modifierOptionQuantities = Map(
                    myAddA -> 2,
                    myHoldA -> 1,
                  ),
                ),
              )

            implicit val upsertion =
              calculationsUpdate(cart.copy(orderType = OrderType.Delivery))
            assertCartTotals(
              tax = 2.78,
              subtotal = 7.50,
              total = 16.78,
              deliveryFeeAmount = ResettableBigDecimal(Some(storeContext.deliveryFeeAmount)),
            )
          }
        }
      }

      "[SCENARIO B]" in {
        "single item with quantity > 1, addon + hold modifiers, included and non included taxes" in new CartCalculationsSpecContext
          with CartFixtures {
          override lazy val tip = 1.50 USD
          override lazy val itemsDetails =
            Seq(ItemDetail(productScenarioB, quantity = 2, modifierOptionQuantities = Map(myAddA -> 2, myHoldA -> 1)))

          implicit val upsertion = calculationsUpdate(cart)
          assertCartTotals(tax = 3.76, subtotal = 6.52, total = 11.78)

          assertCartTaxRateTotals(applyA, total = 1.88)
          assertCartTaxRateTotals(applyB, total = 0.90)
          assertCartTaxRateTotals(includedA, total = 0.33)
          assertCartTaxRateTotals(includedB, total = 0.65)

          assertCartItemsSize(1)
          val (cartItem, cartItemUpsertion) = assertCartItem(0)

          assertCartItemTotals(tax = 3.76, calculatedPrice = 1.00, total = 10.28)(cartItem, cartItemUpsertion)
          assertCartItemTaxRateTotals(applyA, total = 1.88)(cartItem, cartItemUpsertion)
          assertCartItemTaxRateTotals(applyB, total = 0.90)(cartItem, cartItemUpsertion)
          assertCartItemTaxRateTotals(includedA, total = 0.33)(cartItem, cartItemUpsertion)
          assertCartItemTaxRateTotals(includedB, total = 0.65)(cartItem, cartItemUpsertion)
        }
      }

      "[SCENARIO C]" in {
        "multiple items with quantity > 1, addon + hold modifiers, only non included taxes" in new CartCalculationsSpecContext
          with CartFixtures {
          override lazy val tip = 1.50 USD
          override lazy val itemsDetails = Seq(
            ItemDetail(productScenarioC1, quantity = 2, modifierOptionQuantities = Map(myAddA -> 2, myHoldA -> 1)),
            ItemDetail(productScenarioC2, quantity = 1, modifierOptionQuantities = Map(myAddA -> 2, myHoldA -> 1)),
          )

          implicit val upsertion = calculationsUpdate(cart)
          assertCartTotals(tax = 4.17, subtotal = 11.25, total = 16.92)

          assertCartTaxRateTotals(applyA, total = 2.82)
          assertCartTaxRateTotals(applyB, total = 1.35)

          assertCartItemsSize(2)

          val (cartItem1, cartItem1Upsertion) = assertCartItem(0)
          assertCartItemTotals(tax = 2.78, calculatedPrice = 1.00, total = 10.28)(cartItem1, cartItem1Upsertion)
          assertCartItemTaxRateTotals(applyA, total = 1.88)(cartItem1, cartItem1Upsertion)
          assertCartItemTaxRateTotals(applyB, total = 0.90)(cartItem1, cartItem1Upsertion)

          val (cartItem2, cartItem2Upsertion) = assertCartItem(1)
          assertCartItemTotals(tax = 1.39, calculatedPrice = 1.00, total = 5.14)(cartItem2, cartItem2Upsertion)
          assertCartItemTaxRateTotals(applyA, total = 0.94)(cartItem2, cartItem2Upsertion)
          assertCartItemTaxRateTotals(applyB, total = 0.45)(cartItem2, cartItem2Upsertion)
        }
      }

      "[SCENARIO D]" in {
        "single item with quantity > 1, neutral modifiers, only non included taxes" in new CartCalculationsSpecContext
          with CartFixtures {
          override lazy val tip = 1.50 USD
          override lazy val itemsDetails =
            Seq(ItemDetail(productScenarioD, quantity = 2, modifierOptionQuantities = Map(myNeutralA -> 1)))

          implicit val upsertion = calculationsUpdate(cart)
          assertCartTotals(tax = 0.74, subtotal = 2, total = 4.24)

          assertCartTaxRateTotals(applyA, total = 0.5)
          assertCartTaxRateTotals(applyB, total = 0.24)

          assertCartItemsSize(1)
          val (cartItem, cartItemUpsertion) = assertCartItem(0)

          assertCartItemTotals(tax = 0.74, calculatedPrice = 1.00, total = 2.74)(cartItem, cartItemUpsertion)

          assertCartItemTaxRateTotals(applyA, total = 0.50)(cartItem, cartItemUpsertion)
          assertCartItemTaxRateTotals(applyB, total = 0.24)(cartItem, cartItemUpsertion)
        }
      }

      "[SCENARIO E]" in {
        "bundle item with no price adjustment and with modifier" in new CartCalculationsSpecContext with CartFixtures {
          override lazy val tip = 0 USD
          override lazy val itemsDetails =
            Seq(
              ItemDetail(
                bundleScenarioE,
                1,
                modifierOptionQuantities = Map.empty,
                bundleOptionModifiersAndQuantities = Map(myBundleOption1 -> Map(myAddA -> 1)),
              ),
            )

          implicit val upsertion = calculationsUpdate(cart)
          assertCartTotals(tax = 0.88, subtotal = 3.5, total = 4.38)

          assertCartTaxRateTotals(applyA, total = 0.88)

          assertCartItemsSize(1)
          val (cartItem, cartItemUpsertion) = assertCartItem(0)

          assertCartItemTotals(tax = 0.88, calculatedPrice = 2, total = 4.38, cost = Some(0))(
            cartItem,
            cartItemUpsertion,
          )
          assertCartItemTaxRateTotals(applyA, total = 0.88)(cartItem, cartItemUpsertion)
        }

        "bundle item with price adjustment and no modifier" in new CartCalculationsSpecContext with CartFixtures {
          override lazy val tip = 0 USD
          override lazy val itemsDetails =
            Seq(
              ItemDetail(
                bundleScenarioE,
                1,
                modifierOptionQuantities = Map.empty,
                bundleOptionModifiersAndQuantities = Map(myBundleOption2 -> Map.empty),
              ),
            )

          implicit val upsertion = calculationsUpdate(cart)
          assertCartTotals(tax = 1.75, subtotal = 7, total = 8.75)

          assertCartTaxRateTotals(applyA, total = 1.75)

          assertCartItemsSize(1)
          val (cartItem, cartItemUpsertion) = assertCartItem(0)

          assertCartItemTotals(tax = 1.75, calculatedPrice = 2, total = 8.75, cost = Some(0))(
            cartItem,
            cartItemUpsertion,
          )
          assertCartItemTaxRateTotals(applyA, total = 1.75)(cartItem, cartItemUpsertion)
        }
      }

      "[SCENARIO F]" in {
        "gift card with quantity = 1" should {
          "if takeout don't apply delivery fee" in new CartCalculationsSpecContext with CartFixtures {
            override lazy val tip = 0.USD
            override lazy val itemsDetails =
              Seq(
                ItemDetail(
                  productScenarioF,
                  quantity = 1,
                  modifierOptionQuantities = Map.empty,
                ),
              )

            implicit val upsertion = calculationsUpdate(cart)

            assertCartTotals(tax = 0, subtotal = 10, total = 10)

            assertCartItemsSize(1)
            val cartItemUpsertion = upsertion.cartItems.head
            val cartItem = cart.items.head

            assertCartItemTotals(tax = 0, calculatedPrice = 10, total = 10)(cartItem, cartItemUpsertion)
          }

          "if delivery and gift card only order don't apply delivery fee" in new CartCalculationsSpecContext
            with CartFixtures {
            override lazy val tip = 0.USD
            override lazy val itemsDetails =
              Seq(
                ItemDetail(
                  productScenarioF,
                  quantity = 1,
                  modifierOptionQuantities = Map.empty,
                ),
              )

            implicit val upsertion =
              calculationsUpdate(cart.copy(orderType = OrderType.Delivery))

            assertCartTotals(tax = 0, subtotal = 10, total = 10)

            assertCartItemsSize(1)
            val cartItemUpsertion = upsertion.cartItems.head
            val cartItem = cart.items.head

            assertCartItemTotals(tax = 0, calculatedPrice = 10, total = 10)(cartItem, cartItemUpsertion)
          }

          "if delivery and mixed order apply delivery fee" in new CartCalculationsSpecContext with CartFixtures {
            override lazy val tip = 0.USD
            override lazy val itemsDetails =
              Seq(
                ItemDetail(
                  productScenarioF,
                  quantity = 1,
                  modifierOptionQuantities = Map.empty,
                ),
                ItemDetail(
                  productScenarioA,
                  quantity = 2,
                  modifierOptionQuantities = Map(
                    myAddA -> 2,
                    myHoldA -> 1,
                  ),
                ),
              )

            implicit val upsertion =
              calculationsUpdate(cart.copy(orderType = OrderType.Delivery))

            assertCartTotals(
              tax = 2.78,
              subtotal = 17.5,
              total = 25.28,
              deliveryFeeAmount = ResettableBigDecimal(Some(storeContext.deliveryFeeAmount)),
            )

            assertCartItemsSize(2)
            val cartItemGiftCardUpsertion = upsertion.cartItems.head
            val cartItemGiftCard = cart.items.head

            assertCartItemTotals(tax = 0, calculatedPrice = 10, total = 10)(cartItemGiftCard, cartItemGiftCardUpsertion)

            val cartItemProductUpsertion = upsertion.cartItems(1)
            val cartItemProduct = cart.items(1)
            assertCartItemTotals(tax = 2.78, calculatedPrice = 1.00, total = 10.28)(
              cartItemProduct,
              cartItemProductUpsertion,
            )
          }
        }
      }

      "[SCENARIO G]" in {
        "single item with quantity > 1, addon + hold modifiers, only non included taxes" in {
          "(semi-property based test) start calculation from scratch and apply the gift cards sorted by addedAt and don't effect total if there are no gift cards" in new CartCalculationsSpecContext
            with CartFixtures {
            override lazy val tip = 1.50.USD

            override lazy val itemsDetails =
              Seq(
                ItemDetail(
                  productScenarioA,
                  quantity = 2,
                  modifierOptionQuantities = Map(
                    myAddA -> 2,
                    myHoldA -> 1,
                  ),
                ),
              )

            val inputAppliedPasses: Seq[GiftCardPassApplied] =
              actuallyRandomPassSeq(3)

            implicit val upsertion =
              calculationsUpdate(
                cart.copy(
                  orderType = OrderType.Delivery,
                  appliedGiftCardPasses = inputAppliedPasses,
                ),
              )

            upsertion.cart.appliedGiftCardPasses.get.map(_.id) ==== inputAppliedPasses
              .sortBy(_.addedAt)(Ordering.fromLessThan(_ isBefore _))
              .map(_.id)
          }

          "if total is zero don't apply gift card passes" in new CartCalculationsSpecContext with CartFixtures {
            override lazy val tip = 0.USD

            override lazy val itemsDetails =
              Seq.empty

            val inputAppliedPasses: Seq[GiftCardPassApplied] =
              actuallyRandomPassSeq(3)

            implicit val upsertion =
              calculationsUpdate(
                cart.copy(
                  orderType = OrderType.Delivery,
                  appliedGiftCardPasses = inputAppliedPasses,
                ),
              )

            upsertion.cart.appliedGiftCardPasses.get ==== inputAppliedPasses
              .sortBy(_.addedAt)
              .map(_.copy(amountToCharge = None))

            assertCartTotals(
              tax = 0,
              subtotal = 0,
              total = 0,
            )
          }

          "apply gift cards one by one" in new CartCalculationsSpecContext with CartFixtures {
            override lazy val tip = 1.50.USD

            override lazy val itemsDetails =
              Seq(
                ItemDetail(
                  productScenarioA,
                  quantity = 2,
                  modifierOptionQuantities = Map(
                    myAddA -> 2,
                    myHoldA -> 1,
                  ),
                ),
              )

            val inputAppliedPasses: Seq[GiftCardPassApplied] =
              actuallyRandomPassSeq(3).sortBy(_.addedAt).zipWithIndex.map {
                case (pass, 0) => pass.copy(balance = 3.USD)
                case (pass, 1) => pass.copy(balance = 8.USD)
                case (pass, _) => pass.copy(balance = 4.USD)
              }

            implicit val upsertion =
              calculationsUpdate(
                cart.copy(
                  orderType = OrderType.Delivery,
                  appliedGiftCardPasses = inputAppliedPasses,
                ),
              )

            val actualAmountsToCharge =
              upsertion.cart.appliedGiftCardPasses.get.flatMap(_.amountToCharge).map(_.amount)

            actualAmountsToCharge.head === 3
            actualAmountsToCharge.tail.head === 8
            actualAmountsToCharge.tail.tail.head === 4

            val expectedAmountsToChargeTotal = 3 + 8 + 4

            actualAmountsToCharge.sum ==== expectedAmountsToChargeTotal

            val expectedTotalWithoutGiftCards = 16.78
            val expectedTotal = expectedTotalWithoutGiftCards - expectedAmountsToChargeTotal

            assertCartTotals(
              tax = 2.78,
              subtotal = 7.50,
              total = expectedTotal,
              totalWithoutGiftCards = expectedTotalWithoutGiftCards.somew,
              deliveryFeeAmount = ResettableBigDecimal(Some(storeContext.deliveryFeeAmount)),
            )
          }

          "apply gift cards one by one with left overs (but neither gift cards nor tips should be paid with gift cards)" in new CartCalculationsSpecContext
            with CartFixtures {
            override lazy val tip = 1.50.USD

            override lazy val itemsDetails =
              Seq(
                ItemDetail(
                  productScenarioF,
                  quantity = 1,
                  modifierOptionQuantities = Map.empty,
                ),
                ItemDetail(
                  productScenarioA,
                  quantity = 2,
                  modifierOptionQuantities = Map(
                    myAddA -> 2,
                    myHoldA -> 1,
                  ),
                ),
              )

            val inputAppliedPasses: Seq[GiftCardPassApplied] =
              actuallyRandomPassSeq(3).sortBy(_.addedAt).zipWithIndex.map {
                case (pass, 0) => pass.copy(balance = 3.USD)
                case (pass, 1) => pass.copy(balance = 8.USD)
                case (pass, _) => pass.copy(balance = 6.USD)
              }

            implicit val upsertion =
              calculationsUpdate(
                cart.copy(
                  orderType = OrderType.Delivery,
                  appliedGiftCardPasses = inputAppliedPasses,
                ),
              )

            val actualAmountsToCharge =
              upsertion.cart.appliedGiftCardPasses.get.flatMap(_.amountToCharge).map(_.amount)

            actualAmountsToCharge.head === 3
            actualAmountsToCharge.tail.head === 8
            actualAmountsToCharge.tail.tail.head === 5.78 - tip.amount

            val expectedAmountsToChargeTotal = 3 + 8 + (5.78 - tip.amount)

            actualAmountsToCharge.sum ==== expectedAmountsToChargeTotal

            val expectedTotalWithoutGiftCards = 26.78
            val expectedTotal = expectedTotalWithoutGiftCards - expectedAmountsToChargeTotal
            expectedTotal ==== productScenarioF.price + tip.amount

            assertCartTotals(
              tax = 2.78,
              subtotal = 17.50,
              total = expectedTotal,
              totalWithoutGiftCards = expectedTotalWithoutGiftCards.somew,
              deliveryFeeAmount = ResettableBigDecimal(Some(storeContext.deliveryFeeAmount)),
            )
          }
        }
      }
    }
  }
}

object CartCalculationsSpec extends CommonArbitraries {
  import org.scalacheck.ScalacheckShapeless._

  val arbitrary: Gen[GiftCardPassApplied] =
    Arbitrary.arbitrary[GiftCardPassApplied]

  def actuallyRandomPassSeq(passes: Int): Seq[GiftCardPassApplied] =
    1 to passes map (_ => arbitrary.instance)
}
