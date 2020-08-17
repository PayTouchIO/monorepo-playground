package io.paytouch.ordering.clients.stripe

import akka.http.scaladsl.model.Uri

import io.paytouch._
import io.paytouch.ordering.entities.stripe.Livemode

import StripeClientConfig._

final case class StripeClientConfig(
    applicationFeeBasePoints: ApplicationFeeBasePoints,
    baseUri: BaseUri,
    secretKey: SecretKey,
    webhookSecret: WebhookSecret,
    livemode: Livemode,
  )

object StripeClientConfig {
  final case class ApplicationFeeBasePoints(value: Int) extends Opaque[Int]
  case object ApplicationFeeBasePoints extends OpaqueCompanion[Int, ApplicationFeeBasePoints]

  final case class BaseUri(value: Uri) extends Opaque[Uri]
  case object BaseUri extends OpaqueCompanion[Uri, BaseUri]

  final case class SecretKey(value: String) extends Opaque[String]
  case object SecretKey extends OpaqueCompanion[String, SecretKey]

  final case class WebhookSecret(value: String) extends Opaque[String]
  case object WebhookSecret extends OpaqueCompanion[String, WebhookSecret]
}
