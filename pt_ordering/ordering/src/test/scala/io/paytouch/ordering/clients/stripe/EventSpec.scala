package io.paytouch.ordering.clients.stripe

import io.paytouch.ordering.entities.stripe._
import io.paytouch.ordering.errors._
import io.paytouch.ordering.utils._

import org.json4s.JsonAST.JObject

import org.specs2.specification.Scope

class EventSpec extends PaytouchSpec {
  import EventSpec._

  abstract class Context extends Scope with Fixtures {}

  "Event" should {
    "parse a payment_intent.succeeded event" in new Context {
      val result = Event(paymentIntentSucceeded)
      result must beRight.like { case _: Event.PaymentIntentSucceededEvent => true }
    }

    "parse a payment_intent.payment_failed event" in new Context {
      val result = Event(paymentIntentPaymentFailed)
      result must beRight.like { case _: Event.PaymentIntentPaymentFailedEvent => true }
    }

    "return an error for a known event with unparseable data" in new Context {
      val result = Event(unparseableWebhook)
      result must beLeft.like { case err: Error if err.code === "PaymentProcessorUnparsableMandatoryField" => true }
    }

    "return an error for an unknown event" in new Context {
      val result = Event(otherWebhook)
      result must beLeft(UnhandledStripeWebhookType("other-type"))
    }
  }
}

object EventSpec {
  trait Fixtures extends FixturesSupport {
    lazy val paymentIntentSucceeded =
      loadJsonAs[StripeWebhook]("/stripe/webhooks/payment_intent_succeeded.json")

    lazy val paymentIntentPaymentFailed =
      loadJsonAs[StripeWebhook]("/stripe/webhooks/payment_intent_payment_failed.json")

    lazy val unparseableWebhook =
      StripeWebhook(
        id = "evt1234",
        livemode = Livemode(false),
        data = WebhookData(`object` = JObject()),
        `type` = "payment_intent.succeeded",
      )

    lazy val otherWebhook =
      StripeWebhook(
        id = "evt1234",
        livemode = Livemode(false),
        data = WebhookData(`object` = JObject()),
        `type` = "other-type",
      )
  }
}
