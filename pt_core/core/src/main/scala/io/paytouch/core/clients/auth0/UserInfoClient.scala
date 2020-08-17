package io.paytouch.core.clients.auth0

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.client.RequestBuilding._

import io.paytouch.core.clients.auth0.entities.UserInfo
import io.paytouch.logging.BaseMdcActor
import io.paytouch.utils.Tagging._

import scala.concurrent.Future

class UserInfoClient(
    val issuer: String,
  )(implicit
    val mdcActor: ActorRef withTag BaseMdcActor,
    val system: ActorSystem,
  ) extends Auth0HttpClient {
  def userInfo(jwtToken: ValidAuth0JwtToken): Future[EitherAuth0ClientErrorOr[UserInfo]] =
    sendAndReceive[UserInfo](Get(s"/userinfo").withToken(jwtToken.token))
}
