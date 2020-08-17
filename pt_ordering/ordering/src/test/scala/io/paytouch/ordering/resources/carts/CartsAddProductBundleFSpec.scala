package io.paytouch.ordering.resources.carts

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import org.scalacheck.Arbitrary
import io.paytouch.ordering.clients.paytouch.core.entities.{
  ArticleInfo,
  BundleOption,
  BundleSet,
  Product,
  ProductLocation,
}
import io.paytouch.ordering.entities._
import io.paytouch.ordering.entities.MonetaryAmount._
import io.paytouch.ordering.entities.enums.UnitType
import io.paytouch.ordering.stubs.PtCoreStubData
import io.paytouch.ordering.utils.{ FixtureDaoFactory => Factory }

class CartsAddProductBundleFSpec extends CartItemOpsFSpec {

  abstract class CartAddProductBundleFSpecContext extends CartItemOpsFSpecContext with ProductFixtures {
    override lazy val isCombo = true
    val bundledProduct1 = randomBundledProduct().copy(options = Seq(variantOption))
    val bundleOption1 = randomBundleOption(bundledProduct1.id)
    val bundledProduct2 = randomBundledProduct()
    val bundleOption2 = randomBundleOption(bundledProduct2.id)
    val bundleSet1 = random[BundleSet].copy(
      id = UUID.randomUUID,
      minQuantity = 1,
      maxQuantity = 3,
      options = Seq(bundleOption1, bundleOption2),
    )

    val bundledProduct3 = randomBundledProduct()
    val bundleOption3 = randomBundleOption(bundledProduct3.id)
    val bundledProduct4 = randomBundledProduct()
    val bundleOption4 = randomBundleOption(bundledProduct4.id)
    val bundleSet2 = random[BundleSet].copy(
      id = UUID.randomUUID,
      minQuantity = 0,
      maxQuantity = 1,
      options = Seq(bundleOption3, bundleOption4),
    )
    override lazy val bundleSets = Seq(bundleSet1)

    val bundleOptionCreation1 =
      random[CartItemBundleOptionCreation].copy(
        bundleOptionId = bundleOption1.id,
        quantity = 1,
        notes = Some("No mustard"),
        modifierOptions = Seq.empty,
      )

    val bundleOptionCreation2 =
      random[CartItemBundleOptionCreation].copy(
        bundleOptionId = bundleOption2.id,
        quantity = 2,
        notes = None,
        modifierOptions = Seq.empty,
      )

    val bundleOptionCreation3 =
      random[CartItemBundleOptionCreation].copy(
        bundleOptionId = bundleOption3.id,
        quantity = 1,
        notes = None,
        modifierOptions = Seq.empty,
      )

    val bundleOptionCreation4 =
      random[CartItemBundleOptionCreation].copy(
        bundleOptionId = bundleOption4.id,
        quantity = 0,
        notes = None,
        modifierOptions = Seq.empty,
      )

    val bundleSetCreation1 =
      CartItemBundleSetCreation(bundleSetId = bundleSet1.id, bundleOptions = Seq(bundleOptionCreation1))
    val bundleSetCreation2 =
      CartItemBundleSetCreation(bundleSetId = bundleSet2.id, bundleOptions = Seq(bundleOptionCreation3))
    override lazy val bundleSetCreations = Some(Seq(bundleSetCreation1))

    implicit val coreAuthToken = ptCoreClient.generateAuthHeaderForCore

    PtCoreStubData.recordProduct(product)
    PtCoreStubData.recordProduct(bundledProduct1)
    PtCoreStubData.recordProduct(bundledProduct2)
    PtCoreStubData.recordProduct(bundledProduct3)
    PtCoreStubData.recordProduct(bundledProduct4)
  }

  "POST /v1/carts.add_product" in {

    "if request has valid token" in {

      "with bundle product" in {

        "if product exists and is not part of the cart" should {

          "adds item to the cart" in new CartAddProductBundleFSpecContext {
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
                expectedBundleSetIdBundleOptionIdVariantOptionIds =
                  Map(bundleSet1.id -> Map(bundleOption1.id -> Seq(variantOption.id))),
              )
            }
          }

          "tracks notes and positions of bundle options" in new CartAddProductBundleFSpecContext {
            override lazy val bundleSets = Seq(bundleSet1, bundleSet2)

            override val bundleSetCreation1 =
              CartItemBundleSetCreation(
                bundleSetId = bundleSet1.id,
                bundleOptions = Seq(bundleOptionCreation1, bundleOptionCreation2),
              )

            override lazy val bundleSetCreations = Some(Seq(bundleSetCreation1, bundleSetCreation2))

            Post(s"/v1/carts.add_product", creation)
              .addHeader(storeAuthorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.Created)

              val entity = responseAs[ApiResponse[Cart]].data
              val set1 = entity.items.last.bundleSets.get(0)
              set1.bundleSetId ==== bundleSet1.id
              set1.position ==== 1
              set1.cartItemBundleOptions.length ==== 2

              val option1 = set1.cartItemBundleOptions(0)
              option1.bundleOptionId ==== bundleOption1.id
              option1.position ==== 1
              option1.item.notes ==== Some("No mustard")
              option1.item.quantity ==== 1
              option1.item.modifierOptions ==== Seq.empty

              val option2 = set1.cartItemBundleOptions(1)
              option2.bundleOptionId ==== bundleOption2.id
              option2.position ==== 2
              option2.item.notes ==== None
              option2.item.quantity ==== 2
              option2.item.modifierOptions ==== Seq.empty

              val set2 = entity.items.last.bundleSets.get(1)
              set2.bundleSetId ==== bundleSet2.id
              set2.position ==== 2
              set2.cartItemBundleOptions.length ==== 1

              val option3 = set2.cartItemBundleOptions(0)
              option3.bundleOptionId ==== bundleOption3.id
              option3.position ==== 1
              option3.item.notes ==== None
              option3.item.quantity ==== 1
              option3.item.modifierOptions ==== Seq.empty
            }
          }
        }

        "if product exists and is already part of the cart" should {

          "merge item to the cart" in new CartAddProductBundleFSpecContext {
            val cartItem = Factory
              .cartItem(
                cart,
                productId = Some(product.id),
                quantity = Some(3),
                priceAmount = Some(10),
                calculatedPriceAmount = Some(10),
                bundleSets = Some(
                  Seq(
                    CartItemBundleSet(
                      bundleSetId = bundleSet1.id,
                      name = None,
                      position = 0,
                      cartItemBundleOptions = Seq(
                        CartItemBundleOption(
                          bundleOptionId = bundleOption1.id,
                          item = CartItemBundleOptionItem(
                            product = CartItemProduct(
                              id = bundledProduct1.id,
                              name = bundledProduct1.name,
                              description = bundledProduct1.description,
                            ),
                            quantity = 1,
                            unit = UnitType.Unit,
                            cost = None,
                            notes = None,
                            modifierOptions = Seq.empty,
                            variantOptions = Seq.empty,
                          ),
                          priceAdjustment = BigDecimal(0),
                          position = 0,
                        ),
                      ),
                    ),
                  ),
                ),
              )
              .create

            Post(s"/v1/carts.add_product", creation)
              .addHeader(storeAuthorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.OK)

              val entity = responseAs[ApiResponse[Cart]].data
              assertItemMerged(entity, creation, cartItem)
            }
          }
        }

        "if product exists and is already part of the cart with different options" should {
          "adds item to the cart" in new CartAddProductBundleFSpecContext {
            val cartItem = Factory
              .cartItem(
                cart,
                productId = Some(product.id),
                quantity = Some(3),
                priceAmount = Some(10),
                calculatedPriceAmount = Some(10),
                bundleSets = Some(
                  Seq(
                    CartItemBundleSet(
                      bundleSetId = bundleSet1.id,
                      name = None,
                      position = 0,
                      cartItemBundleOptions = Seq(
                        CartItemBundleOption(
                          bundleOptionId = bundleOption2.id,
                          item = CartItemBundleOptionItem(
                            product = CartItemProduct(
                              id = bundledProduct2.id,
                              name = bundledProduct2.name,
                              description = bundledProduct2.description,
                            ),
                            quantity = 1,
                            unit = UnitType.Unit,
                            cost = None,
                            notes = None,
                            modifierOptions = Seq.empty,
                            variantOptions = Seq.empty,
                          ),
                          priceAdjustment = BigDecimal(0),
                          position = 0,
                        ),
                      ),
                    ),
                  ),
                ),
              )
              .create

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
                expectedBundleSetIdBundleOptionIdVariantOptionIds =
                  Map(bundleSet1.id -> Map(bundleOption1.id -> Seq(variantOption.id))),
                itemsSize = 2,
              )
            }
          }
        }
      }
    }
  }
}
