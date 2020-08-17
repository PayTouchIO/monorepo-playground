package io.paytouch.core.validators

import cats.implicits._

import io.paytouch.core.errors.InvalidPassword
import io.paytouch.core.utils.PaytouchSuite
import io.paytouch.core.validators.features.PasswordValidator

final class PasswordValidatorSuite extends PaytouchSuite with PasswordValidator {
  "PasswordValidator.MinLength" should {
    "be 8" in {
      PasswordValidator.MinLength ==== 8
    }
  }

  "PasswordValidator.validatedPassword" should {
    "yield valid if password is none" in {
      validatePassword(None) ==== None.validNel
    }

    "yield valid if password is >= MinLength" in {
      val password: Option[String] =
        ("p" * PasswordValidator.MinLength).some

      validatePassword(password) ==== password.validNel
    }

    "yield error if password is < MinLength" in {
      val password: Option[String] =
        ("p" * (PasswordValidator.MinLength - 1)).some

      validatePassword(password) ==== InvalidPassword().invalidNel
    }
  }
}
