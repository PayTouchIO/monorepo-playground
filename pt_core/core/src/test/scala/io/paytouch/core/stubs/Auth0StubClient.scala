package io.paytouch.core.stubs

import cats.implicits._
import akka.actor.{ ActorRef, ActorSystem }
import io.paytouch._
import io.paytouch.core.clients.auth0._
import io.paytouch.core.clients.auth0.entities._
import io.paytouch.core.utils.FixturesSupport
import io.paytouch.logging.BaseMdcActor
import io.paytouch.utils.Generators
import io.paytouch.utils.Tagging._
import scala.concurrent._

class Auth0StubClient(
    override val config: Auth0Config,
    override val jwkClient: JwkClient,
  )(implicit
    override val mdcActor: ActorRef withTag BaseMdcActor,
    override val system: ActorSystem,
  ) extends Auth0Client(config, jwkClient)
       with FixturesSupport
       with Generators {
  def emailForUserId(userId: String) = s"${userId}@auth0-paytouch-user.com"

  override def userInfo(token: ValidAuth0JwtToken): Future[Either[Auth0ClientError, UserInfo]] =
    loadJsonAs[UserInfo]("/auth0/responses/userinfo.json")
      .copy(
        email = emailForUserId(token.auth0UserId.value),
      )
      .asRight
      .pure[Future]
}
