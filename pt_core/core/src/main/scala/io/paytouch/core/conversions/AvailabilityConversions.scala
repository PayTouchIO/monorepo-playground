package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.{ Availabilities, AvailabilitiesPerItemId }
import io.paytouch.core.data.model.enums.AvailabilityItemType
import io.paytouch.core.data.model.{ AvailabilityRecord, AvailabilityUpdate }
import io.paytouch.core.entities.Weekdays._
import io.paytouch.core.entities.{ UserContext, Weekdays, Availability => AvailabilityEntity }

trait AvailabilityConversions extends EntityConversion[AvailabilityRecord, AvailabilityEntity] {
  def itemType: AvailabilityItemType

  def toAvailabilityMap(
      availabilities: Seq[AvailabilityRecord],
    )(implicit
      user: UserContext,
    ): Map[Day, Seq[AvailabilityEntity]] =
    Weekdays
      .values
      .toSeq
      .flatMap { day =>
        val availabilitiesPerDay = availabilities.filter(_.isApplicableOn(day)).map(fromRecordToEntity)
        if (availabilitiesPerDay.nonEmpty) Some(day, availabilitiesPerDay)
        else None
      }
      .toMap

  def fromRecordToEntity(record: AvailabilityRecord)(implicit user: UserContext): AvailabilityEntity =
    AvailabilityEntity(record.start, record.end)

  def toAvailabilityUpdate(
      itemId: UUID,
      days: Seq[Day],
      availabilityEntity: AvailabilityEntity,
    )(implicit
      user: UserContext,
    ): AvailabilityUpdate =
    AvailabilityUpdate(
      id = None,
      merchantId = Some(user.merchantId),
      itemId = Some(itemId),
      itemType = Some(itemType),
      sunday = Some(days.contains(Weekdays.Sunday)),
      monday = Some(days.contains(Weekdays.Monday)),
      tuesday = Some(days.contains(Weekdays.Tuesday)),
      wednesday = Some(days.contains(Weekdays.Wednesday)),
      thursday = Some(days.contains(Weekdays.Thursday)),
      friday = Some(days.contains(Weekdays.Friday)),
      saturday = Some(days.contains(Weekdays.Saturday)),
      start = Some(availabilityEntity.start),
      end = Some(availabilityEntity.end),
    )

  def groupDaysPerAvailability(availabilitiesMap: Availabilities): Map[AvailabilityEntity, Seq[Day]] = {
    val flattenLocationAvailabilities: Seq[(AvailabilityEntity, Day)] = normalizeLocationAvailabilities(
      availabilitiesMap,
    )
    flattenLocationAvailabilities.groupBy { case (availability, _) => availability }.map {
      case (k, v) => (k, v.map { case (_, day) => day })
    }
  }

  private def normalizeLocationAvailabilities(
      availabilitiesMap: Map[Day, Seq[AvailabilityEntity]],
    ): Seq[(AvailabilityEntity, Day)] =
    availabilitiesMap.toSeq.flatMap {
      case (day, availabilities) =>
        availabilities.map((_, day))
    }

  def toAvailabilities(
      itemId: Option[UUID],
      availabilities: Option[Map[Day, Seq[AvailabilityEntity]]],
    )(implicit
      user: UserContext,
    ): Seq[AvailabilityUpdate] =
    availabilities match {
      case Some(availabilitiesMap) =>
        val daysPerAvailability: Map[AvailabilityEntity, Seq[Day]] = groupDaysPerAvailability(availabilitiesMap)
        daysPerAvailability.flatMap {
          case (availabilityEntity, days) =>
            itemId.map(toAvailabilityUpdate(_, days, availabilityEntity))
        }.toSeq
      case _ => Seq.empty
    }

  def groupAvailabilitiesPerItemId(
      availabilities: Seq[AvailabilityRecord],
    )(implicit
      user: UserContext,
    ): AvailabilitiesPerItemId =
    availabilities
      .groupBy(_.itemId)
      .transform((_, v) => toAvailabilityMap(v))
}
