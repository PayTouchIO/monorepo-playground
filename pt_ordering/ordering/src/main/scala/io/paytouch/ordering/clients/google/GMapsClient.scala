package io.paytouch.ordering.clients.google

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.model.Uri
import akka.stream.Materializer
import io.paytouch.logging.BaseMdcActor
import io.paytouch.ordering.clients.google.entities.GDistanceMatrix
import io.paytouch.ordering.{ withTag, ServiceConfigurations }

import scala.concurrent.Future

class GMapsClient(
    val key: String withTag GMapsClient,
  )(implicit
    val mdcActor: ActorRef withTag BaseMdcActor,
    val system: ActorSystem,
    val materializer: Materializer,
  ) extends GClient {

  lazy val uri = Uri("https://maps.googleapis.com")
  lazy val referer = ServiceConfigurations.orderingUri

  def distanceMatrix(origin: String, destination: String): Future[Wrapper[GDistanceMatrix]] = {
    val params = s"origins=$origin&destinations=$destination"
    sendAndReceive[GDistanceMatrix](Get(s"/maps/api/distancematrix/json?$params").withAPIKey)
  }
}
