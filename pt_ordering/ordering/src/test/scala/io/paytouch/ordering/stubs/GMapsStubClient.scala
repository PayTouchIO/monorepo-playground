package io.paytouch.ordering.stubs

import java.time.Duration

import akka.actor.{ ActorRef, ActorSystem }
import akka.stream.Materializer
import io.paytouch.logging.BaseMdcActor
import io.paytouch.ordering._
import io.paytouch.ordering.clients.google.GMapsClient
import io.paytouch.ordering.clients.google.entities.{ GDistance, GDistanceMatrix, GDuration }
import io.paytouch.ordering.entities.Address
import io.paytouch.ordering.logging.MdcActor
import io.paytouch.ordering.utils.CommonArbitraries

import scala.concurrent.Future

class GMapsStubClient(
    implicit
    override val mdcActor: ActorRef withTag BaseMdcActor,
    override val system: ActorSystem,
    override val materializer: Materializer,
  ) extends GMapsClient("my-google-key".taggedWith[GMapsClient]) {

  import GMapsStubData._

  override def distanceMatrix(origin: String, destination: String): Future[Wrapper[GDistanceMatrix]] =
    Future.successful {
      val distance = GDistance(s"distance-$origin-$destination", retrieveDistance(origin, destination))
      val duration = GDuration(s"duration-$origin-$destination", retrieveDuration(origin, destination))
      Right(GDistanceMatrix(distance, duration))
    }

}

object GMapsStubData extends CommonArbitraries {

  private var distanceMap: Map[String, Long] = Map.empty
  private var durationMap: Map[String, Long] = Map.empty

  def recordDistance(destination: Address, distance: BigDecimal) =
    synchronized {
      distanceMap += destination.encodedString -> distance.toLong
    }

  def recordDuration(destination: Address, duration: Int) =
    synchronized {
      durationMap += destination.encodedString -> duration.toLong
    }

  def retrieveDistanceInMeters(origin: Address, destination: Address): BigDecimal =
    retrieveDistance(origin.encodedString, destination.encodedString)

  def retrieveDistance(origin: String, destination: String): Long =
    distanceMap.getOrElse(destination, guessDistance(origin, destination))

  def retrieveDurationInMins(origin: Address, destination: Address): Int = {
    val seconds = retrieveDuration(origin.encodedString, destination.encodedString)
    Duration.ofSeconds(seconds).toMinutes.toInt
  }

  def retrieveDuration(origin: String, destination: String): Long =
    durationMap.getOrElse(destination, guessDuration(origin, destination))

  private def guessDistance(origin: String, destination: String): Long =
    origin.length * random[Int]

  private def guessDuration(origin: String, destination: String): Long =
    destination.length / random[Int]
}
