package io.paytouch.core.clients.auth0

import akka.actor.{ ActorRef, ActorSystem }
import cats.data._
import cats.implicits._

import java.security.PublicKey
import JwtVerificationError._

import pdi.jwt._

import scala.util.{ Failure, Success }
import scala.concurrent._

import io.paytouch.logging.BaseMdcActor
import io.paytouch.utils.Tagging._
import io.paytouch.core.clients.auth0.entities.UserInfo

class Auth0Client(
    val config: Auth0Config,
    val jwkClient: JwkClient,
  )(implicit
    val mdcActor: ActorRef withTag BaseMdcActor,
    val system: ActorSystem,
  ) {
  implicit val ec = system.dispatcher

  def validateJwtToken(token: String): Future[Either[JwtVerificationError, ValidAuth0JwtToken]] = {
    val resultT = for {
      decodedToken <- EitherT.fromEither[Future](decodeToken(token))
      userId <- EitherT.fromEither[Future](validateUserId(decodedToken))
      audience <- EitherT.fromEither[Future](validateAudience(decodedToken))
      issuer <- EitherT.fromEither[Future](validateIssuer(decodedToken))
      signature <- fetchSignature(decodedToken, issuer)
      validatedToken <- EitherT.fromEither[Future](validateToken(decodedToken, issuer, signature, userId))
    } yield validatedToken
    resultT.value
  }

  protected def decodeToken(token: String): Either[JwtVerificationError, Auth0JwtToken] =
    // First we decode the claim from the token, which we use to validate and
    // fetch the issuer. Once we have that we can fetch the public key, then
    // decode the token again, verifying the signature is valid.
    JwtJson4s.decodeAll(token, JwtOptions(signature = false)) match {
      case Success((header, claim, _)) =>
        Auth0JwtToken(
          token = token,
          header = header,
          claim = claim,
        ).asRight
      case Failure(error) =>
        InvalidToken(error).asLeft
    }

  protected def validateUserId(jwtToken: Auth0JwtToken): Either[JwtVerificationError, String] =
    if (jwtToken.subject.isDefined)
      jwtToken.subject.get.asRight
    else
      MissingSubject.asLeft

  protected def validateAudience(jwtToken: Auth0JwtToken): Either[JwtVerificationError, Set[String]] =
    if (jwtToken.audience.exists(_.matches(config.apiIdentifier.value)))
      jwtToken.audience.asRight
    else
      UnexpectedAudience(jwtToken.audience.mkString(",")).asLeft

  protected def validateIssuer(jwtToken: Auth0JwtToken): Either[JwtVerificationError, String] = {
    def getIssuer(token: Auth0JwtToken) =
      token.issuer.map(_.asRight).getOrElse(MissingIssuer.asLeft)

    def allowedIssuer(issuer: String) =
      if (config.allowedIssuers.exists(_.value.matches(issuer)))
        issuer.asRight
      else
        UnexpectedIssuer(issuer).asLeft

    getIssuer(jwtToken).flatMap(allowedIssuer)
  }

  protected def fetchSignature(
      jwtToken: Auth0JwtToken,
      issuer: String,
    ): EitherT[Future, JwtVerificationError, PublicKey] = {
    def getKeyId(token: Auth0JwtToken) =
      token.keyId.map(_.asRight).getOrElse(MissingKeyId.asLeft)

    for {
      keyId <- EitherT.fromEither[Future](getKeyId(jwtToken))
      result <- EitherT(jwkClient.getPublicKey(issuer, keyId))
    } yield result
  }

  protected def validateToken(
      jwtToken: Auth0JwtToken,
      issuer: String,
      publicKey: PublicKey,
      userId: String,
    ): Either[JwtVerificationError, ValidAuth0JwtToken] =
    JwtJson4s.decode(jwtToken.token, publicKey, Seq(config.algorithm)) match {
      case Success(_)     => ValidAuth0JwtToken(jwtToken, issuer, userId).asRight
      case Failure(error) =>
        // Token not valid (invalid signature, expired, etc)
        InvalidSignature(error).asLeft
    }

  def userInfo(token: ValidAuth0JwtToken): Future[Either[Auth0ClientError, UserInfo]] =
    new UserInfoClient(token.issuer).userInfo(token)
}
