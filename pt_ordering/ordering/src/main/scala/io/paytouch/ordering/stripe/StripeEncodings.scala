package io.paytouch.ordering.stripe

import cats.implicits._

import com.stripe.exception.SignatureVerificationException
import com.stripe.net.Webhook

import com.typesafe.scalalogging.LazyLogging

import io.paytouch.ordering.clients.stripe._
import io.paytouch.ordering.clients.stripe.StripeClientConfig._
import io.paytouch.ordering.entities.enums.PaymentProcessor
import io.paytouch.ordering.entities.stripe._
import io.paytouch.ordering.errors._

trait StripeEncodings extends LazyLogging {
  protected def calculateSignatureHeader(
      payload: String,
      secret: WebhookSecret,
      timestamp: Long = timestampNow,
    ): String = {
    val payloadToSign = "%d.%s".format(timestamp, payload)
    val signature = Webhook.Util.computeHmacSha256(secret.value, payloadToSign)

    s"t=$timestamp,v1=$signature"
  }

  def validateWebhookSignature(
      header: String,
      payload: String,
      secret: WebhookSecret,
    ): Either[DataError, String] =
    Either
      .catchOnly[SignatureVerificationException] {
        // This raises an exception if the signature is invalid
        Webhook.constructEvent(payload, header, secret.value)

        payload
      }
      .leftMap { ex =>
        logger.info(s"Stripe signature verification failed: ${ex.getMessage} Header = $header")
        InvalidPaymentProcessorHashCodeResult(PaymentProcessor.Stripe, header.some)
      }

  protected def timestampNow: Long =
    System.currentTimeMillis() / 1000L
}
