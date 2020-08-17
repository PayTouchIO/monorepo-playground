package io.paytouch.core.validators.features

import cats.implicits._

import io.paytouch.core.errors.InvalidPassword
import io.paytouch.core.utils.Multiple.ErrorsOr

trait PasswordValidator {
  def validatePassword(password: Option[String]): ErrorsOr[Option[String]] =
    password
      .map(toValidated)
      .getOrElse(none.validNel)

  private def toValidated(password: String): ErrorsOr[Option[String]] =
    if (isValidLength(password))
      password.some.validNel
    else
      InvalidPassword().invalidNel

  private def isValidLength(password: String): Boolean =
    password.length >= PasswordValidator.MinLength
}

object PasswordValidator {
  val MinLength: Int = 8
}
