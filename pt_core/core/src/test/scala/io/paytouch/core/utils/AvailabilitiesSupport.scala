package io.paytouch.core.utils

import java.time.LocalTime
import java.util.UUID

import io.paytouch.core.Availabilities
import io.paytouch.core.data.daos.AvailabilityDao
import io.paytouch.core.entities.{ Availability, Weekdays }
import io.paytouch.utils.FutureHelpers
import org.specs2.matcher.MustThrownExpectations

trait AvailabilitiesSupport[D <: AvailabilityDao]
    extends MustThrownExpectations
       with FutureHelpers
       with LocalDateAssertions {

  def availabilityDao: D

  def assertAvailabilityUpsertion(itemId: UUID, availabilityUpsertions: Availabilities) = {
    val availabilities = availabilityDao.findByItemId(itemId).await
    availabilities.size ==== 1

    val availability = availabilities.head
    availability.start must beApproxTheSame(availabilityUpsertions.values.head.head.start)
    availability.end must beApproxTheSame(availabilityUpsertions.values.head.head.end)
    availability.sunday ==== availabilityUpsertions.keySet.contains(Weekdays.Sunday)
    availability.monday ==== availabilityUpsertions.keySet.contains(Weekdays.Monday)
    availability.tuesday ==== availabilityUpsertions.keySet.contains(Weekdays.Tuesday)
    availability.wednesday ==== availabilityUpsertions.keySet.contains(Weekdays.Wednesday)
    availability.thursday ==== availabilityUpsertions.keySet.contains(Weekdays.Thursday)
    availability.friday ==== availabilityUpsertions.keySet.contains(Weekdays.Friday)
    availability.saturday ==== availabilityUpsertions.keySet.contains(Weekdays.Saturday)
  }

  def buildAvailability = {
    val availabilities = Seq(Availability(LocalTime.of(12, 34, 0), LocalTime.of(21, 43, 35)))
    Map(Weekdays.Monday -> availabilities, Weekdays.Tuesday -> availabilities)
  }
}
