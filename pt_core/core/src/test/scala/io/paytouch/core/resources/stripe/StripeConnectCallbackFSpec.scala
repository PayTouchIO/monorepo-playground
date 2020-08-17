package io.paytouch.core.resources.stripe

import akka.http.scaladsl.model.StatusCodes

import cats.implicits._

import io.paytouch.implicits._

import io.paytouch.core.data.model.enums.PaymentProcessor
import io.paytouch.core.data.model.PaymentProcessorConfig
import io.paytouch.core.services._
import io.paytouch.core.stubs.StripeStubData
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

import StripeConnectCallbackFSpec._

class StripeConnectCallbackFSpec extends StripeFSpec {
  abstract class StripeConnectCallbackFSpecContext extends StripeFSpecContext {
    lazy val NonEmptyStripeConfig: PaymentProcessorConfig =
      PaymentProcessorConfig.Stripe(
        accessToken = "qewr",
        refreshToken = "asdf",
        liveMode = random[Boolean],
        accountId = "zxcv",
        publishableKey = "poiu",
      )

    def stripeOrPaytouchConfig(shouldStripeConfigBeEmpty: Boolean) =
      IndexedSeq(
        random[PaymentProcessorConfig.Stripe],
        random[PaymentProcessorConfig.Paytouch],
      ).random.pipe { config =>
        if (config.paymentProcessor == PaymentProcessor.Stripe)
          if (shouldStripeConfigBeEmpty)
            PaymentProcessorConfig.Stripe.empty
          else
            NonEmptyStripeConfig
        else
          config
      }

    def notStripeAndNotPaytouchConfig =
      IndexedSeq(
        random[PaymentProcessorConfig.Creditcall],
        random[PaymentProcessorConfig.Jetpay],
        random[PaymentProcessorConfig.Worldpay],
      ).ensuring(
        cond = _.size === PaymentProcessor.values.size - StripeAndPayTouch.size,
        msg = "Add the missing PaymentProcessorConfig to this Seq.",
      ).random

    def assertProcessorAndConfig(expectedConfig: PaymentProcessorConfig): MatchResult[Any] = {
      val refreshedMerchant = daos.merchantDao.findById(merchant.id).await.get

      refreshedMerchant.paymentProcessorConfig ==== expectedConfig
      refreshedMerchant.paymentProcessor ==== expectedConfig.paymentProcessor
    }

    def assertProcessorAndConfig(
        expectedConfig: PaymentProcessorConfig,
        expectedProcessor: PaymentProcessor,
      ): MatchResult[Any] = {
      val refreshedMerchant = daos.merchantDao.findById(merchant.id).await.get

      refreshedMerchant.paymentProcessorConfig ==== expectedConfig
      refreshedMerchant.paymentProcessor ==== expectedProcessor
    }
  }

  "POST /v1/vendor/stripe/connect_callback" should {
    "set config" in {
      "if current paymentProcessor is Paytouch or Stripe with an EMPTY Stripe config" in new StripeConnectCallbackFSpecContext {
        override lazy val merchant = {
          val config = stripeOrPaytouchConfig(shouldStripeConfigBeEmpty = true)

          Factory
            .merchant(
              paymentProcessor = config.paymentProcessor.some,
              paymentProcessorConfig = config.some,
            )
            .create
        }
        val code = random[String]

        Post(s"/v1/vendor/stripe/connect_callback?code=$code")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NoContent)
          assertProcessorAndConfig(
            expectedConfig = StripeStubData
              .RandomConnectResponse
              .toPaymentProcessorConfig,
          )
        }
      }
    }

    "not set config" in {
      "if current paymentProcessor is Stripe with a NON EMPTY config" in new StripeConnectCallbackFSpecContext {
        override lazy val merchant = {
          val config = NonEmptyStripeConfig

          Factory
            .merchant(
              paymentProcessor = config.paymentProcessor.some,
              paymentProcessorConfig = config.some,
            )
            .create
        }
        val code = random[String]

        Post(s"/v1/vendor/stripe/connect_callback?code=$code")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NoContent)
          assertProcessorAndConfig(expectedConfig = merchant.paymentProcessorConfig)
        }
      }

      "if current paymentProcessor is other than Stripe or Paytouch" in
        new StripeConnectCallbackFSpecContext {
          override lazy val merchant = {
            val config = notStripeAndNotPaytouchConfig

            Factory
              .merchant(
                paymentProcessor = config.paymentProcessor.some,
                paymentProcessorConfig = config.some,
              )
              .create
          }

          val code = random[String]

          Post(s"/v1/vendor/stripe/connect_callback?code=$code")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("UnexpectedPaymentProcessor")
            assertProcessorAndConfig(expectedConfig = merchant.paymentProcessorConfig)
          }
        }

      "if current paymentProcessorConfig is other than Stripe or Paytouch" in
        new StripeConnectCallbackFSpecContext {
          lazy val processor = StripeOrPaytouch

          override lazy val merchant =
            Factory
              .merchant( // hopefully this will never happen
                paymentProcessor = processor.some,
                paymentProcessorConfig = notStripeAndNotPaytouchConfig.some,
              )
              .create

          val code = random[String]

          Post(s"/v1/vendor/stripe/connect_callback?code=$code")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("UnexpectedPaymentProcessorConfig")
            assertProcessorAndConfig(
              expectedConfig = merchant.paymentProcessorConfig,
              expectedProcessor = processor,
            )
          }
        }

      "if stripe does not recognize the code" in
        new StripeConnectCallbackFSpecContext {
          override lazy val merchant = {
            val config = stripeOrPaytouchConfig(shouldStripeConfigBeEmpty = true)

            Factory
              .merchant(
                paymentProcessor = config.paymentProcessor.some,
                paymentProcessorConfig = config.some,
              )
              .create
          }

          val code = "bad-code"

          Post(s"/v1/vendor/stripe/connect_callback?code=$code")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("StripeClientError")
            assertProcessorAndConfig(expectedConfig = merchant.paymentProcessorConfig)
          }
        }
    }
  }
}

object StripeConnectCallbackFSpec {
  val StripeAndPayTouch: Set[PaymentProcessor] =
    Set(PaymentProcessor.Stripe, PaymentProcessor.Paytouch)

  def StripeOrPaytouch: PaymentProcessor =
    StripeAndPayTouch.toIndexedSeq.random

  def notStripeAndNotPaytouch: PaymentProcessor =
    PaymentProcessor.values.filterNot(StripeAndPayTouch).random
}
