package io.paytouch.core.clients.paytouch.ordering

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.headers.BasicHttpCredentials

import io.paytouch.core.clients.paytouch.ordering.entities._
import io.paytouch.core.clients.paytouch.{ OrderingApiResponse, PaytouchClient }
import io.paytouch.core.entities.UserContext
import io.paytouch.core.logging.MdcActor
import io.paytouch.core.{ PtOrderingPassword, PtOrderingUser }
import io.paytouch.logging.BaseMdcActor
import io.paytouch.utils.Tagging._

import scala.concurrent._

class PtOrderingClient(
    val uri: Uri withTag PtOrderingClient,
    user: String withTag PtOrderingUser,
    password: String withTag PtOrderingPassword,
  )(implicit
    val mdcActor: ActorRef withTag BaseMdcActor,
    val system: ActorSystem,
  ) extends PaytouchClient {

  implicit private val credentials = BasicHttpCredentials(username = user, password = password)

  def idsCheckUsage(ids: IdsToCheck)(implicit user: UserContext): Future[OrderingApiResponse[IdsUsage]] =
    sendAndReceiveAsApiResponse[IdsUsage](Post(s"/v1/ids.check_usage?merchant_id=${user.merchantId}", ids).withAppAuth)

}
