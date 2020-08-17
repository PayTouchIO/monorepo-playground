package io.paytouch.ordering.services

import java.util.UUID

import scala.concurrent._

import akka.http.scaladsl.model.Uri

import com.softwaremill.macwire._

import org.specs2.mock.Mockito

import io.paytouch.ordering.clients.stripe._
import io.paytouch.ordering.clients.stripe.entities._
import io.paytouch.ordering.clients.stripe.StripeClientConfig.WebhookSecret
import io.paytouch.ordering.data.model.{ CartRecord, StripeConfig }
import io.paytouch.ordering.entities.{ MonetaryAmount, StoreContext }
import io.paytouch.ordering.entities.stripe._
import io.paytouch.ordering.entities.stripe.Livemode
import io.paytouch.ordering.errors.DataError
import io.paytouch.ordering.stubs.{ StripeStubClient, StripeStubData }
import io.paytouch.ordering.stripe.StripeEncodings
import io.paytouch.ordering.utils._

@scala.annotation.nowarn("msg=Auto-application")
class StripeServiceSpec extends ServiceDaoSpec with Mockito with CommonArbitraries {
  abstract class StripeServiceSpecContext extends ServiceDaoSpecContext {
    val stripeClient = mock[StripeClient]
    val stripeConfig = random[StripeConfig]
    val stripeClientConfig = StripeStubClient.config

    val cartSyncService = MockedRestApi.cartSyncService
    val service = wire[StripeService]
  }

  "StripeService" in {
    "createPaymentIntent" should {
      "calculate the proper application fee" in new StripeServiceSpecContext {
        val cart = random[CartRecord].copy(orderId = Some(UUID.randomUUID), totalAmount = 10)
        val merchantConfig = random[StripeConfig]
        val storeContext = random[StoreContext]
        stripeClient.createPaymentIntent(any, any, any, any, any) returns Future.successful(
          Right(StripeStubData.randomPaymentIntent()),
        )

        service.createPaymentIntent(cart, merchantConfig)(storeContext).await.success

        val expectedApplicationFee = MonetaryAmount(0.04, cart.currency)

        there was one(stripeClient).createPaymentIntent(
          stripeConfig,
          cart.orderId.get,
          Some(cart.id),
          MonetaryAmount(cart.totalAmount, cart.currency),
          expectedApplicationFee,
        )
      }

      "if livemode does not match the configured one" should {
        "return success to Stripe and NOT handle the PaymentIntent events" in new StripeServiceSpecContext
          with EventSpec.Fixtures {
          val fakeService = spy(service)

          fakeService.encodings returns new StripeEncodings {
            override def validateWebhookSignature(
                header: String,
                payload: String,
                secret: WebhookSecret,
              ): Either[DataError, String] =
              Right("I'm always right :)")
          }

          fakeService.processWebhook(
            signature = random[String],
            payload = random[String],
            webhook = paymentIntentSucceeded
              .copy(
                livemode = Livemode(true), // this will be compared against false in reference.conf
              ),
          )

          there was no(fakeService).handlePaymentIntentSucceeded(any)
          there was no(fakeService).handlePaymentIntentPaymentFailed(any)
        }
      }
    }
  }
}
