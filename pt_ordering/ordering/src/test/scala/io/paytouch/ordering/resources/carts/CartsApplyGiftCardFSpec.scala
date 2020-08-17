package io.paytouch.ordering.resources.carts

import java.time.ZonedDateTime
import java.util.UUID

import scala.concurrent._

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._

import cats.implicits._

import org.scalacheck._

import org.specs2.matcher.MatchResult

import io.paytouch.implicits._

import io.paytouch.ordering.clients._
import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.data.model.CartUpdate
import io.paytouch.ordering.entities._
import io.paytouch.ordering.entities.MonetaryAmount._
import io.paytouch.ordering.errors.ClientError
import io.paytouch.ordering.resources.CartResource
import io.paytouch.ordering.stubs.PtCoreStubClient
import io.paytouch.ordering.stubs.PtCoreStubData
import io.paytouch.ordering.utils.{ FixtureDaoFactory => Factory, _ }

class CartsApplyGiftCardFSpec extends CartItemOpsFSpec {
  import CartsApplyGiftCardFSpec._

  abstract class CartApplyGiftCardFSpecContext extends CartItemOpsFSpecContext with ProductFixtures {
    implicit val coreAuthToken = ptCoreClient.generateAuthHeaderForCore

    def actualAppliedPasses: Seq[GiftCardPassApplied] =
      responseAs[ApiResponse[Cart]]
        .data
        .appliedGiftCardPasses

    def assert(
        actual: GiftCardPassApplied,
        expectedData: PtCoreStubData.OnlineCodeWithGiftCardPass,
      ): MatchResult[Any] = {
      actual.id ==== expectedData.giftCardPass.id
      actual.onlineCode ==== expectedData.onlineCode
      actual.balance ==== expectedData.giftCardPass.balance
    }
  }

  "POST /v1/carts.apply_gift_card" in {
    "if request has valid token" in {
      "good" in {
        "apply gift card if it is not used" in new CartApplyGiftCardFSpecContext {
          val coreResponse = {
            val r = actuallyRandom()

            r.copy(giftCardPass = r.giftCardPass.copy(balance = 10.USD))
          }

          PtCoreStubData.recordGiftCardPass(coreResponse)

          val body =
            CartResource.ApplyGiftCard(coreResponse.onlineCode)

          Post(s"/v1/carts.apply_gift_card?cart_id=${cart.id}", body)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatusOK()

            assert(
              actual = actualAppliedPasses.head,
              expectedData = coreResponse,
            )
          }
        }

        "NOT apply gift card TWICE" in new CartApplyGiftCardFSpecContext {
          val coreResponse = {
            val r = actuallyRandom()

            r.copy(giftCardPass = r.giftCardPass.copy(balance = 10.USD))
          }

          PtCoreStubData.recordGiftCardPass(coreResponse)

          val body =
            CartResource.ApplyGiftCard(coreResponse.onlineCode)

          Post(s"/v1/carts.apply_gift_card?cart_id=${cart.id}", body)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatusOK()

            assert(
              actual = actualAppliedPasses.head,
              expectedData = coreResponse,
            )
          }

          Post(s"/v1/carts.apply_gift_card?cart_id=${cart.id}", body)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatusOK()

            assert(
              actual = actualAppliedPasses.ensuring(_.size ==== 1).head,
              expectedData = coreResponse,
            )
          }
        }

        "apply gift cards and return them in ascending order and also unapply one of them and attempt to unapply the other one but fail" in new CartApplyGiftCardFSpecContext {
          val coreResponse1 = {
            val r = actuallyRandom()

            r.copy(giftCardPass = r.giftCardPass.copy(balance = 10.USD))
          }

          PtCoreStubData.recordGiftCardPass(coreResponse1)

          val body1 =
            CartResource.ApplyGiftCard(coreResponse1.onlineCode)

          Post(s"/v1/carts.apply_gift_card?cart_id=${cart.id}", body1)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatusOK()

            assert(
              actual = actualAppliedPasses.head,
              expectedData = coreResponse1,
            )
          }

          val coreResponse2 =
            PtCoreStubData.OnlineCodeWithGiftCardPass(
              onlineCode = io.paytouch.GiftCardPass.OnlineCode.Raw(Arbitrary.arbitrary[String].instance),
              giftCardPass = GiftCardPass(
                id = io.paytouch.GiftCardPass.IdPostgres(UUID.randomUUID()).cast,
                balance = 5.USD,
              ),
            )

          PtCoreStubData.recordGiftCardPass(coreResponse2)

          val body2 =
            CartResource.ApplyGiftCard(coreResponse2.onlineCode)

          Post(s"/v1/carts.apply_gift_card?cart_id=${cart.id}", body2)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatusOK()

            assert(
              actual = actualAppliedPasses.head,
              expectedData = coreResponse1,
            )

            assert(
              actual = actualAppliedPasses.tail.head,
              expectedData = coreResponse2,
            )

            actualAppliedPasses ==== actualAppliedPasses.sortBy(_.addedAt)
          }

          val unapplyBody1 =
            CartResource.UnapplyGiftCard(body1.onlineCode)

          // unapply the first gift card
          Post(s"/v1/carts.unapply_gift_card?cart_id=${cart.id}", unapplyBody1)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatusOK()

            assert( // first gift card was unapplied so the second one is .head of the list
              actual = actualAppliedPasses.head,
              expectedData = coreResponse2,
            )

            actualAppliedPasses ==== actualAppliedPasses.sortBy(_.addedAt)

            actualAppliedPasses.size ==== 1
          }

          def findTheOnlyRemainingOne =
            daos
              .cartDao
              .findById(cart.id)
              .await
              .get
              .appliedGiftCardPasses
              .head

          // fake the fact that the remaining pass was charged by setting the paymentTransactionId
          val appliedPassCharged =
            findTheOnlyRemainingOne
              .copy(paymentTransactionId = UUID.randomUUID().some)

          daos
            .cartDao
            .upsert(
              CartUpdate
                .empty
                .copy(
                  id = cart.id.some,
                  appliedGiftCardPasses = Seq(
                    appliedPassCharged,
                  ).some,
                ),
            )
            .await

          val unapplyBody2 =
            CartResource.UnapplyGiftCard(body2.onlineCode)

          // attempt to unapply the remaining gift card
          Post(s"/v1/carts.unapply_gift_card?cart_id=${cart.id}", unapplyBody2)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("GiftCardPassAlreadyCharged")

            val actual = findTheOnlyRemainingOne
            val expected = appliedPassCharged

            // it should remain untouched since it has already been charged
            actual ==== expected
          }
        }
      }

      "bad" should {
        "error out if it is used" in new CartApplyGiftCardFSpecContext {
          val coreResponse = {
            val r = actuallyRandom()

            r.copy(giftCardPass = r.giftCardPass.copy(balance = 0.USD)) // 0 means used
          }

          PtCoreStubData.recordGiftCardPass(coreResponse)

          val body =
            CartResource.ApplyGiftCard(coreResponse.onlineCode)

          Post(s"/v1/carts.apply_gift_card?cart_id=${cart.id}", body)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("GiftCardPassIsUsedUp")
          }
        }

        "error out if it doesn't exist" in new CartApplyGiftCardFSpecContext {
          val body =
            CartResource.ApplyGiftCard(io.paytouch.GiftCardPass.OnlineCode.Raw("does not exit"))

          Post(s"/v1/carts.apply_gift_card?cart_id=${cart.id}", body)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("GiftCardPassByOnlineCodeNotFound")
          }
        }
      }
    }
  }
}

object CartsApplyGiftCardFSpec extends CommonArbitraries {
  import org.scalacheck.ScalacheckShapeless._

  val arbitrary: Gen[PtCoreStubData.OnlineCodeWithGiftCardPass] =
    Arbitrary.arbitrary[PtCoreStubData.OnlineCodeWithGiftCardPass]

  def actuallyRandom(): PtCoreStubData.OnlineCodeWithGiftCardPass =
    arbitrary.instance
}
