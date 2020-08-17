package io.paytouch.core.calculations

import java.time.{ LocalTime, ZoneId, ZonedDateTime }

import io.paytouch.core.RichZoneDateTime

trait OrderPreperationTimeCalculations {
  def calculateEstimatedReadyAt(
      acceptedAt: ZonedDateTime,
      estimatedPrepTimeInMins: Option[Int],
      estimatedDrivingTimeInMins: Option[Int],
      locationTimezone: ZoneId,
      customerRequestedPrepareByTime: Option[LocalTime],
    ): Option[ZonedDateTime] =
    estimatedPrepTimeInMins.map { prepTimeInMins =>
      val maybeCustomerRequestedPrepareByDateTime =
        interpretPrepareByTimeAsDateTimeInUTC(acceptedAt, customerRequestedPrepareByTime, locationTimezone).map(
          _.minusMinutes(estimatedDrivingTimeInMins.getOrElse[Int](0)),
        )

      val baseTime =
        maybeCustomerRequestedPrepareByDateTime.getOrElse(acceptedAt)

      baseTime.plusMinutes(prepTimeInMins)
    }

  def calculateEstimatedDeliveredAt(
      estimatedReadyAt: Option[ZonedDateTime],
      estimatedDrivingTimeInMins: Option[Int],
    ): Option[ZonedDateTime] =
    for {
      drivingTime <- estimatedDrivingTimeInMins
      readyAt <- estimatedReadyAt
    } yield readyAt plusMinutes drivingTime

  def interpretPrepareByTimeAsDateTimeInUTC(
      acceptedAt: ZonedDateTime,
      maybeCustomerRequestedPrepareByTime: Option[LocalTime],
      locationTimezone: ZoneId,
    ): Option[ZonedDateTime] =
    maybeCustomerRequestedPrepareByTime.map { customerRequestedPrepareByTime =>
      val acceptedAtTz = acceptedAt
        .toLocationTimezone(locationTimezone)
      val prepareByDateTimeTz =
        acceptedAtTz.`with`(customerRequestedPrepareByTime)
      val calculatedPrepareByDateTimeTz =
        if (
          prepareByDateTimeTz.isBefore(acceptedAtTz) || prepareByDateTimeTz
            .isEqual(acceptedAtTz)
        )
          prepareByDateTimeTz plusDays 1
        else
          prepareByDateTimeTz

      calculatedPrepareByDateTimeTz.toLocationTimezone(ZoneId.of("UTC"))
    }
}
