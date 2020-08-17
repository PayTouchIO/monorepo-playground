package io.paytouch.core.validators

import java.util.UUID

import scala.concurrent._

import cats.implicits._

import io.paytouch.implicits._

import io.paytouch.core.data.daos.Daos
import io.paytouch.core.entities.PasswordReset
import io.paytouch.core.errors.InvalidPasswordResetToken
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.features.PasswordValidator

class PasswordResetValidator(implicit val ec: ExecutionContext, val daos: Daos) extends PasswordValidator {
  val dao = daos.passwordResetTokenDao

  def validatePasswordReset(payload: PasswordReset): Future[ErrorsOr[PasswordReset]] =
    (
      validateToken(payload.userId, payload.token).nested,
      validatePassword(payload.password.some).pure[Future].nested,
    ).tupled.as(payload).value

  protected def validateToken(userId: UUID, token: String): Future[ErrorsOr[Unit]] =
    dao
      .findByUserIdAndToken(userId, token)
      .map(_.as(().validNel).getOrElse(InvalidPasswordResetToken().invalidNel))
}
