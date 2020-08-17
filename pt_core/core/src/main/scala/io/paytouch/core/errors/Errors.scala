package io.paytouch.core.errors

import java.util.UUID

import akka.http.scaladsl.model.Uri

import cats.data._

import io.paytouch._

import io.paytouch.core.json.JsonSupport
import io.paytouch.core.json.JsonSupport.JValue
import io.paytouch.utils.RejectionMsg

final case class Errors(global: List[Error], objectsWithErrors: Map[UUID, ObjectError])

final case class ObjectError(imageUploadType: Option[String], errors: List[Error])

object Errors {
  def apply(nel: NonEmptyList[Error]): Errors = {
    val (global, other) =
      nel
        .toList
        .distinct
        .partition(_.objectId.isEmpty)

    val groupedOther =
      other
        .filter(_.objectId.isDefined)
        .groupBy(_.objectId.get)
        .transform((_, v) => ObjectError(None, v))

    Errors(global, groupedOther)
  }
}

sealed trait Error extends SerializableProduct {
  def message: String
  def code: String = productPrefix
  def values: Seq[AnyRef]
  def objectId: Option[UUID]
  def field: Option[String]
}

trait BadRequest extends Error
trait NotFound extends Error
trait Unauthorized extends Error

final case class ClientError private (uri: String, clientErrors: Option[JValue]) extends BadRequest {
  val message = s"Client error from $uri"

  val values = Seq.empty
  val objectId = None
  val field = None
}

object ClientError {
  def apply(uri: Uri, errors: JValue): ClientError = new ClientError(uri.toString, Some(errors))

  def apply(uri: Uri, error: String): ClientError = apply(uri.toString, error)

  def apply(uri: String, error: String): ClientError = {
    val json = JsonSupport.fromEntityToJValue(RejectionMsg(error))
    new ClientError(uri, Some(json))
  }
}

trait ErrorMessageCodeForIds[E] {
  def message: String
  def code: String
  def apply(ids: Seq[UUID]): E
}
