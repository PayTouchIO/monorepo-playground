package io.paytouch.ordering.clients.paytouch.core

import java.util.UUID

import scala.util._

import io.paytouch._
import io.paytouch.implicits._

import io.paytouch.ordering.errors._
import io.paytouch.ordering.json.JsonSupport.JValue
import io.paytouch.ordering.json.JsonSupport
import io.paytouch.ordering.clients.paytouch.core.entities.GiftCardPassCharge

trait CoreError {
  def convert: Seq[Error]
}

case class CoreEmbeddedError(
    message: String,
    code: String,
    values: Seq[AnyRef] = Seq.empty,
    objectId: Option[UUID] = None,
    field: Option[String] = None,
  ) extends CoreError {
  def valuesAsUuids: Seq[UUID] =
    values.flatMap {
      case s: String => Try(UUID.fromString(s)).toOption
      case u: UUID   => Some(u)
      case _         => None
    }

  def convert: Seq[Error] =
    code match {
      case "ProductOutOfStock" =>
        valuesAsUuids.map(ProductOutOfStock(_))

      case "GiftCardPassesNotAllFound" =>
        Seq(GiftCardPassesNotAllFound)

      case "InsufficientFunds" =>
        Seq(InsufficientFunds(values.map(_.asInstanceOf[GiftCardPassCharge.Failure])))

      case _ =>
        Seq.empty
    }
}

case class CoreGenericError(error: String) extends CoreError {
  def convert = Seq.empty
}

trait CoreErrorResponse {
  def errors: Seq[CoreError]
  def objectWithErrors: Option[JValue]
}
case class CoreEmbeddedErrorResponse(errors: Seq[CoreEmbeddedError], objectWithErrors: Option[JValue])
    extends CoreErrorResponse
case class CoreGenericErrorResponse(errors: Seq[CoreGenericError]) extends CoreErrorResponse {
  def objectWithErrors: Option[JValue] = None
}

object CoreErrorResponse {
  def apply(global: Seq[CoreEmbeddedError], objectWithErrors: Option[JValue]): CoreErrorResponse =
    new CoreEmbeddedErrorResponse(global, objectWithErrors)

  def apply(errors: Seq[String]): CoreErrorResponse =
    new CoreGenericErrorResponse(errors.map(new CoreGenericError(_)))
}
