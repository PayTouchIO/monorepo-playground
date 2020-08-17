package io.paytouch.core.services

import cats.implicits._

import com.softwaremill.macwire._

import io.paytouch._
import io.paytouch.implicits._

import io.paytouch.core.{ ServiceConfigurations => Config }
import io.paytouch.core.async.monitors.AuthenticationMonitor
import io.paytouch.core.async.sqs._
import io.paytouch.core.clients.stripe._
import io.paytouch.core.data.model.enums._
import io.paytouch.core.data._
import io.paytouch.core.data.model.MerchantRecord
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums._
import io.paytouch.core.entities.enums.MerchantSetupSteps._
import io.paytouch.core.expansions.MerchantExpansions
import io.paytouch.core.messages.entities.{ MerchantChanged, MerchantPayload }
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.stubs._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, _ }
import io.paytouch.logging.BaseMdcActor
import io.paytouch.utils.Tagging._

class StripeServiceSpec extends ServiceDaoSpec {
  abstract class StripeServiceSpecContext extends ServiceDaoSpecContext {
    implicit val logger = new PaytouchLogger

    val messageHandler = new SQSMessageHandler(actorSystem, actorMock.ref.taggedWith[SQSMessageSender])

    val bcryptRounds = Config.bcryptRounds
    val authenticationMonitor = actorMock.ref.taggedWith[AuthenticationMonitor]
    val authenticationService: AuthenticationService = wire[AuthenticationService]
    val adminService: AdminMerchantService = wire[AdminMerchantService]

    override val merchantService = wire[MerchantService]

    // We don't care about sample data being created for demo merchants
    override val sampleDataService = mock[SampleDataService]

    implicit val system = actorSystem
    implicit val mdc = actorMock.ref.taggedWith[BaseMdcActor]
    val stripeClient: StripeClient = wire[StripeStubClient]
    val stripeConnectClient: StripeConnectClient = wire[StripeConnectStubClient]
    val service = wire[StripeService]

    val merchantDao = daos.merchantDao

    lazy val NonEmptyStripeConfig: model.PaymentProcessorConfig =
      model
        .PaymentProcessorConfig
        .Stripe(
          accessToken = "qewr",
          refreshToken = "asdf",
          liveMode = random[Boolean],
          accountId = "zxcv",
          publishableKey = "poiu",
        )

    def stripeOrPaytouchConfig(shouldStripeConfigBeEmpty: Boolean) =
      IndexedSeq(
        random[model.PaymentProcessorConfig.Stripe],
        random[model.PaymentProcessorConfig.Paytouch],
      ).random.pipe { config =>
        if (config.paymentProcessor == PaymentProcessor.Stripe)
          if (shouldStripeConfigBeEmpty)
            model.PaymentProcessorConfig.Stripe.empty
          else
            NonEmptyStripeConfig
        else
          config
      }
  }

  "StripeService" in {
    "connectCallback" should {
      "sendMerchantChangedMessage" in new StripeServiceSpecContext {
        override lazy val merchant = {
          val config = stripeOrPaytouchConfig(shouldStripeConfigBeEmpty = true)

          Factory
            .merchant(
              paymentProcessor = config.paymentProcessor.some,
              paymentProcessorConfig = config.some,
            )
            .create
        }

        service.connectCallback(random[StripeService.ConnectRequest]).await.success

        val entity = merchantService.findById(merchant.id)(merchantService.defaultExpansions).await.get
        val reloadedMerchantRecord = merchantDao.findById(merchant.id).await.get
        actorMock.expectMsg(SendMsgWithRetry(MerchantChanged(entity, reloadedMerchantRecord.paymentProcessorConfig)))
      }

      "not sendMerchantChangedMessage for demo merchants" in new StripeServiceSpecContext {
        override lazy val merchant = {
          val config = stripeOrPaytouchConfig(shouldStripeConfigBeEmpty = true)

          Factory
            .merchant(
              mode = MerchantMode.Demo.some,
              paymentProcessor = config.paymentProcessor.some,
              paymentProcessorConfig = config.some,
            )
            .create
        }

        service.connectCallback(random[StripeService.ConnectRequest]).await.success

        actorMock.expectNoMessage()
      }
    }
  }
}
