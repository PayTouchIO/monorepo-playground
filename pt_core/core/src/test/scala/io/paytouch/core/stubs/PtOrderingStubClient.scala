package io.paytouch.core.stubs

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.model.Uri

import io.paytouch.core.{ PtOrderingPassword, PtOrderingUser }
import io.paytouch.core.clients.paytouch.OrderingApiResponse
import io.paytouch.core.clients.paytouch.ordering.PtOrderingClient
import io.paytouch.core.clients.paytouch.ordering.entities.{ IdsToCheck, IdsUsage }
import io.paytouch.core.entities.{ ApiResponse, UserContext }
import io.paytouch.core.json.JsonSupport
import io.paytouch.core.logging.MdcActor
import io.paytouch.logging.BaseMdcActor
import io.paytouch.utils.Tagging._

import scala.concurrent._

class PtOrderingStubClient(
    implicit
    override val mdcActor: ActorRef withTag BaseMdcActor,
    override val system: ActorSystem,
  ) extends PtOrderingClient(
      Uri("http://example.com").taggedWith[PtOrderingClient],
      "user".taggedWith[PtOrderingUser],
      "password".taggedWith[PtOrderingPassword],
    )
       with JsonSupport {

  override def idsCheckUsage(ids: IdsToCheck)(implicit user: UserContext): Future[OrderingApiResponse[IdsUsage]] = {
    val usage = PtOrderingStubData.inferIdsUsage(ids)
    Future.successful(Right(ApiResponse(usage, "ids_usage")))
  }

}
