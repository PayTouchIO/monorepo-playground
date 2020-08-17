package io.paytouch.ordering.resources.carts

import java.util.UUID

import cats.implicits._

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{ AuthenticationFailedRejection, ValidationRejection }

import io.paytouch.ordering.clients.paytouch.core.entities.{ BundleOption, BundleSet }
import io.paytouch.ordering.entities._
import io.paytouch.ordering.stubs.PtCoreStubData
import io.paytouch.ordering.utils.{ FixtureDaoFactory => Factory }

class CartsAddProductRejectionsFSpec extends CartItemOpsFSpec {
  abstract class CartAddProductFSpecContext extends CartItemOpsFSpecContext with ProductFixtures

  "POST /v1/carts.add_product" in {
    "if request has valid token" in {
      "if product id does not exist in core" should {
        "reject the request" in new CartAddProductFSpecContext {
          val invalidCreation = creation.copy(productId = UUID.randomUUID)

          Post(s"/v1/carts.add_product", invalidCreation)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.InternalServerError)
            assertErrorCode("InternalServerError")
          }
        }
      }

      "if cart id does not exist" should {
        "reject the request" in new CartAddProductFSpecContext {
          val invalidCreation = creation.copy(cartId = UUID.randomUUID)

          Post(s"/v1/carts.add_product", invalidCreation)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            rejection ==== ValidationRejection("cart id does not exist")
          }
        }
      }

      "if modifier option id does not exist in core" should {
        "reject the request" in new CartAddProductFSpecContext {
          override lazy val modifierOptionCreations = Seq(random[CartItemModifierOptionCreation])

          implicit val coreAuthToken = ptCoreClient.generateAuthHeaderForCore
          PtCoreStubData.recordProduct(product)

          Post(s"/v1/carts.add_product", creation).addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)

            assertErrorCode("InvalidModifierOptionIds")
          }
        }
      }

      "if bundle set are sent for a non bundle product" should {
        "reject the request" in new CartAddProductFSpecContext {
          override lazy val bundleSetCreations = Some(Seq(random[CartItemBundleSetCreation]))

          implicit val coreAuthToken = ptCoreClient.generateAuthHeaderForCore
          PtCoreStubData.recordProduct(product)

          Post(s"/v1/carts.add_product", creation).addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)

            assertErrorCode("BundleMetadataForNonBundleProduct")
          }
        }
      }

      "if no bundle set are sent for a bundle product" should {
        "reject the request" in new CartAddProductFSpecContext {
          override lazy val isCombo = true
          override lazy val bundleSets = Seq(random[BundleSet])

          implicit val coreAuthToken = ptCoreClient.generateAuthHeaderForCore
          PtCoreStubData.recordProduct(product)

          Post(s"/v1/carts.add_product", creation).addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)

            assertErrorCode("MissingBundleOptionsForBundleProduct")
          }
        }
      }

      "if not enough options in bundle set are sent for a bundle product" should {
        "reject the request" in new CartAddProductFSpecContext {
          override lazy val isCombo = true
          val bundleProduct1 = randomBundledProduct()
          val bundleOption1 = randomBundleOption(bundleProduct1.id)
          val bundleSet1 = random[BundleSet].copy(
            id = UUID.randomUUID,
            minQuantity = 2,
            maxQuantity = 2,
            options = Seq(bundleOption1),
          )
          override lazy val bundleSets = Seq(bundleSet1)

          private val bundleOptionCreation1 =
            random[CartItemBundleOptionCreation].copy(
              bundleOptionId = bundleOption1.id,
              quantity = 1,
              modifierOptions = Seq.empty,
            )
          val bundleSetCreation =
            CartItemBundleSetCreation(bundleSetId = bundleSet1.id, bundleOptions = Seq(bundleOptionCreation1))
          override lazy val bundleSetCreations = Some(Seq(bundleSetCreation))

          implicit val coreAuthToken = ptCoreClient.generateAuthHeaderForCore
          PtCoreStubData.recordProduct(product)
          PtCoreStubData.recordProduct(bundleProduct1)

          Post(s"/v1/carts.add_product", creation).addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)

            assertErrorCode("NotEnoughOptionsForBundleSet")
          }
        }
      }

      "if too many options in bundle set are sent for a bundle product" should {
        "reject the request" in new CartAddProductFSpecContext {
          override lazy val isCombo = true
          val bundleProduct1 = randomBundledProduct()
          val bundleOption1 = randomBundleOption(bundleProduct1.id)
          val bundleProduct2 = randomBundledProduct()
          val bundleOption2 = randomBundleOption(bundleProduct2.id)
          val bundleSet1 = random[BundleSet].copy(
            id = UUID.randomUUID,
            minQuantity = 1,
            maxQuantity = 2,
            options = Seq(bundleOption1, bundleOption2),
          )
          override lazy val bundleSets = Seq(bundleSet1)

          private val bundleOptionCreation1 =
            random[CartItemBundleOptionCreation].copy(
              bundleOptionId = bundleOption1.id,
              quantity = 1,
              modifierOptions = Seq.empty,
            )

          private val bundleOptionCreation2 =
            random[CartItemBundleOptionCreation].copy(
              bundleOptionId = bundleOption1.id,
              quantity = 2,
              modifierOptions = Seq.empty,
            )

          val bundleSetCreation =
            CartItemBundleSetCreation(
              bundleSetId = bundleSet1.id,
              bundleOptions = Seq(bundleOptionCreation1, bundleOptionCreation2),
            )

          override lazy val bundleSetCreations =
            Seq(bundleSetCreation).some

          implicit val coreAuthToken = ptCoreClient.generateAuthHeaderForCore
          PtCoreStubData.recordProduct(product)
          PtCoreStubData.recordProduct(bundleProduct1)
          PtCoreStubData.recordProduct(bundleProduct2)

          Post(s"/v1/carts.add_product", creation).addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)

            assertErrorCode("TooManyOptionsForBundleSet")
          }
        }
      }

      "if wrong option id in bundle set is sent for a bundle product" should {
        "reject the request" in new CartAddProductFSpecContext {
          override lazy val isCombo = true
          val bundleProduct1 = randomBundledProduct()
          val bundleOption1 = randomBundleOption(bundleProduct1.id)
          val bundleSet1 = random[BundleSet].copy(
            id = UUID.randomUUID,
            minQuantity = 1,
            maxQuantity = 1,
            options = Seq(bundleOption1),
          )
          override lazy val bundleSets = Seq(bundleSet1)

          private val bundleOptionCreation1 =
            random[CartItemBundleOptionCreation].copy(bundleOptionId = UUID.randomUUID, modifierOptions = Seq.empty)
          val bundleSetCreation =
            CartItemBundleSetCreation(bundleSetId = bundleSet1.id, bundleOptions = Seq(bundleOptionCreation1))
          override lazy val bundleSetCreations = Some(Seq(bundleSetCreation))

          implicit val coreAuthToken = ptCoreClient.generateAuthHeaderForCore
          PtCoreStubData.recordProduct(product)
          PtCoreStubData.recordProduct(bundleProduct1)

          Post(s"/v1/carts.add_product", creation).addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)

            assertErrorCode("InvalidBundleOptionIdForBundleSet")
          }
        }
      }

      "if same bundle set is sent more than once" should {
        "reject the request" in new CartAddProductFSpecContext {
          override lazy val isCombo = true
          val bundleProduct1 = randomBundledProduct()
          val bundleOption1 = randomBundleOption(bundleProduct1.id)
          val bundleSet1 = random[BundleSet].copy(
            id = UUID.randomUUID,
            minQuantity = 1,
            maxQuantity = 1,
            options = Seq(bundleOption1),
          )
          override lazy val bundleSets = Seq(bundleSet1)

          private val bundleOptionCreation1 =
            random[CartItemBundleOptionCreation].copy(bundleOptionId = bundleOption1.id, modifierOptions = Seq.empty)
          val bundleSetCreation =
            CartItemBundleSetCreation(bundleSetId = bundleSet1.id, bundleOptions = Seq(bundleOptionCreation1))
          override lazy val bundleSetCreations = Some(Seq(bundleSetCreation, bundleSetCreation))

          implicit val coreAuthToken = ptCoreClient.generateAuthHeaderForCore
          PtCoreStubData.recordProduct(product)
          PtCoreStubData.recordProduct(bundleProduct1)

          Post(s"/v1/carts.add_product", creation).addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)

            assertErrorCode("DuplicatedBundleMetadataForBundleProduct")
          }
        }
      }

      "if a bundle set is missing" should {
        "reject the request" in new CartAddProductFSpecContext {
          override lazy val isCombo = true
          val bundleProduct1 = randomBundledProduct()
          val bundleOption1 = randomBundleOption(bundleProduct1.id)
          val bundleSet1 = random[BundleSet].copy(
            id = UUID.randomUUID,
            minQuantity = 1,
            maxQuantity = 1,
            options = Seq(bundleOption1),
          )
          override lazy val bundleSets = Seq(bundleSet1)

          private val bundleOptionCreation1 =
            random[CartItemBundleOptionCreation].copy(bundleOptionId = bundleOption1.id, modifierOptions = Seq.empty)
          val bundleSetCreation =
            CartItemBundleSetCreation(bundleSetId = UUID.randomUUID, bundleOptions = Seq(bundleOptionCreation1))
          override lazy val bundleSetCreations = Some(Seq(bundleSetCreation))

          implicit val coreAuthToken = ptCoreClient.generateAuthHeaderForCore
          PtCoreStubData.recordProduct(product)
          PtCoreStubData.recordProduct(bundleProduct1)

          Post(s"/v1/carts.add_product", creation).addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)

            assertErrorCode("MissingBundleMetadataForBundleSet")
          }
        }
      }

      "if a bundle product can't be retrieved from core" should {
        "reject the request" in new CartAddProductFSpecContext {
          override lazy val isCombo = true
          val bundleProduct1 = randomBundledProduct()
          val bundleOption1 = randomBundleOption(bundleProduct1.id)
          val bundleSet1 = random[BundleSet].copy(
            id = UUID.randomUUID,
            minQuantity = 1,
            maxQuantity = 1,
            options = Seq(bundleOption1),
          )
          override lazy val bundleSets = Seq(bundleSet1)

          private val bundleOptionCreation1 =
            random[CartItemBundleOptionCreation].copy(bundleOptionId = bundleOption1.id, modifierOptions = Seq.empty)
          val bundleSetCreation =
            CartItemBundleSetCreation(bundleSetId = bundleSet1.id, bundleOptions = Seq(bundleOptionCreation1))
          override lazy val bundleSetCreations = Some(Seq(bundleSetCreation))

          implicit val coreAuthToken = ptCoreClient.generateAuthHeaderForCore
          PtCoreStubData.recordProduct(product)

          Post(s"/v1/carts.add_product", creation).addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)

            assertErrorCode("FailedToFetchAllProducts")
          }
        }
      }

      "if the product is out of stock" should {
        "reject the request" in new CartAddProductFSpecContext {
          override lazy val quantity = BigDecimal(0)

          implicit val coreAuthToken = ptCoreClient.generateAuthHeaderForCore
          PtCoreStubData.recordProduct(product)

          Post(s"/v1/carts.add_product", creation)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("ProductOutOfStock")
          }
        }

        "accept the request if trackInventory = false" in new CartAddProductFSpecContext {
          override lazy val quantity = BigDecimal(0)
          override lazy val trackInventory = false

          implicit val coreAuthToken = ptCoreClient.generateAuthHeaderForCore
          PtCoreStubData.recordProduct(product)

          Post(s"/v1/carts.add_product", creation)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.Created)
          }
        }

        "accept the request if sellOutOfStock = true" in new CartAddProductFSpecContext {
          override lazy val quantity = BigDecimal(0)
          override lazy val sellOutOfStock = true

          implicit val coreAuthToken = ptCoreClient.generateAuthHeaderForCore
          PtCoreStubData.recordProduct(product)

          Post(s"/v1/carts.add_product", creation)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.Created)
          }
        }
      }

      "if a bundled product is out of stock" should {
        "reject the request" in new CartAddProductFSpecContext {
          override lazy val isCombo = true
          val bundleProduct1 = randomBundledProduct(quantity = 0)
          val bundleOption1 = randomBundleOption(bundleProduct1.id)
          val bundleSet1 = random[BundleSet].copy(
            id = UUID.randomUUID,
            minQuantity = 1,
            maxQuantity = 1,
            options = Seq(bundleOption1),
          )
          override lazy val bundleSets = Seq(bundleSet1)

          private val bundleOptionCreation1 =
            random[CartItemBundleOptionCreation].copy(
              bundleOptionId = bundleOption1.id,
              modifierOptions = Seq.empty,
              quantity = 1,
            )
          val bundleSetCreation =
            CartItemBundleSetCreation(bundleSetId = bundleSet1.id, bundleOptions = Seq(bundleOptionCreation1))
          override lazy val bundleSetCreations = Some(Seq(bundleSetCreation))

          implicit val coreAuthToken = ptCoreClient.generateAuthHeaderForCore
          PtCoreStubData.recordProduct(product)
          PtCoreStubData.recordProduct(bundleProduct1)

          Post(s"/v1/carts.add_product", creation)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("ProductOutOfStock")
          }
        }
      }
    }

    "if request has an invalid token" in {
      "reject the request" in new CartAddProductFSpecContext {
        Post(s"/v1/carts.add_product", creation)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
