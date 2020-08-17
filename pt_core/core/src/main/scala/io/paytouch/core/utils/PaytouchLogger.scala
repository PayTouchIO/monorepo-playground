package io.paytouch.core.utils

import java.util.UUID

import cats.data._

import com.typesafe.scalalogging.Logger

import org.slf4j.LoggerFactory

import io.paytouch.core.json.JsonSupport

class PaytouchLogger extends JsonSupport {
  @volatile private lazy val logger: Logger =
    Logger(LoggerFactory.getLogger(getClass.getName))

  def loggedRecover[E, T, S <: AnyRef](
      validation: ValidatedNel[E, Option[T]],
    )(
      description: String,
      context: S,
    ): Option[T] =
    loggedGenericRecover(validation, None)(description, context)

  def loggedRecoverUUID[E, S <: AnyRef](validation: ValidatedNel[E, UUID])(description: String, context: S): UUID =
    loggedGenericRecover(validation, UUID.randomUUID)(description, context)

  def loggedRecoverSeq[E, S <: AnyRef, T](
      validation: ValidatedNel[E, Seq[T]],
    )(
      description: String,
      context: S,
    ): Seq[T] =
    loggedGenericRecover(validation, Seq.empty)(description, context)

  def loggedGenericRecover[E, T, S <: AnyRef](
      validation: ValidatedNel[E, T],
      default: T,
    )(
      description: String,
      context: S,
    ): T =
    validation match {
      case Validated.Valid(a) => a
      case Validated.Invalid(i) =>
        recoverLog(s"$description: ${fromEntityToString(i)}. Recovering with $default", context)
        default
    }

  def loggedSoftRecover[E, T](validation: ValidatedNel[E, Option[T]])(description: String): Option[T] =
    loggedGenericSoftRecover(validation, None)(description)

  def loggedSoftRecoverUUID[E](validation: ValidatedNel[E, UUID])(description: String): UUID =
    loggedGenericSoftRecover(validation, UUID.randomUUID)(description)

  def loggedGenericSoftRecover[E, T](validation: ValidatedNel[E, T], default: T)(description: String): T =
    validation match {
      case Validated.Valid(a) => a
      case Validated.Invalid(i) =>
        softRecoverLog(s"$description: ${fromEntityToString(i)}. Setting it to $default")
        default
    }

  def softRecoverLog(message: String) = info(s"[SOFT RECOVERING] $message")

  def recoverLog[S <: AnyRef](message: String, context: S) = {
    val tag = "WARNING - VALIDATION RECOVERING"
    warn(s"[$tag] $message [${fromEntityToString(context)}]")
  }

  def error(message: String) = logger.error(message)
  def info(message: String) = logger.info(message)
  def warn(message: String) = logger.warn(message)
}
