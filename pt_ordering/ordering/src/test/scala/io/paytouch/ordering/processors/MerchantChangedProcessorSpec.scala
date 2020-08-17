package io.paytouch.ordering.processors

import cats.implicits._

import java.util.UUID

import com.softwaremill.macwire._

import io.paytouch.ordering.data.model
import io.paytouch.ordering.entities
import io.paytouch.ordering.entities.enums
import io.paytouch.ordering.messages.entities.{ MerchantChanged, MerchantChangedData, MerchantPayload }
import io.paytouch.ordering.utils.{ MockedRestApi, MultipleLocationFixtures, FixtureDaoFactory => Factory }

class MerchantChangedProcessorSpec extends ProcessorSpec {
  abstract class MerchantChangedProcessorSpecContext extends ProcessorSpecContext {
    val merchantService = MockedRestApi.merchantService
    val storeService = MockedRestApi.storeService
    lazy val processor = wire[MerchantChangedProcessor]

    val merchantDao = daos.merchantDao
    val storeDao = daos.storeDao

    val merchantId = UUID.randomUUID
    val msg = MerchantChanged(
      merchantId,
      MerchantChangedData(
        displayName = "Carlbucks Coffee",
        paymentProcessor = enums.PaymentProcessor.Stripe,
      ),
      Some(
        MerchantPayload
          .OrderingPaymentProcessorConfigUpsertion
          .StripeConfigUpsertion(accountId = "account", publishableKey = "publishable"),
      ),
    )
  }

  "MerchantChangedProcessor" should {
    "new merchant" should {
      "creates a new merchant" in new MerchantChangedProcessorSpecContext {
        processor.execute(msg)

        afterAWhile {
          val merchantRecord = merchantDao.findById(merchantId).await.get
          merchantRecord.urlSlug ==== "carlbucks-coffee"
          merchantRecord.paymentProcessor ==== enums.PaymentProcessor.Stripe
          merchantRecord.paymentProcessorConfig ==== model.StripeConfig(
            accountId = "account",
            publishableKey = "publishable",
          )
        }
      }
    }

    "existing merchant" should {
      "not change the url slug" in new MerchantChangedProcessorSpecContext {
        val merchant = Factory.merchant(id = Some(merchantId), urlSlug = Some("carlos-coffee")).create
        processor.execute(msg)

        afterAWhile {
          val merchantRecord = merchantDao.findById(merchantId).await.get
          merchantRecord.urlSlug ==== merchant.urlSlug // unchanged for existing merchant
        }
      }

      "change the payment processor and enables payment methods on all stores if the merchant has no payment processor" in new MerchantChangedProcessorSpecContext {
        val merchant = Factory.merchant(id = Some(merchantId), paymentProcessor = None).create
        val store1 = Factory
          .store(
            merchant,
            catalogId = UUID.randomUUID,
            locationId = UUID.randomUUID,
            paymentMethods = Seq(
              entities.PaymentMethod(enums.PaymentMethodType.Cash, active = true),
            ).some,
          )
          .create

        val store2 = Factory
          .store(
            merchant,
            catalogId = UUID.randomUUID,
            locationId = UUID.randomUUID,
            paymentMethods = Seq(
              entities.PaymentMethod(enums.PaymentMethodType.Cash, active = false),
            ).some,
          )
          .create

        merchant.paymentProcessor ==== enums.PaymentProcessor.Paytouch
        merchant.paymentProcessorConfig ==== model.PaytouchConfig()

        processor.execute(msg)

        afterAWhile {
          val merchantRecord = merchantDao.findById(merchant.id).await.get
          merchantRecord.paymentProcessor ==== enums.PaymentProcessor.Stripe
          merchantRecord.paymentProcessorConfig ==== model.StripeConfig(
            accountId = "account",
            publishableKey = "publishable",
          )

          val enabledPaymentMethod = entities.PaymentMethod(enums.PaymentMethodType.Stripe, active = true)
          val store1Record = storeDao.findById(store1.id).await.get
          store1Record.paymentMethods should containTheSameElementsAs(
            store1.paymentMethods ++ Seq(enabledPaymentMethod),
          )
          val store2Record = storeDao.findById(store2.id).await.get
          store2Record.paymentMethods should containTheSameElementsAs(
            store2.paymentMethods ++ Seq(enabledPaymentMethod),
          )
        }
      }

      "change the payment processor if the merchant has stripe" in new MerchantChangedProcessorSpecContext {
        val merchant = Factory
          .merchant(
            id = Some(merchantId),
            paymentProcessor = Some(enums.PaymentProcessor.Stripe),
            paymentProcessorConfig = Some(model.StripeConfig(accountId = "old", publishableKey = "old")),
          )
          .create

        processor.execute(msg)

        afterAWhile {
          val merchantRecord = merchantDao.findById(merchant.id).await.get
          merchantRecord.paymentProcessor ==== enums.PaymentProcessor.Stripe
          merchantRecord.paymentProcessorConfig ==== model.StripeConfig(
            accountId = "account",
            publishableKey = "publishable",
          )
        }
      }

      "not change the payment processor if it is anything else" in new MerchantChangedProcessorSpecContext {
        val config = model.WorldpayConfig(
          accountId = "accountId",
          terminalId = "tid",
          acceptorId = "acceptorId",
          accountToken = "token",
        )
        val merchant = Factory
          .merchant(
            id = Some(merchantId),
            paymentProcessor = Some(enums.PaymentProcessor.Worldpay),
            paymentProcessorConfig = Some(config),
          )
          .create

        processor.execute(msg)

        afterAWhile {
          val merchantRecord = merchantDao.findById(merchant.id).await.get
          merchantRecord.paymentProcessor ==== enums.PaymentProcessor.Worldpay
          merchantRecord.paymentProcessorConfig ==== config
        }
      }
    }
  }
}
