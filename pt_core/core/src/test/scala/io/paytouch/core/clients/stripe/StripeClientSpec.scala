package io.paytouch.core.clients.stripe

import java.nio.charset.StandardCharsets
import java.time.ZonedDateTime
import java.util.{ Base64, Currency }

import akka.http.scaladsl.model.{ HttpMethod, HttpMethods, HttpRequest }
import io.paytouch.core.{ StripeBaseUri, StripeSecretKey }
import io.paytouch.core.clients.ClientSpec
import io.paytouch.core.clients.stripe.entities._
import io.paytouch.core.data.model.PaymentProcessorConfig
import io.paytouch.core.utils.FixturesSupport
import io.paytouch.utils.Tagging._
import org.json4s.JsonAST._

class StripeClientSpec extends ClientSpec {
  val secretKey = "secret1234"

  abstract class StripeClientSpec
      extends StripeClient(
        uri.taggedWith[StripeBaseUri],
        secretKey.taggedWith[StripeSecretKey],
      )
         with ClientSpecContext
         with Fixtures
         with FixturesSupport {
    val merchantConfig = PaymentProcessorConfig.Stripe(
      accessToken = "access-token",
      refreshToken = "refresh-token",
      liveMode = false,
      accountId = "account1234",
      publishableKey = "publishable-key",
    )

    def assertRequest(
        request: HttpRequest,
        method: HttpMethod,
        path: String,
        stripeAccount: Option[String] = None,
        body: Option[String] = None,
      ) = {
      request.method ==== method
      request.uri.path.toString ==== path
      request.headers.find(h => h.name == "Authorization").get.value ==== s"Basic ${Base64
        .getEncoder
        .encodeToString(s"$secretKey:".getBytes(StandardCharsets.UTF_8))}"

      stripeAccount.map(account => request.headers.find(h => h.name == "Stripe-Account").get.value ==== account)

      body.map(b => s"$b&refund_application_fee=true").map(request.entity.body ==== _)
    }
  }

  "StripeClient" should {

    "#refundPaymentIntent" should {
      "refund a payment intent" in new StripeClientSpec {
        val response =
          when(refundPaymentIntent(merchantConfig, paymentIntentId = "intent1234", amount = Some(BigDecimal(14.95))))
            .expectRequest(request =>
              assertRequest(
                request,
                HttpMethods.POST,
                s"/v1/refunds",
                Some("account1234"),
                Some("payment_intent=intent1234&amount=1495&expand%5B%5D=charge"),
              ),
            )
            .respondWith("/stripe/responses/refund.json")

        response.await ==== Right(refund)
      }

      "refund a payment intent without amount" in new StripeClientSpec {
        val response = when(refundPaymentIntent(merchantConfig, paymentIntentId = "intent1234", amount = None))
          .expectRequest(request =>
            assertRequest(
              request,
              HttpMethods.POST,
              s"/v1/refunds",
              Some("account1234"),
              Some("payment_intent=intent1234&expand%5B%5D=charge"),
            ),
          )
          .respondWith("/stripe/responses/refund.json")

        response.await ==== Right(refund)
      }
    }
  }

  trait Fixtures {
    lazy val refund = Refund(
      id = "re_1GYty0JdRL7ojpuqR7isKw7P",
      amount = 2064,
      currency = Currency.getInstance("USD"),
      paymentIntent = "pi_1GYTdJJdRL7ojpuqFT8BJErO",
      status = "succeeded",
      charge = JObject(
        List(
          JField("id", JString("ch_1GYTdfJdRL7ojpuqdI2Mp9W1")),
          JField("object", JString("charge")),
          JField("amount", JInt(2064)),
          JField("amountRefunded", JInt(2064)),
          JField("application", JNull),
          JField("applicationFee", JNull),
          JField("applicationFeeAmount", JNull),
          JField("balanceTransaction", JString("txn_1GYTdfJdRL7ojpuq4t73gFZP")),
          JField(
            "billingDetails",
            JObject(
              List(
                JField(
                  "address",
                  JObject(
                    List(
                      JField("city", JNull),
                      JField("country", JNull),
                      JField("line1", JNull),
                      JField("line2", JNull),
                      JField("postalCode", JString("11111")),
                      JField("state", JNull),
                    ),
                  ),
                ),
                JField("email", JNull),
                JField("name", JNull),
                JField("phone", JNull),
              ),
            ),
          ),
          JField("calculatedStatementDescriptor", JString("Stripe")),
          JField("captured", JBool(true)),
          JField("created", JInt(1587027843)),
          JField("currency", JString("usd")),
          JField("customer", JNull),
          JField("description", JNull),
          JField("destination", JNull),
          JField("dispute", JNull),
          JField("disputed", JBool(false)),
          JField("failureCode", JNull),
          JField("failureMessage", JNull),
          JField("fraudDetails", JObject(List())),
          JField("invoice", JNull),
          JField("livemode", JBool(false)),
          JField("metadata", JObject(List())),
          JField("onBehalfOf", JNull),
          JField("order", JNull),
          JField(
            "outcome",
            JObject(
              List(
                JField("networkStatus", JString("approved_by_network")),
                JField("reason", JNull),
                JField("riskLevel", JString("normal")),
                JField("riskScore", JInt(24)),
                JField("sellerMessage", JString("Payment complete.")),
                JField("type", JString("authorized")),
              ),
            ),
          ),
          JField("paid", JBool(true)),
          JField("paymentIntent", JString("pi_1GYTdJJdRL7ojpuqFT8BJErO")),
          JField("paymentMethod", JString("pm_1GYTdeJdRL7ojpuqWFeSvrWu")),
          JField(
            "paymentMethodDetails",
            JObject(
              List(
                JField(
                  "card",
                  JObject(
                    List(
                      JField("brand", JString("visa")),
                      JField(
                        "checks",
                        JObject(
                          List(
                            JField("addressLine1Check", JNull),
                            JField("addressPostalCodeCheck", JString("pass")),
                            JField("cvcCheck", JString("pass")),
                          ),
                        ),
                      ),
                      JField("country", JString("US")),
                      JField("expMonth", JInt(4)),
                      JField("expYear", JInt(2024)),
                      JField("fingerprint", JString("TSLLp9Uk08QrLHv9")),
                      JField("funding", JString("credit")),
                      JField("installments", JNull),
                      JField("last4", JString("4242")),
                      JField("network", JString("visa")),
                      JField("threeDSecure", JNull),
                      JField("wallet", JNull),
                    ),
                  ),
                ),
                JField("type", JString("card")),
              ),
            ),
          ),
          JField("receiptEmail", JNull),
          JField("receiptNumber", JNull),
          JField(
            "receiptUrl",
            JString(
              "https://pay.stripe.com/receipts/acct_1GY4s6JdRL7ojpuq/ch_1GYTdfJdRL7ojpuqdI2Mp9W1/rcpt_H6h1pfj0KYOQHNAIgVMFJg2DGb3bM9k",
            ),
          ),
          JField("refunded", JBool(true)),
          JField(
            "refunds",
            JObject(
              List(
                JField("object", JString("list")),
                JField(
                  "data",
                  JArray(
                    List(
                      JObject(
                        List(
                          JField("id", JString("re_1GYty0JdRL7ojpuqR7isKw7P")),
                          JField("object", JString("refund")),
                          JField("amount", JInt(2064)),
                          JField("balanceTransaction", JString("txn_1GYty0JdRL7ojpuqHQqUeIfL")),
                          JField("charge", JString("ch_1GYTdfJdRL7ojpuqdI2Mp9W1")),
                          JField("created", JInt(1587129048)),
                          JField("currency", JString("usd")),
                          JField("metadata", JObject(List())),
                          JField("paymentIntent", JString("pi_1GYTdJJdRL7ojpuqFT8BJErO")),
                          JField("reason", JNull),
                          JField("receiptNumber", JNull),
                          JField("sourceTransferReversal", JNull),
                          JField("status", JString("succeeded")),
                          JField("transferReversal", JNull),
                        ),
                      ),
                    ),
                  ),
                ),
                JField("hasMore", JBool(false)),
                JField("totalCount", JInt(1)),
                JField("url", JString("/v1/charges/ch_1GYTdfJdRL7ojpuqdI2Mp9W1/refunds")),
              ),
            ),
          ),
          JField("review", JNull),
          JField("shipping", JNull),
          JField("source", JNull),
          JField("sourceTransfer", JNull),
          JField("statementDescriptor", JNull),
          JField("statementDescriptorSuffix", JNull),
          JField("status", JString("succeeded")),
          JField("transferData", JNull),
          JField("transferGroup", JNull),
        ),
      ),
    )

  }

}
