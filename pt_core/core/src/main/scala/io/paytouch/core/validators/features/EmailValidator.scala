package io.paytouch.core.validators.features

import cats.implicits._

import io.paytouch.core.errors.InvalidEmail
import io.paytouch.core.utils.Multiple.ErrorsOr

import io.paytouch.core.utils.{ Formatters, Multiple }

import scala.concurrent._

trait EmailValidator {

  implicit val ec: ExecutionContext

  def validateEmailFormat(email: String): Future[ErrorsOr[String]] =
    Future.successful {
      if (Formatters.isValidEmail(email)) Multiple.success(email.toLowerCase)
      else Multiple.failure(InvalidEmail(email))
    }

  def validateEmailFormat(email: Option[String]): Future[ErrorsOr[Option[String]]] =
    email match {
      case Some(em) => validateEmailFormat(em).mapNested(Some(_))
      case _        => Future.successful(Multiple.empty)
    }
}
