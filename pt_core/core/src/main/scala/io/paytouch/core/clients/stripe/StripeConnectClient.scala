package io.paytouch.core.clients.stripe

import java.util.UUID

import scala.concurrent._

import cats.implicits._

import akka.actor._
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.model._

import io.paytouch.core._
import io.paytouch.core.clients.stripe.entities._
import io.paytouch.core.entities.MonetaryAmount
import io.paytouch.core.services.StripeService
import io.paytouch.logging.BaseMdcActor

class StripeConnectClient(
    val uri: Uri withTag StripeConnectUri,
    val secretKey: String withTag StripeSecretKey,
  )(implicit
    val mdcActor: ActorRef withTag BaseMdcActor,
    val system: ActorSystem,
  ) extends StripeHttpClient {
  def connectCallback(
      connect: StripeService.ConnectRequest,
    ): Future[EitherStripeErrorOr[StripeService.ConnectResponse]] =
    sendAndReceive[StripeConnectClient.ConnectResponse] {
      Post(
        uri = s"/oauth/token",
        entity = FormData(
          Map(
            "grant_type" -> "authorization_code",
            "code" -> connect.code.value,
          ),
        ).toEntity,
      )
    }.map(_.map(_.toStripeServiceConnect))
}

object StripeConnectClient {
  final case class ConnectResponse(
      accessToken: String,
      refreshToken: String,
      scope: String,
      livemode: Boolean,
      tokenType: String,
      stripeUserId: String,
      stripePublishableKey: String,
    ) {
    def toStripeServiceConnect: StripeService.ConnectResponse =
      StripeService.ConnectResponse(
        accessToken = StripeService.Token.Access(accessToken),
        refreshToken = StripeService.Token.Refresh(refreshToken),
        liveMode = if (livemode) StripeService.LiveMode.Live else StripeService.LiveMode.Test,
        accountId = StripeService.AccountId(stripeUserId),
        publishableKey = StripeService.PublishableKey(stripePublishableKey),
      )
  }
}
