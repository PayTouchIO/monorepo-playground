package io.paytouch.ordering.clients.stripe

import java.nio.charset.StandardCharsets
import java.time.ZonedDateTime
import java.util.{ Base64, Currency, UUID }

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.StatusCodes

import org.json4s.JsonAST._

import io.paytouch._
import io.paytouch.implicits._

import io.paytouch.ordering.clients.ClientSpec
import io.paytouch.ordering.clients.stripe._
import io.paytouch.ordering.clients.stripe.entities._
import io.paytouch.ordering.data.model.StripeConfig
import io.paytouch.ordering.entities.MonetaryAmount
import io.paytouch.ordering.entities.MonetaryAmount._
import io.paytouch.ordering.entities.stripe.Livemode
import io.paytouch.ordering.utils.{ Generators, FixtureDaoFactory => Factory, DefaultFixtures }

class StripeClientSpec extends ClientSpec {
  lazy val config: StripeClientConfig = {
    import StripeClientConfig._

    StripeClientConfig(
      ApplicationFeeBasePoints(40),
      BaseUri(uri),
      SecretKey("secret1234"),
      WebhookSecret("webhook-secret"),
      Livemode(true),
    )
  }

  abstract class StripeClientSpecContext
      extends StripeClient(config)
         with ClientSpecContext
         with Fixtures
         with Generators {
    lazy val merchantConfig =
      StripeConfig(
        accountId = genString.instance,
        publishableKey = genString.instance,
      )

    lazy val cartId = UUID.randomUUID
    lazy val orderId = UUID.randomUUID
    lazy val cartTotal = genMonetaryAmount.instance
    lazy val applicationFee = genMonetaryAmount.instance

    def assertRequest(
        request: HttpRequest,
        method: HttpMethod,
        path: String,
        stripeAccount: String,
        body: Option[String] = None,
      ) = {
      request.method ==== method
      request.uri.path.toString ==== path
      request
        .headers
        .find(h => h.name == "Authorization")
        .get
        .value ==== s"Basic ${Base64
        .getEncoder
        .encodeToString(s"${config.secretKey.value}:".getBytes(StandardCharsets.UTF_8))}"
      request
        .headers
        .find(h => h.name == "Stripe-Account")
        .get
        .value ==== stripeAccount

      body.map(request.entity.body ==== _)
    }
  }

  "StripeClient" should {
    "#createPaymentIntent" should {
      "create a payment intent" in new StripeClientSpecContext {
        val response = when(createPaymentIntent(merchantConfig, orderId, Some(cartId), cartTotal, applicationFee))
          .expectRequest(request =>
            assertRequest(
              request,
              HttpMethods.POST,
              s"/v1/payment_intents",
              merchantConfig.accountId,
              Some(
                Seq(
                  s"amount=${cartTotal.cents}",
                  s"currency=${cartTotal.currency}",
                  s"metadata%5Border_id%5D=${orderId}",
                  s"metadata%5Bcart_id%5D=${cartId}",
                  s"application_fee_amount=${applicationFee.cents}",
                ).mkString("&"),
              ),
            ),
          )
          .respondWith("/stripe/responses/payment_intent.json")

        response.await ==== Right(paymentIntent)
      }

      "doesn't send application_fee_amount if equals zero" in new StripeClientSpecContext {
        val response =
          when(createPaymentIntent(merchantConfig, orderId, Some(cartId), cartTotal, applicationFee = 0 USD))
            .expectRequest(request =>
              assertRequest(
                request,
                HttpMethods.POST,
                s"/v1/payment_intents",
                merchantConfig.accountId,
                Some(
                  Seq(
                    s"amount=${cartTotal.cents}",
                    s"currency=${cartTotal.currency}",
                    s"metadata%5Border_id%5D=${orderId}",
                    s"metadata%5Bcart_id%5D=${cartId}",
                  ).mkString("&"),
                ),
              ),
            )
            .respondWith("/stripe/responses/payment_intent.json")

        response.await ==== Right(paymentIntent)
      }

      "handle account invalid error" in new StripeClientSpecContext {
        val response = when(createPaymentIntent(merchantConfig, orderId, Some(cartId), cartTotal, applicationFee))
          .expectRequest(request =>
            assertRequest(
              request,
              HttpMethods.POST,
              s"/v1/payment_intents",
              merchantConfig.accountId,
            ),
          )
          .respondWith("/stripe/responses/payment_intent_account_invalid.json", status = StatusCodes.BadRequest)

        response.await ==== Left(
          StripeError(
            `type` = "invalid_request_error",
            message =
              "The provided key 'sk_test_47****************************6GsH' does not have access to account 'acct_1GZxhiG1fbFG85s0' (or that account does not exist). Application access may have been revoked.",
            code = "account_invalid",
            ex = None,
          ),
        )
      }

      "handle amount invalid error" in new StripeClientSpecContext {
        val response = when(createPaymentIntent(merchantConfig, orderId, Some(cartId), cartTotal, applicationFee))
          .expectRequest(request =>
            assertRequest(
              request,
              HttpMethods.POST,
              s"/v1/payment_intents",
              merchantConfig.accountId,
            ),
          )
          .respondWith("/stripe/responses/payment_intent_amount_invalid.json", status = StatusCodes.BadRequest)

        response.await ==== Left(
          StripeError(
            `type` = "invalid_request_error",
            message = "Amount must convert to at least 50 cents. $0.01 converts to approximately €0.00.",
            code = "amount_too_small",
            ex = None,
          ),
        )
      }

      "handle currency invalid error" in new StripeClientSpecContext {
        val response = when(createPaymentIntent(merchantConfig, orderId, Some(cartId), cartTotal, applicationFee))
          .expectRequest(request =>
            assertRequest(
              request,
              HttpMethods.POST,
              s"/v1/payment_intents",
              merchantConfig.accountId,
            ),
          )
          .respondWith("/stripe/responses/payment_intent_currency_invalid.json", status = StatusCodes.BadRequest)

        response.await ==== Left(
          StripeError(
            `type` = "invalid_request_error",
            message = "Invalid currency: ppp. Stripe currently supports these currencies: ...",
            code = "Unknown",
            ex = None,
          ),
        )
      }
    }
  }

  trait Fixtures {
    lazy val paymentIntent = PaymentIntent(
      id = "pi_1GZwKPJdRL7ojpuqxKijXaEE",
      amount = 10000,
      currency = Currency.getInstance("USD"),
      charges = PaymentIntentCharges(
        data = Seq.empty,
      ),
      status = PaymentIntentStatus.RequiresPaymentMethod,
      clientSecret = Some("pi_1GZwKPJdRL7ojpuqxKijXaEE_secret_dNHNxVvHKmujWrCwrXXH7w9nR"),
      metadata = Map(
        "orderId" -> "0d367fdc-2f55-4eaa-b866-0c6d28f08d6e",
        "cartId" -> "a9c19b59-15c5-453a-8a18-9a766fd3554d",
      ),
    )
  }
}
