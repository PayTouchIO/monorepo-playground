package io.paytouch.ordering.stubs

import java.util.UUID

import scala.concurrent._

import akka.actor._
import akka.http.scaladsl.model.Uri
import akka.stream.Materializer

import io.paytouch.logging.BaseMdcActor
import io.paytouch.ordering._
import io.paytouch.ordering.clients.stripe._
import io.paytouch.ordering.clients.stripe.entities._
import io.paytouch.ordering.data.model.StripeConfig
import io.paytouch.ordering.entities.MonetaryAmount
import io.paytouch.ordering.entities.stripe.Livemode
import io.paytouch.ordering.logging.MdcActor
import io.paytouch.ordering.utils.CommonArbitraries

class StripeStubClient(
  )(implicit
    override val mdcActor: ActorRef withTag BaseMdcActor,
    override val system: ActorSystem,
    override val materializer: Materializer,
  ) extends StripeClient(StripeStubClient.config) {
  override def createPaymentIntent(
      merchantConfig: StripeConfig,
      orderId: UUID,
      cartId: Option[UUID],
      total: MonetaryAmount,
      applicationFee: MonetaryAmount,
    ): Future[Wrapper[PaymentIntent]] =
    Future.successful(Right(StripeStubData.randomPaymentIntent()))
}

object StripeStubData extends CommonArbitraries {
  @scala.annotation.nowarn("msg=Auto-application")
  def randomPaymentIntent(): PaymentIntent =
    random[PaymentIntent].copy(clientSecret = Some(genString.instance))
}

object StripeStubClient {
  val config: StripeClientConfig = {
    import StripeClientConfig._

    StripeClientConfig(
      ApplicationFeeBasePoints(40),
      BaseUri(Uri("https://example.com")),
      SecretKey("secret-key"),
      WebhookSecret("webhook-secret"),
      Livemode(false),
    )
  }
}
