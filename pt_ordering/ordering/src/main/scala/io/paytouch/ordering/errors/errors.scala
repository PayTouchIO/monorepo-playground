package io.paytouch.ordering.errors

import java.util.UUID

import akka.http.scaladsl.model.Uri
import cats.data._
import io.paytouch.ordering.clients.paytouch.core.{ CoreError, CoreErrorResponse, CoreGenericError }
import io.paytouch.ordering.clients.worldpay.entities.WorldpayResponse
import io.paytouch.ordering.clients.stripe.entities.StripeError

final case class Errors private (global: Seq[Error])

object Errors {
  def apply(nel: NonEmptyList[Error]): Errors =
    new Errors(nel.toList.distinct)
}

sealed trait Error {
  def message: String
  def code: String
  def values: Seq[AnyRef]
  def objectId: Option[UUID]
  def field: Option[String]
}

trait BadRequest extends Error
trait NotFound extends Error
trait Unauthorized extends Error

final case class ClientError private (uri: String, errors: Seq[CoreError]) extends BadRequest {
  val message = s"Client error from $uri"
  val code = "ClientError"

  val values = Seq.empty
  val objectId = None
  val field = None
}

object ClientError {
  def apply(uri: Uri, response: CoreErrorResponse): ClientError =
    new ClientError(uri.toString, errors = response.errors)

  def apply(uri: Uri, error: String): ClientError =
    new ClientError(uri.toString, errors = Seq(CoreGenericError(error)))
}

final case class WorldpayClientError private (response: WorldpayResponse) extends BadRequest {
  val message = s"Worldpay client error: $response"
  val code = "WorldpayClientError"

  val values = Seq.empty
  val objectId = None
  val field = None
}

final case class WorldpayPaymentNotFound private (id: String) extends NotFound {
  val message = s"Worldpay payment with transactionSetupId: $id not found"
  val code = "WorldpayPaymentNotFound"

  val values = Seq.empty
  val objectId = None
  val field = None
}

trait ErrorMessageCodeForIds[E] {
  def message: String
  def code: String
  def apply(ids: Seq[UUID]): E
}

final case class StripeClientError private (error: StripeError) extends BadRequest {
  val message = s"Stripe client error: $error"
  val code = "StripeClientError"

  val values = Seq.empty
  val objectId = None
  val field = None
}
