package io.paytouch.ordering.entities.stripe

import cats.implicits._
import cats.data._

import io.paytouch.ordering.clients.stripe.entities._
import io.paytouch.ordering.entities.enums.PaymentProcessor
import io.paytouch.ordering.errors._
import io.paytouch.ordering.json.JsonSupport

sealed abstract class WebhookType(val code: String) extends scala.Product with Serializable

object WebhookType {
  case object PaymentIntentSucceeded extends WebhookType("payment_intent.succeeded")
  case object PaymentIntentPaymentFailed extends WebhookType("payment_intent.payment_failed")
}

sealed abstract class Event(val code: String) extends scala.Product with Serializable {
  def id: String
  def livemode: Livemode
  def data: StripeEntity
}

object Event extends JsonSupport {
  final case class PaymentIntentSucceededEvent(
      id: String,
      livemode: Livemode,
      data: PaymentIntent,
    ) extends Event("payment_intent.succeeded")

  final case class PaymentIntentPaymentFailedEvent(
      id: String,
      livemode: Livemode,
      data: PaymentIntent,
    ) extends Event("payment_intent.payment_failed")

  def apply(webhook: StripeWebhook): Either[Error, Event] =
    webhook.`type` match {
      case WebhookType.PaymentIntentSucceeded.code =>
        parseData[PaymentIntent](webhook)
          .map(PaymentIntentSucceededEvent(webhook.id, webhook.livemode, _))

      case WebhookType.PaymentIntentPaymentFailed.code =>
        parseData[PaymentIntent](webhook)
          .map(PaymentIntentPaymentFailedEvent(webhook.id, webhook.livemode, _))

      case _ =>
        Left(UnhandledStripeWebhookType(webhook.`type`))
    }

  private def parseData[T](webhook: StripeWebhook)(implicit m: Manifest[T]): Either[DataError, T] =
    Either.catchNonFatal(fromJsonToEntity[T](webhook.data.`object`)).leftMap {
      case ex: Throwable => PaymentProcessorUnparsableMandatoryField(PaymentProcessor.Stripe, "object.data", None, ex)
    }
}
