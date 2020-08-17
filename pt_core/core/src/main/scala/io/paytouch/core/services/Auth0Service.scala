package io.paytouch.core.services

import java.util._
import java.time.ZoneId

import cats.data._
import cats.data.Validated._
import cats.implicits._

import io.paytouch.core.clients.auth0._
import io.paytouch.core.clients.auth0.entities._
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.enums._
import io.paytouch.core.entities.{ UserInfo => UserInfoEntity, _ }
import io.paytouch.core.errors._
import io.paytouch.core.utils._
import io.paytouch.core.utils.FindResult._
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.utils.Multiple.ErrorsOr

import scala.concurrent._

class Auth0Service(
    val auth0Client: Auth0Client,
    val userService: UserService,
    val authenticationService: AuthenticationService,
    val adminMerchantService: AdminMerchantService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
    val logger: PaytouchLogger,
  ) {
  val userDao = daos.userDao

  def login(credentials: Auth0Credentials): Future[ErrorsOr[Option[LoginResponse]]] =
    findUserLoginByAuth0Token(credentials.token).flatMap {
      case Some(userLogin) =>
        authenticationService.validateUserLoginAndSource(userLogin, credentials)

      case _ =>
        Multiple.empty.pure[Future]
    }

  def validateToken(token: String): Future[ErrorsOr[Option[Unit]]] =
    auth0Client.validateJwtToken(token).flatMap {
      case Left(error) =>
        logger.info(s"Auth0 token is invalid: $error")
        Validated.Invalid(NonEmptyList.of(InvalidToken())).pure[Future]

      case Right(validatedToken) =>
        userDao.findUserLoginByAuth0UserId(validatedToken.auth0UserId).map {
          case Some(user) => Validated.Valid(((): Unit).some)
          case None       => Validated.Valid(None)
        }
    }

  def findUserLoginByAuth0Token(token: String): Future[Option[UserLogin]] =
    auth0Client.validateJwtToken(token).flatMap {
      case Left(error) =>
        logger.info(s"Auth0 token is invalid: $error")
        None.pure[Future]

      case Right(validatedToken) =>
        userDao.findUserLoginByAuth0UserId(validatedToken.auth0UserId)
    }

  def registration(creation: Auth0Registration): Future[ErrorsOr[Result[Merchant]]] =
    auth0Client.validateJwtToken(creation.token).flatMap {
      case Right(validToken) =>
        auth0Client.userInfo(validToken).flatMap {
          case Right(userInfo) =>
            val merchantId = UUID.randomUUID
            adminMerchantService.create(merchantId, toMerchantCreation(merchantId, creation, validToken, userInfo))

          case Left(_) =>
            Validated.Invalid(NonEmptyList.of(InvalidToken())).pure[Future]
        }
      case Left(_) =>
        Validated.Invalid(NonEmptyList.of(InvalidToken())).pure[Future]
    }

  case object InvalidToken {
    def apply(): UnauthorizedError = UnauthorizedError(message = "The supplied authentication token is invalid")
  }

  private def toMerchantCreation(
      id: UUID,
      registration: Auth0Registration,
      validToken: ValidAuth0JwtToken,
      userInfo: UserInfo,
    ): MerchantCreation =
    MerchantCreation(
      businessType = registration.businessType,
      businessName = registration.businessName,
      address = registration.address,
      restaurantType = registration.restaurantType,
      currency = registration.currency,
      firstName = userInfo.name,
      lastName = "",
      // TODO refactor user so password authentication can be disabled, then we don't need this
      password = UUID.randomUUID.toString,
      email = userInfo.email,
      auth0UserId = Some(validToken.auth0UserId),
      zoneId = userInfo.zoneinfo.getOrElse(registration.zoneId),
      pin = registration.pin,
      mode = registration.mode,
      setupType = registration.setupType,
      dummyData = registration.dummyData,
    )
}
