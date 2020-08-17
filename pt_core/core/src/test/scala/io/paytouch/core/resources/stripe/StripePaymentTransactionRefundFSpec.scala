package io.paytouch.core.resources.stripe

import java.time.LocalTime
import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.clients.stripe.entities.Refund
import io.paytouch.core.data.model.PaymentProcessorConfig
import io.paytouch.core.data.model.enums._
import io.paytouch.core.entities.{ Discount => DiscountEntity, _ }
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

import scala.util.Random

class StripePaymentTransactionsRefundFSpec extends StripeFSpec {

  abstract class StripePaymentTransactionsRefundFSpecContext extends StripeFSpecContext {
    lazy val intentId = "intent1234"
    lazy val chargeId = "charge1234"

    override lazy val merchant = Factory
      .merchant(
        paymentProcessor = Some(PaymentProcessor.Stripe),
        paymentProcessorConfig = Some(
          PaymentProcessorConfig.Stripe(
            accessToken = "access-token",
            refreshToken = "refresh-token",
            liveMode = false,
            accountId = "account1234",
            publishableKey = "publishable-key",
          ),
        ),
      )
      .create

    lazy val order = Factory.order(merchant, Some(london)).create
    lazy val paymentTransaction =
      Factory
        .paymentTransaction(
          order,
          Seq.empty,
          `type` = Some(TransactionType.Payment),
          paymentProcessor = Some(TransactionPaymentProcessor.Stripe),
          paymentDetails = Some(
            PaymentDetails(
              amount = Some(BigDecimal(14.95)),
              gatewayTransactionReference = Some(intentId),
              transactionReference = Some(chargeId),
            ),
          ),
        )
        .create
  }

  "POST /v1/vendor/stripe/payment_transactions.refund?payment_transaction_id=$" in {
    "if request has valid token" in {

      "initiates a refund for the full amount with the provider and returns the response" in new StripePaymentTransactionsRefundFSpecContext {
        Post(s"/v1/vendor/stripe/payment_transactions.refund?payment_transaction_id=${paymentTransaction.id}")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val response = responseAs[ApiResponse[Refund]]
          response.data.paymentIntent ==== intentId
          response.data.amount ==== 1234
        }
      }

      "initiates a refund for a partial amount with the provider and returns the response" in new StripePaymentTransactionsRefundFSpecContext {
        Post(
          s"/v1/vendor/stripe/payment_transactions.refund?payment_transaction_id=${paymentTransaction.id}&amount=4.95",
        ).addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val response = responseAs[ApiResponse[Refund]]
          response.data.paymentIntent ==== intentId
          response.data.amount ==== 495
        }
      }

      "rejections" should {
        "reject the request if the amount is greater than the paid amount" in new StripePaymentTransactionsRefundFSpecContext {
          Post(
            s"/v1/vendor/stripe/payment_transactions.refund?payment_transaction_id=${paymentTransaction.id}&amount=24.95",
          ).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("InvalidRefundAmount")
          }
        }

        "reject the request if the payment transaction is for a different payment processor" in new StripePaymentTransactionsRefundFSpecContext {
          override lazy val paymentTransaction =
            Factory
              .paymentTransaction(
                order,
                Seq.empty,
                `type` = Some(TransactionType.Payment),
                paymentProcessor = Some(TransactionPaymentProcessor.Worldpay),
              )
              .create
          Post(s"/v1/vendor/stripe/payment_transactions.refund?payment_transaction_id=${paymentTransaction.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("PaymentTransactionProcessorMismatch")
          }
        }

        "reject the request if the payment transaction is not a payment" in new StripePaymentTransactionsRefundFSpecContext {
          override lazy val paymentTransaction =
            Factory
              .paymentTransaction(
                order,
                Seq.empty,
                `type` = Some(TransactionType.PreauthPayment),
                paymentProcessor = Some(TransactionPaymentProcessor.Stripe),
              )
              .create
          Post(s"/v1/vendor/stripe/payment_transactions.refund?payment_transaction_id=${paymentTransaction.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("PaymentTransactionTypeMismatch")
          }
        }

        "reject the request if the transaction id is for a different merchant" in new StripePaymentTransactionsRefundFSpecContext {
          val competitor = Factory.merchant.create
          val competitorLocation = Factory.location(competitor).create
          override lazy val order = Factory.order(competitor, Some(competitorLocation)).create

          Post(s"/v1/vendor/stripe/payment_transactions.refund?payment_transaction_id=${paymentTransaction.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }
  }
}
