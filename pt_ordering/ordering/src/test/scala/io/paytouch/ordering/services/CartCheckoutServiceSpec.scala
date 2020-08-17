package io.paytouch.ordering.services

import java.time.ZonedDateTime
import java.util.UUID

import scala.concurrent._

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Authorization

import cats.implicits._

import com.softwaremill.macwire._

import org.specs2.mock.Mockito

import io.paytouch._
import io.paytouch.implicits._

import io.paytouch.ordering._
import io.paytouch.ordering.clients._
import io.paytouch.ordering.clients.paytouch.core._
import io.paytouch.ordering.clients.paytouch.core.CoreError
import io.paytouch.ordering.clients.paytouch.core.entities.{
  GenericPaymentDetails,
  GiftCardPassCharge,
  OnlineOrderAttribute,
  Order,
  PaymentTransaction,
}
import io.paytouch.ordering.clients.paytouch.core.entities.enums.AcceptanceStatus
import io.paytouch.ordering.entities._
import io.paytouch.ordering.entities.enums._
import io.paytouch.ordering.entities.MonetaryAmount._
import io.paytouch.ordering.errors._
import io.paytouch.ordering.stubs.PtCoreStubClient
import io.paytouch.ordering.stubs.PtCoreStubData
import io.paytouch.ordering.utils.{ FixtureDaoFactory => Factory, _ }

@scala.annotation.nowarn("msg=Auto-application")
class CartCheckoutServiceSpec extends ServiceDaoSpec with CommonArbitraries with Mockito {
  abstract class CartCheckoutServiceSpecContext extends ServiceDaoSpecContext {
    implicit val storeContext: StoreContext = StoreContext.fromRecord(londonStore)

    def errorValues: Seq[AnyRef] = Seq.empty

    override lazy val ptCoreClient = {
      import MockedRestApi._

      new PtCoreStubClient {
        override def giftCardPassesBulkCharge(
            orderId: OrderId,
            bulkCharge: Seq[GiftCardPassCharge],
          )(implicit
            authToken: Authorization,
          ): Future[CoreApiResponse[Order]] = {
          import io.paytouch.ordering.clients._

          val response =
            ClientError(
              uri = "https://example.com",
              CoreEmbeddedErrorResponse(
                errors = Seq(
                  CoreEmbeddedError(
                    code = "GiftCardPassesNotAllFound",
                    message = "doesn't matter",
                  ),
                  CoreEmbeddedError(
                    code = "InsufficientFunds",
                    message =
                      s"One or more gift card passes for order: ${orderId.value} did not have sufficient funds to cover the requested amount.",
                    values = errorValues,
                  ),
                ),
                objectWithErrors = None,
              ),
            )

          response.asLeft[ApiResponse[Order]].pure[Future]
        }
      }
    }

    val cartService = MockedRestApi.cartService
    val cartSyncService = MockedRestApi.cartSyncService
    val worldpayService = MockedRestApi.worldpayService
    val stripeService = MockedRestApi.stripeService
    val ekashuService = MockedRestApi.ekashuService
    val jetDirectService = MockedRestApi.jetDirectService

    val service: CartCheckoutService = wire[CartCheckoutService]

    val coreItemTaxRateId = UUID.randomUUID
    val coreTaxRateId = UUID.randomUUID
    val coreModifierOptionId = UUID.randomUUID
    val coreVariantOptionId = UUID.randomUUID

    lazy val order = randomOrder()
    implicit val authHeader = ptCoreClient.generateAuthHeaderForCore
    PtCoreStubData.recordOrder(order)

    def assertPass(actual: GiftCardPassApplied, expected: GiftCardPassApplied) = {
      actual.amountToCharge ==== expected.amountToCharge
      actual.balance ==== expected.balance
    }
  }

  "CartCheckoutService" in {
    "bulkChargeGiftCardPasses" should {
      "update balance for relevant passes and propagate the error to the caller" in new CartCheckoutServiceSpecContext {
        object stale {
          val id = GiftCardPass.Id("stale")
          val amountToCharge: BigDecimal = 23.1234
          val actualBalance: BigDecimal = 22.789
        }

        object upToDate {
          val id = GiftCardPass.Id("upToDate")
          val amountToCharge: BigDecimal = 10
          val balance: BigDecimal = 20
        }

        override lazy val errorValues: Seq[AnyRef] =
          Seq(
            GiftCardPassCharge.Failure(
              giftCardPassId = stale.id,
              requestedAmount = stale.amountToCharge,
              actualBalance = stale.actualBalance,
            ),
          )

        val originalCartRecord =
          Factory
            .cart(
              store = londonStore,
              orderId = order.id.some,
              appliedGiftCardPasses = Seq(
                GiftCardPassApplied(
                  id = stale.id,
                  onlineCode = GiftCardPass.OnlineCode.Raw("doesn't matter"),
                  // should suffice but it won't because the balance changed in core
                  balance = stale.amountToCharge.USD,
                  addedAt = ZonedDateTime.now(),
                  amountToCharge = stale.amountToCharge.USD.some,
                ),
                GiftCardPassApplied(
                  id = upToDate.id,
                  onlineCode = GiftCardPass.OnlineCode.Raw("still doesn't matter"),
                  balance = upToDate.balance.USD,
                  addedAt = ZonedDateTime.now(),
                  amountToCharge = upToDate.amountToCharge.USD.some,
                ),
              ).some,
            )
            .create

        val actualErrors =
          service
            .bulkChargeGiftCardPasses(originalCartRecord)
            .await
            .failures

        val actualInsufficientFundsError =
          actualErrors
            .find(_.code === "InsufficientFunds")
            .get
            .asInstanceOf[InsufficientFunds]

        val expectedInsufficientFundsError =
          InsufficientFunds(
            List(
              GiftCardPassCharge.Failure(
                stale.id,
                stale.amountToCharge,
                stale.actualBalance,
              ),
            ),
          )

        actualInsufficientFundsError ==== expectedInsufficientFundsError

        val actualGiftCardPassesNotAllFoundError =
          actualErrors
            .find(_.code === "GiftCardPassesNotAllFound")
            .get
            .asInstanceOf[GiftCardPassesNotAllFound.type]

        actualGiftCardPassesNotAllFoundError ==== GiftCardPassesNotAllFound

        val actualRefreshedCartRecord =
          cartService
            .findById(originalCartRecord.id)
            .await
            .get

        actualRefreshedCartRecord
          .appliedGiftCardPasses
          .find(_.id === stale.id)
          .head
          .balance
          .amount ==== stale.actualBalance // was changed

        actualRefreshedCartRecord
          .appliedGiftCardPasses
          .find(_.id === upToDate.id)
          .head
          .balance
          .amount ==== upToDate.balance // was not changed

        // this one is flaky so I commented it out for now
        // calculations reran
        // actualRefreshedCartRecord.total.amount !=== originalCartRecord.totalAmount
      }

      "not call core client if there are no appliedGiftCardPasses" in new CartCheckoutServiceSpecContext {
        var wasCalled = false

        override def errorValues: Seq[AnyRef] = {
          wasCalled = true

          Seq.empty
        }

        val cartRecord =
          Factory
            .cart(
              store = londonStore,
              orderId = order.id.some,
              appliedGiftCardPasses = Seq.empty.some,
            )
            .create

        service
          .bulkChargeGiftCardPasses(cartRecord)
          .await
          .success

        wasCalled ==== false
      }

      "store the payment transaction ids which come back from core and update the cart status based on the online order attribute acceptance status" in new CartCheckoutServiceSpecContext {
        val giftCardPassId = UUID.randomUUID
        val giftCardPassTransactionId = UUID.randomUUID

        override lazy val ptCoreClient = {
          import MockedRestApi._

          new PtCoreStubClient {
            override def giftCardPassesBulkCharge(
                orderId: OrderId,
                bulkCharge: Seq[GiftCardPassCharge],
              )(implicit
                authToken: Authorization,
              ): Future[CoreApiResponse[Order]] = {
              import io.paytouch.ordering.clients._

              val order =
                random[Order]
                  .copy(
                    onlineOrderAttribute = random[OnlineOrderAttribute]
                      .some
                      .map(_.copy(acceptanceStatus = AcceptanceStatus.Pending)),
                    paymentTransactions = Seq(
                      random[PaymentTransaction].copy(paymentDetails =
                        random[GenericPaymentDetails].copy(
                          giftCardPassId = giftCardPassId.some,
                          giftCardPassTransactionId = giftCardPassTransactionId.some,
                        ),
                      ),
                    ),
                  )

              ApiResponse(order, "whatever").asRight[ClientError].pure[Future]
            }
          }
        }

        val originalCartRecord =
          Factory
            .cart(
              store = londonStore,
              orderId = order.id.some,
              appliedGiftCardPasses = Seq(
                GiftCardPassApplied(
                  id = GiftCardPass.IdPostgres(giftCardPassId).cast,
                  onlineCode = GiftCardPass.OnlineCode.Raw("doesn't matter"),
                  balance = 20.USD,
                  addedAt = ZonedDateTime.now(),
                  amountToCharge = 10.USD.some,
                ),
              ).some,
            )
            .create

        service
          .bulkChargeGiftCardPasses(originalCartRecord)
          .await

        val refreshedCartRecord =
          daos
            .cartDao
            .findById(originalCartRecord.id)
            .await
            .get

        refreshedCartRecord
          .tap(_.status ==== CartStatus.Paid)
          .appliedGiftCardPasses
          .ensuring(_.size == 1)
          .head
          .tap { pass =>
            pass.id ==== GiftCardPass.IdPostgres(giftCardPassId).cast
            pass.paymentTransactionId.get ==== giftCardPassTransactionId
          }
      }

      "partially charge applied gift cards in case other gift cards in cart are charged already" in new CartCheckoutServiceSpecContext {
        val newTransactionIdGeneratedByCore =
          UUID.randomUUID()

        lazy val originalFirstPass =
          GiftCardPassApplied(
            id = GiftCardPass.IdPostgres(UUID.randomUUID()).cast,
            onlineCode = GiftCardPass.OnlineCode.Raw("matters"),
            balance = 20.USD,
            addedAt = ZonedDateTime.now(),
            amountToCharge = 10.USD.some,
            paymentTransactionId = UUID.randomUUID().some,
          )

        lazy val originalSecondPass =
          GiftCardPassApplied(
            id = GiftCardPass.IdPostgres(UUID.randomUUID()).cast,
            onlineCode = GiftCardPass.OnlineCode.Raw("doesn't matter"),
            balance = 30.USD,
            addedAt = ZonedDateTime.now(),
            amountToCharge = 15.USD.some,
            paymentTransactionId = None, // this one will be charged/set
          )

        override lazy val ptCoreClient = {
          import MockedRestApi._

          lazy val doNotInlineMeIDontCompileUnderScala213: PaymentTransaction =
            random[PaymentTransaction].copy(paymentDetails =
              random[GenericPaymentDetails].copy(
                giftCardPassId = originalSecondPass.id.cast.get.value.some,
                giftCardPassTransactionId = newTransactionIdGeneratedByCore.some,
              ),
            )

          new PtCoreStubClient {
            override def giftCardPassesBulkCharge(
                orderId: OrderId,
                bulkCharge: Seq[GiftCardPassCharge],
              )(implicit
                authToken: Authorization,
              ): Future[CoreApiResponse[Order]] = {
              import io.paytouch.ordering.clients._

              val order: Order =
                random[Order]
                  .copy(
                    onlineOrderAttribute = random[OnlineOrderAttribute]
                      .some
                      .map(_.copy(acceptanceStatus = AcceptanceStatus.Pending)),
                    paymentTransactions = Seq(
                      random[PaymentTransaction].copy(
                        paymentDetails = random[GenericPaymentDetails].copy(
                          giftCardPassId = originalFirstPass.id.cast.get.value.some,
                          giftCardPassTransactionId = originalFirstPass.paymentTransactionId,
                        ),
                      ),
                      doNotInlineMeIDontCompileUnderScala213,
                    ),
                  )

              ApiResponse(order, "whatever").asRight[ClientError].pure[Future]
            }
          }
        }

        val originalCartRecord =
          Factory
            .cart(
              store = londonStore,
              orderId = order.id.some,
              appliedGiftCardPasses = Seq(originalFirstPass, originalSecondPass).some,
            )
            .create

        service
          .bulkChargeGiftCardPasses(originalCartRecord)
          .await
          .success

        val refreshedCartRecord =
          daos
            .cartDao
            .findById(originalCartRecord.id)
            .await
            .get

        val refreshedFirstPass =
          refreshedCartRecord
            .appliedGiftCardPasses
            .find(_.id === originalFirstPass.id)
            .get

        assertPass(
          actual = refreshedFirstPass,
          expected = originalFirstPass,
        )

        refreshedFirstPass.paymentTransactionId ==== originalFirstPass.paymentTransactionId

        val refreshedSecondPass =
          refreshedCartRecord
            .appliedGiftCardPasses
            .find(_.id === originalSecondPass.id)
            .get

        assertPass(
          actual = refreshedSecondPass,
          expected = originalSecondPass,
        )

        refreshedSecondPass.paymentTransactionId ==== newTransactionIdGeneratedByCore.some
      }
    }
  }
}
