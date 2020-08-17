package io.paytouch.core.services

import java.util.UUID

import scala.concurrent._

import cats.data._
import cats.implicits._

import io.paytouch.core.BcryptRounds
import io.paytouch.core.conversions.PasswordResetConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model
import io.paytouch.core.entities
import io.paytouch.core.entities.enums.LoginSource
import io.paytouch.core.expansions.MerchantExpansions
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.validators.PasswordResetValidator
import io.paytouch.utils.Tagging.withTag

class PasswordResetService(
    val bcryptRounds: Int withTag BcryptRounds,
    val authenticationService: AuthenticationService,
    val merchantService: MerchantService,
    val messageHandler: SQSMessageHandler,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends PasswordResetConversions {
  protected val dao = daos.passwordResetTokenDao
  protected val userDao = daos.userDao
  protected val validator = new PasswordResetValidator

  // To prevent user email enumeration we perform the grunt work in a seperate
  // future that we don't await the result of. This ensures that emails that
  // exist and emails that don't exist always have the same request time and
  // response.
  def startPasswordReset(email: String): Future[Unit] =
    Future {
      generateResetToken(
        email,
        (user, tokenEntity, merchant) =>
          messageHandler.sendPasswordReset(
            user.merchantId,
            tokenEntity,
            user.toUserInfo,
            merchant,
          ),
      ) // value discard Future[Unit] => Unit
    }

  // This endpoint is triggered by automation after our SDR team has logged interest to signup
  def sendWelcomePasswordReset(email: String): Future[Unit] =
    generateResetToken(
      email,
      (user, tokenEntity, merchant) =>
        messageHandler.sendWelcomePasswordReset(
          user.merchantId,
          tokenEntity,
          user.toUserInfo,
          merchant,
        ),
    )

  private[this] def generateResetToken(
      email: String,
      f: (model.UserRecord, entities.PasswordResetToken, entities.Merchant) => Unit,
    ): Future[Unit] =
    (for {
      user <- OptionT(userDao.findByEmail(email))
      merchant <- OptionT(merchantService.findById(user.merchantId)(MerchantExpansions.none))
      _ <- OptionT.liftF(dao.deleteAllByUserId(user.id))
      (_, tokenRecord) <- OptionT.liftF(dao.upsert(toResetToken(user)))
    } yield f(user, fromRecordToEntity(tokenRecord), merchant)).value.void

  def passwordReset(payload: entities.PasswordReset): Future[ErrorsOr[entities.LoginResponse]] =
    validator.validatePasswordReset(payload).flatMap {
      _.fold(
        _.invalid.pure[Future],
        p =>
          for {
            _ <- dao.deleteAllByUserId(p.userId)
            _ <- userDao.upsert(toUserUpdate(p))
            _ <- authenticationService.deleteSessionsByUserId(p.userId)
            r <- authenticationService.createValidatedSessionForUser(p.userId, LoginSource.PtDashboard)
          } yield r,
      )
    }
}
