package io.paytouch.ordering.resources.stripe

import java.time.Instant

import com.stripe.model.Event
import com.stripe.net.Webhook

import io.paytouch.ordering.errors.InvalidPaymentProcessorHashCodeResult
import io.paytouch.ordering.entities.enums.PaymentProcessor
import io.paytouch.ordering.stripe.StripeEncodings
import io.paytouch.ordering.utils.PaytouchSpec

import io.paytouch.ordering.clients.stripe.StripeClientConfig.WebhookSecret
import org.specs2.specification.Scope

class StripeEncodingsSpec extends PaytouchSpec with StripeEncodings {
  abstract class Context extends Scope {
    val payload = "{\"id\":\"evt1234\",\"object\":\"event\",\"data\":{}}"
    val secret = WebhookSecret("secret1234")
  }

  "StripeEncodings" should {
    "calculateSignatureHeader" should {
      "return the expected value" in new Context {
        val instant = Instant.parse("2020-04-23T12:00:00Z")
        val timestamp = instant.toEpochMilli / 1000

        val expected = s"t=$timestamp,v1=aff812704617edf5b346dd4c68a20465675262668559aede8d803ed0304a64f4"

        calculateSignatureHeader(
          payload,
          secret,
          timestamp,
        ) ==== expected
      }

      "returns a signature which Stripe accepts as valid" in new Context {
        val header = calculateSignatureHeader(
          payload,
          secret,
        )

        // This raises an exception if the signature is invalid
        val result: Event = Webhook
          .constructEvent(
            payload,
            header,
            secret.value,
          );

        result.getId() ==== "evt1234"
      }
    }

    "validateWebhookSignature" should {
      "accept a valid header" in new Context {
        val header = calculateSignatureHeader(
          payload,
          secret,
        )

        validateWebhookSignature(header, payload, secret) ==== Right(payload)
      }

      "reject an invalid header" in new Context {
        val header = s"t=$timestampNow,v1=not-valid"

        validateWebhookSignature(header, payload, secret) ==== Left(
          InvalidPaymentProcessorHashCodeResult(PaymentProcessor.Stripe, Some(header)),
        )
      }
    }
  }
}
