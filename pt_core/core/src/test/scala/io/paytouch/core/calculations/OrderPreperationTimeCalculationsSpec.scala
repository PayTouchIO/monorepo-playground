package io.paytouch.core.calculations

import java.time.{ LocalTime, ZoneId, ZonedDateTime }

import io.paytouch.core.RichZoneDateTime
import io.paytouch.core.utils.PaytouchSpec
import io.paytouch.core.utils.TimeImplicits._
import org.scalacheck.Gen
import org.specs2.ScalaCheck

import scala.concurrent.duration._

@scala.annotation.nowarn("msg=Auto-application")
class OrderPreperationTimeCalculationsSpec extends PaytouchSpec with ScalaCheck {

  object Subject extends OrderPreperationTimeCalculations

//  Useful to run more tests while testing/developing
//  implicit val params = Parameters(minTestsOk = 300, minSize = 300, maxSize = 5000).verbose

  "estimatedReadyAt" should {
    "if estimatedPrepTimeInMins is None" should {
      "be None " >> prop {
        (
            acceptedAt: ZonedDateTime,
            locationTimezone: ZoneId,
            estimatedDrivingTimeInMins: Option[Int],
            customerRequestedPrepareByTime: Option[LocalTime],
        ) =>
          Subject.calculateEstimatedReadyAt(
            acceptedAt,
            None,
            estimatedDrivingTimeInMins,
            locationTimezone,
            customerRequestedPrepareByTime,
          ) ==== None
      }
    }

    "if estimatedPrepTimeInMins is defined" should {
      "if customerRequestedPrepareByTime is None" should {
        "be the sum of acceptedAt and estimatedPrepTimeInMins" >> prop {
          (
              acceptedAt: ZonedDateTime,
              estimatedPrepTimeInMins: Int,
              estimatedDrivingTimeInMins: Option[Int],
              locationTimezone: ZoneId,
          ) =>
            Subject.calculateEstimatedReadyAt(
              acceptedAt,
              Some(estimatedPrepTimeInMins),
              estimatedDrivingTimeInMins,
              locationTimezone,
              None,
            ) ==== Some(acceptedAt + estimatedPrepTimeInMins.minutes)
        }
      }
      "if customerRequestedPrepareByTime is defined" should {
        "be the sum of customerRequestedPrepareByTime (in UTC) and estimatedPrepTimeInMins minus estimatedDrivingTimeInMins" >> prop {
          (
              acceptedAt: ZonedDateTime,
              estimatedPrepTimeInMins: Int,
              estimatedDrivingTimeInMins: Option[Int],
              locationTimezone: ZoneId,
              customerRequestedPrepareByTime: LocalTime,
          ) =>
            val customerRequestedPrepareByDateTime =
              Subject.interpretPrepareByTimeAsDateTimeInUTC(
                acceptedAt,
                Some(customerRequestedPrepareByTime),
                locationTimezone,
              )

            Subject.calculateEstimatedReadyAt(
              acceptedAt,
              Some(estimatedPrepTimeInMins),
              estimatedDrivingTimeInMins,
              locationTimezone,
              Some(customerRequestedPrepareByTime),
            ) ==== customerRequestedPrepareByDateTime.map(
              _ + estimatedPrepTimeInMins.minutes - estimatedDrivingTimeInMins.getOrElse(0).minutes,
            )
        }
      }
    }
  }

  "estimatedDeliveredAt" should {
    "be None if estimatedPrepTimeInMins is None" >> prop { estimatedDrivingTimeInMins: Option[Int] =>
      Subject.calculateEstimatedDeliveredAt(
        None,
        estimatedDrivingTimeInMins,
      ) ==== None
    }

    "be None if estimatedReadyAt is None" >> prop { estimatedReadyAt: Option[ZonedDateTime] =>
      Subject.calculateEstimatedDeliveredAt(
        estimatedReadyAt,
        None,
      ) ==== None
    }

    "be the sum if both estimatedReadyAt and estimatedDrivingTimeInMins are defined" >> prop {
      (estimatedReadyAt: ZonedDateTime, estimatedDrivingTimeInMins: Int) =>
        Subject.calculateEstimatedDeliveredAt(
          Some(estimatedReadyAt),
          Some(estimatedDrivingTimeInMins),
        ) ==== Some(estimatedReadyAt + estimatedDrivingTimeInMins.minutes)
    }
  }

  "interpretPrepareByTimeAsDateTime" should {
    "be None if customerRequestedPrepareByTime is None" >> prop {
      (acceptedAt: ZonedDateTime, locationTimezone: ZoneId) =>
        Subject.interpretPrepareByTimeAsDateTimeInUTC(
          acceptedAt.withZoneSameLocal(ZoneId.of("UTC")),
          None,
          locationTimezone,
        ) ==== None
    }

    "if customerRequestedPrepareByTime is defined" should {
      def genZonedDateTimeAtTimezone =
        for {
          zonedDateTime <- genZonedDateTime
          locationTimezone <- genZoneId
          zonedDateTimeInLocation = zonedDateTime.toLocationTimezone(locationTimezone)
        } yield (zonedDateTime, zonedDateTimeInLocation, locationTimezone)

      def genLocalTimeInRange(min: LocalTime, max: LocalTime) =
        Gen
          .chooseNum(min.toNanoOfDay, max.toNanoOfDay)
          .map(LocalTime.ofNanoOfDay)

      "if preparation time is within the same day" should {
        def randomSameDayLocalTimes =
          for {
            (zonedDateTime, zonedDateTimeInLocation, locationTimezone) <- genZonedDateTimeAtTimezone
            customerRequestedPrepareByTime <- genLocalTimeInRange(
              zonedDateTimeInLocation
                .toLocalTime
                .plusNanos(1), // exclude equality
              LocalTime.MAX,
            )
          } yield (zonedDateTime, customerRequestedPrepareByTime, locationTimezone)

        "equal to accepted at converted to timezone with preparation time " >> prop {
          randomOverflowingLocalTimes: (ZonedDateTime, LocalTime, ZoneId) =>
            val acceptedAt = randomOverflowingLocalTimes._1
            val customerRequestedPrepareByTime =
              randomOverflowingLocalTimes._2
            val locationTimezone = randomOverflowingLocalTimes._3
            Subject.interpretPrepareByTimeAsDateTimeInUTC(
              acceptedAt,
              Some(customerRequestedPrepareByTime),
              locationTimezone,
            ) ==== Some(
              acceptedAt
                .toLocationTimezone(locationTimezone)
                .`with`(customerRequestedPrepareByTime)
                .toLocationTimezone(ZoneId.of("UTC")),
            )
        }.setGen(randomSameDayLocalTimes)
      }

      "if preparation time overflows to next day" should {
        def randomOverflowingLocalTimes =
          for {
            (zonedDateTime, zonedDateTimeInLocation, locationTimezone) <- genZonedDateTimeAtTimezone
            customerRequestedPrepareByTime <- genLocalTimeInRange(LocalTime.MIN, zonedDateTimeInLocation.toLocalTime)
          } yield (zonedDateTime, customerRequestedPrepareByTime, locationTimezone)

        "equal to accepted at converted to timezone with preparation time plus one day " >> prop {
          randomOverflowingLocalTimes: (ZonedDateTime, LocalTime, ZoneId) =>
            val acceptedAt = randomOverflowingLocalTimes._1
            val customerRequestedPrepareByTime =
              randomOverflowingLocalTimes._2
            val locationTimezone = randomOverflowingLocalTimes._3
            Subject.interpretPrepareByTimeAsDateTimeInUTC(
              acceptedAt,
              Some(customerRequestedPrepareByTime),
              locationTimezone,
            ) ==== Some(
              acceptedAt
                .toLocationTimezone(locationTimezone)
                .`with`(customerRequestedPrepareByTime)
                .plusDays(1)
                .toLocationTimezone(ZoneId.of("UTC")),
            )
        }.setGen(randomOverflowingLocalTimes)
      }
    }
  }

}
