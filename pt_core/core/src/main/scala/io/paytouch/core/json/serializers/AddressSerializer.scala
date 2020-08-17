package io.paytouch.core.json.serializers

import cats.implicits._

import com.typesafe.scalalogging.LazyLogging

import org.json4s._
import org.json4s.JsonAST._

import io.paytouch.implicits._

import io.paytouch.core._
import io.paytouch.core.validators.AddressValidator

sealed abstract class AddressSerializer[A: Manifest: entities.Address.LossyIso] extends Serializer[A] {
  protected def defaultFormats: Formats

  final protected val M = implicitly[Manifest[A]]
  final protected val Class = M.runtimeClass

  final override def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
    case address: A => Extraction.decompose(address)(defaultFormats)
  }

  final override def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), A] = {
    case (TypeInfo(Class, _), json: JObject) =>
      handle(json.extract[A](defaultFormats, M), json)
  }

  private def handle(a: A, json: JObject): A =
    AddressValidator
      .validated(a)
      .fold(handleErrors(a, json), identity)

  private def handleErrors(a: A, json: JObject)(errors: AddressValidator.Errors): A = {
    handleErrorMessage(errorMessage(json, errors))

    recover(a)
  }

  private def errorMessage(json: JObject, errors: AddressValidator.Errors): String =
    s"Can't convert $json to $Class because ${errors.toList.mkString(" and ")}"

  protected def handleErrorMessage(errorMessage: String): Unit

  protected def recover(address: A): A =
    address
}

sealed abstract class Log[A: Manifest: entities.Address.LossyIso] extends AddressSerializer[A] with LazyLogging {
  final override protected def handleErrorMessage(errorMessage: String): Unit =
    logger.warn(errorMessage)
}

sealed abstract class Raise[A: Manifest: entities.Address.LossyIso] extends AddressSerializer[A] {
  final override protected def handleErrorMessage(errorMessage: String): Unit =
    throw new MappingException(errorMessage)
}

object AddressSerializer {
  final case class Address(defaultFormats: Formats) extends Raise[entities.Address]

  final case class AddressImproved(defaultFormats: Formats) extends Raise[entities.AddressImproved]

  final case class AddressImprovedSync(defaultFormats: Formats) extends Log[entities.AddressImprovedSync] {
    final override protected def recover(address: entities.AddressImprovedSync): entities.AddressImprovedSync =
      address.copy(
        countryCode = None,
        stateCode = None,
      )
  }

  final case class AddressImprovedUpsertion(defaultFormats: Formats) extends Raise[entities.AddressImprovedUpsertion]

  final case class AddressUpsertion(defaultFormats: Formats) extends Raise[entities.AddressUpsertion]

  final case class AddressSync(defaultFormats: Formats) extends Log[entities.AddressSync] {
    final override protected def recover(address: entities.AddressSync): entities.AddressSync =
      address.copy(
        country = None,
        state = None,
        countryCode = None,
        stateCode = None,
      )
  }
}
