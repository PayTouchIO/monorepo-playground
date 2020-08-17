package io.paytouch.core.clients.stripe

import akka.actor._
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.model._
import cats.implicits._
import io.paytouch.core._
import io.paytouch.core.clients.stripe.entities._
import io.paytouch.core.data.model.PaymentProcessorConfig
import io.paytouch.logging.BaseMdcActor

import scala.concurrent._

class StripeClient(
    val uri: Uri withTag StripeBaseUri,
    val secretKey: String withTag StripeSecretKey,
  )(implicit
    val mdcActor: ActorRef withTag BaseMdcActor,
    val system: ActorSystem,
  ) extends StripeHttpClient {
  def refundPaymentIntent(
      merchantConfig: PaymentProcessorConfig.Stripe,
      paymentIntentId: String,
      amount: Option[BigDecimal],
    ): Future[EitherStripeErrorOr[Refund]] = {
    val requestBody: RequestEntity =
      formData(
        Map(
          "payment_intent" -> paymentIntentId.some,
          "amount" -> amountInCents(amount),
          "expand[]" -> "charge".some,
          "refund_application_fee" -> "true".some,
        ),
      )

    sendAndReceive[Refund](Post(s"/v1/refunds", requestBody).withCredentials(merchantConfig))
  }

  protected def amountInCents(amount: Option[BigDecimal]): Option[String] =
    amount.map(a => (a * 100).toBigInt.toString)

  protected def formData(values: Map[String, Option[String]]) =
    FormData(values.flatMap { case (k, v) => v.map(k -> _) }).toEntity
}
