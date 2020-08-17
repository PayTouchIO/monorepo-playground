package io.paytouch.ordering.services

import java.time.Duration

import com.typesafe.scalalogging.LazyLogging
import io.paytouch.ordering.clients.google.GMapsClient
import io.paytouch.ordering.entities.{ Address, DrivingInfo }

import scala.concurrent.{ ExecutionContext, Future }

class GMapsService(val client: GMapsClient)(implicit val ec: ExecutionContext) extends LazyLogging {

  def getDrivingInfo(origin: Option[Address], destination: Option[Address]): Future[Option[DrivingInfo]] =
    (origin, destination) match {
      case (Some(o), Some(d)) => getDrivingInfo(o, d)
      case _                  => Future.successful(None)
    }

  def getDrivingInfo(origin: Address, destination: Address): Future[Option[DrivingInfo]] =
    client.distanceMatrix(origin = origin.encodedString, destination = destination.encodedString).map {
      case Left(error) =>
        logger.info(s"While requesting driving info to GMaps from $origin to $destination: " + error)
        None
      case Right(gDistanceMatrix) =>
        gDistanceMatrix.best.map { element =>
          val distanceInMeters = element.distance.value
          val durationInMins = Duration.ofSeconds(element.duration.value).toMinutes.toInt
          DrivingInfo(distanceInMeters, durationInMins)
        }
    }
}
