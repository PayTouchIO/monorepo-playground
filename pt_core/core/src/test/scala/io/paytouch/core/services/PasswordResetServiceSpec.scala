package io.paytouch.core.services

import java.util.UUID

import com.softwaremill.macwire._
import io.paytouch.core.async.monitors.{ AuthenticationMonitor, UserMonitor }
import io.paytouch.core.async.sqs.{ SQSMessageSender, SendMsgWithRetry }
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.PasswordResetTokenRecord
import io.paytouch.core.entities.PasswordResetToken
import io.paytouch.core.expansions.{ MerchantExpansions, UserExpansions }
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.messages.entities.{ PasswordResetRequested, WelcomePasswordResetRequested }
import io.paytouch.core.utils.{ PaytouchLogger, FixtureDaoFactory => Factory }
import io.paytouch.core.{ ServiceConfigurations => Config }
import io.paytouch.utils.Tagging._

class PasswordResetServiceSpec extends ServiceDaoSpec {

  abstract class PasswordResetServiceSpecContext extends ServiceDaoSpecContext {
    implicit val logger = new PaytouchLogger

    val bcryptRounds = Config.bcryptRounds

    val messageHandler = new SQSMessageHandler(actorSystem, actorMock.ref.taggedWith[SQSMessageSender])

    val authenticationMonitor = actorMock.ref.taggedWith[AuthenticationMonitor]
    val authententicationService: AuthenticationService = wire[AuthenticationService]

    val service: PasswordResetService = wire[PasswordResetService]
    val merchantEntity = merchantService.findById(user.merchantId)(MerchantExpansions.none).await.get

    val userDao = daos.userDao
    val passwordResetTokenDao = daos.passwordResetTokenDao

    def getTokenEntity(): PasswordResetToken = getMaybeTokenEntity().get
    def getMaybeTokenEntity(): Option[PasswordResetToken] =
      passwordResetTokenDao
        .run(
          passwordResetTokenDao.table.filter(_.userId === user.id).result.headOption,
        )
        .await
        .map(service.fromRecordToEntity)

    def findTokenById(id: UUID): Option[PasswordResetTokenRecord] =
      passwordResetTokenDao
        .run(
          passwordResetTokenDao.table.filter(_.id === id).result.headOption,
        )
        .await
  }

  "PasswordResetService" in {
    "startPasswordReset" should {
      "if a user with the email is found" should {
        "send the correct message" in new PasswordResetServiceSpecContext {
          service.startPasswordReset(user.email).await

          afterAWhile {
            getMaybeTokenEntity() must beSome
          }

          val token = getTokenEntity()
          actorMock.expectMsg(
            SendMsgWithRetry(PasswordResetRequested(user.merchantId, token, user.toUserInfo, merchantEntity)),
          )
        }

        "delete old password reset tokens" in new PasswordResetServiceSpecContext {
          val oldToken = Factory.passwordResetToken(user).create
          findTokenById(oldToken.id) ==== Some(oldToken)

          service.startPasswordReset(user.email).await

          afterAWhile {
            findTokenById(oldToken.id) ==== None
          }

          val token = getTokenEntity()
          token.id !=== oldToken.id
        }
      }

      "if a user with the email is not found" should {
        "not send any message" in new PasswordResetServiceSpecContext {
          service.startPasswordReset("nobody@nope.com").await

          actorMock.expectNoMessage()
        }
      }
    }
    "sendWelcomePasswordReset" should {
      "if a user with the email is found" should {
        "send the correct message" in new PasswordResetServiceSpecContext {
          service.sendWelcomePasswordReset(user.email).await

          afterAWhile {
            getMaybeTokenEntity() must beSome
          }

          val token = getTokenEntity()
          actorMock.expectMsg(
            SendMsgWithRetry(WelcomePasswordResetRequested(user.merchantId, token, user.toUserInfo, merchantEntity)),
          )
        }

        "delete old password reset tokens" in new PasswordResetServiceSpecContext {
          val oldToken = Factory.passwordResetToken(user).create
          findTokenById(oldToken.id) ==== Some(oldToken)

          service.sendWelcomePasswordReset(user.email).await

          afterAWhile {
            findTokenById(oldToken.id) ==== None
          }

          val token = getTokenEntity()
          token.id !=== oldToken.id
        }
      }

      "if a user with the email is not found" should {
        "not send any message" in new PasswordResetServiceSpecContext {
          service.sendWelcomePasswordReset("nobody@nope.com").await

          actorMock.expectNoMessage()
        }
      }
    }
  }
}
