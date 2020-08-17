package io.paytouch.ordering.clients.stripe

import java.util.UUID

import com.typesafe.scalalogging.LazyLogging

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.model.{ FormData, Uri }
import akka.stream.Materializer

import io.paytouch.logging.BaseMdcActor
import io.paytouch.ordering.entities.MonetaryAmount
import io.paytouch.ordering.data.model.StripeConfig
import io.paytouch.ordering.clients.stripe.entities._
import io.paytouch.ordering.{ withTag, ServiceConfigurations }

import scala.concurrent.{ ExecutionContext, Future }
import scala.xml.{ Node, Utility, XML }

sealed trait StripeBaseUri

class StripeClient(
    val config: StripeClientConfig,
  )(implicit
    val mdcActor: ActorRef withTag BaseMdcActor,
    val system: ActorSystem,
    val materializer: Materializer,
  ) extends StripeHttpClient {
  def createPaymentIntent(
      merchantConfig: StripeConfig,
      orderId: UUID,
      cartId: Option[UUID] = None,
      total: MonetaryAmount,
      applicationFee: MonetaryAmount,
    ): Future[Wrapper[PaymentIntent]] = {
    val applicationFeeField: Seq[(String, String)] =
      if (applicationFee.cents > 0) Seq("application_fee_amount" -> applicationFee.cents.toString)
      else Seq.empty

    val fields = Seq(
      "amount" -> total.cents.toString,
      "currency" -> total.currency.toString,
      "metadata[order_id]" -> orderId.toString,
      "metadata[cart_id]" -> cartId.map(_.toString).getOrElse(""),
    ) ++ applicationFeeField

    val requestBody = FormData(fields: _*).toEntity

    sendAndReceive[PaymentIntent](Post(s"/v1/payment_intents", requestBody).withCredentials(merchantConfig))
  }
}
