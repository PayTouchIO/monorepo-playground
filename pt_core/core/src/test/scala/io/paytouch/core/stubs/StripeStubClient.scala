package io.paytouch.core.stubs

import java.util.{ Currency, UUID }

import scala.concurrent._

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.model.Uri

import cats.implicits._

import org.json4s.JsonAST.JObject

import io.paytouch.core.{ StripeBaseUri, StripeConnectUri, StripeSecretKey }
import io.paytouch.core.clients.stripe._
import io.paytouch.core.clients.stripe.entities._
import io.paytouch.core.data.model.PaymentProcessorConfig
import io.paytouch.core.entities.MonetaryAmount
import io.paytouch.core.logging.MdcActor
import io.paytouch.logging.BaseMdcActor
import io.paytouch.utils.Tagging._
import io.paytouch.core.services.StripeService

class StripeStubClient(
    implicit
    override val mdcActor: ActorRef withTag BaseMdcActor,
    override val system: ActorSystem,
  ) extends StripeClient(Uri("http://example.com").taggedWith[StripeBaseUri], "secret".taggedWith[StripeSecretKey]) {

  override def refundPaymentIntent(
      merchantConfig: PaymentProcessorConfig.Stripe,
      paymentIntentId: String,
      amount: Option[BigDecimal],
    ): Future[EitherStripeErrorOr[Refund]] =
    StripeStubData
      .randomRefund(paymentIntentId, amount)
      .asRight
      .pure[Future]
}

object StripeStubData {
  def randomRefund(paymentIntentId: String, maybeAmount: Option[BigDecimal]): Refund =
    Refund(
      id = "re_1GYty0JdRL7ojpuqR7isKw7P",
      amount = maybeAmount.map(a => (a * 100).toBigInt).getOrElse(BigInt(1234)),
      currency = Currency.getInstance("USD"),
      paymentIntent = paymentIntentId,
      status = "succeeded",
      charge = JObject(),
    )

  val RandomConnectResponse: StripeService.ConnectResponse =
    StripeService.ConnectResponse(
      accessToken = StripeService.Token.Access("accessToken"),
      refreshToken = StripeService.Token.Refresh("refreshToken"),
      liveMode = StripeService.LiveMode.Live,
      accountId = StripeService.AccountId("accountId"),
      publishableKey = StripeService.PublishableKey("publishableKey"),
    )
}

class StripeConnectStubClient(
    implicit
    override val mdcActor: ActorRef withTag BaseMdcActor,
    override val system: ActorSystem,
  ) extends StripeConnectClient(
      Uri("http://example.com").taggedWith[StripeConnectUri],
      "secret".taggedWith[StripeSecretKey],
    ) {
  override def connectCallback(
      connect: StripeService.ConnectRequest,
    ): Future[Either[StripeError, StripeService.ConnectResponse]] =
    response(connect.code.value).pure[Future]

  private def response(code: String): Either[StripeError, StripeService.ConnectResponse] =
    if (code === "bad-code")
      StripeError().asLeft
    else
      StripeStubData.RandomConnectResponse.asRight
}
