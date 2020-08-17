package io.paytouch.core.resources.merchants

import akka.http.scaladsl.model.StatusCodes

import cats.implicits._

import io.paytouch.implicits._

import io.paytouch.core.data.model.enums.PaymentProcessor
import io.paytouch.core.data.model.PaymentProcessorConfig
import io.paytouch.core.entities.WorldpayConfigUpsertion
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

import MerchantsSetWorldpayConfigFSpec._

class MerchantsSetWorldpayConfigFSpec extends MerchantsFSpec {
  abstract class MerchantsSetWorldpayConfigFSpecContext extends MerchantResourceFSpecContext {
    def worldpayOrPaytouchConfig =
      IndexedSeq(
        random[PaymentProcessorConfig.Worldpay],
        random[PaymentProcessorConfig.Paytouch],
      ).random

    def notWorldpayAndNotPaytouchConfig =
      IndexedSeq(
        random[PaymentProcessorConfig.Creditcall],
        random[PaymentProcessorConfig.Jetpay],
        random[PaymentProcessorConfig.Stripe],
      ).ensuring(
        cond = _.size === PaymentProcessor.values.size - WorldpayAndPayTouch.size,
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

  "POST /v1/merchants.set_worldpay_config" should {
    "set config" in {
      "if current paymentProcessor is Worldpay or Paytouch" in new MerchantsSetWorldpayConfigFSpecContext {
        override lazy val merchant = {
          val config = worldpayOrPaytouchConfig

          Factory
            .merchant(
              paymentProcessor = config.paymentProcessor.some,
              paymentProcessorConfig = config.some,
            )
            .create
        }

        val upsertion = random[WorldpayConfigUpsertion]

        Post(s"/v1/merchants.set_worldpay_config", upsertion)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NoContent)
          assertProcessorAndConfig(expectedConfig = upsertion.toPaymentProcessorConfig)
        }
      }
    }

    "not set config" in {
      "if current paymentProcessor is other than Worldpay or Paytouch" in
        new MerchantsSetWorldpayConfigFSpecContext {
          override lazy val merchant = {
            val config = notWorldpayAndNotPaytouchConfig

            Factory
              .merchant(
                paymentProcessor = config.paymentProcessor.some,
                paymentProcessorConfig = config.some,
              )
              .create
          }

          val upsertion = random[WorldpayConfigUpsertion]

          Post(s"/v1/merchants.set_worldpay_config", upsertion)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("UnexpectedPaymentProcessor")
            assertProcessorAndConfig(expectedConfig = merchant.paymentProcessorConfig)
          }
        }

      "if current paymentProcessorConfig is other than Worldpay or Paytouch" in
        new MerchantsSetWorldpayConfigFSpecContext {
          lazy val processor = worldpayOrPaytouch

          override lazy val merchant =
            Factory
              .merchant( // hopefully this will never happen
                paymentProcessor = processor.some,
                paymentProcessorConfig = notWorldpayAndNotPaytouchConfig.some,
              )
              .create

          val config = random[WorldpayConfigUpsertion]

          Post(s"/v1/merchants.set_worldpay_config", config)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("UnexpectedPaymentProcessorConfig")
            assertProcessorAndConfig(
              expectedConfig = merchant.paymentProcessorConfig,
              expectedProcessor = processor,
            )
          }
        }
    }
  }
}

object MerchantsSetWorldpayConfigFSpec {
  val WorldpayAndPayTouch: Set[PaymentProcessor] =
    Set(PaymentProcessor.Worldpay, PaymentProcessor.Paytouch)

  def worldpayOrPaytouch: PaymentProcessor =
    WorldpayAndPayTouch.toIndexedSeq.random

  def notWorldpayAndNotPaytouch: PaymentProcessor =
    PaymentProcessor.values.filterNot(WorldpayAndPayTouch).random
}
