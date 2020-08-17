package io.paytouch.core.data.daos.features

import java.time.{ LocalDateTime, ZonedDateTime }
import java.util.UUID

import io.paytouch.core.data.daos.LocationDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.{ LocationIdColumn, OptLocationIdColumn }
import io.paytouch.core.data.tables.{ LocationsTable, SlickTable }

trait SlickLocationTimeZoneHelper extends SlickCommonLocationTimeZone {

  type Table <: SlickTable[Record] with LocationIdColumn

  override def locationTimeZoneJoinCondition(locations: LocationsTable, others: Table): Rep[Boolean] =
    locations.id === others.locationId

}

trait SlickLocationOptTimeZoneHelper extends SlickCommonLocationTimeZone {

  type Table <: SlickTable[Record] with OptLocationIdColumn

  override def locationTimeZoneJoinCondition(locations: LocationsTable, others: Table): Rep[Boolean] =
    (locations.id === others.locationId).getOrElse(false)

}

trait SlickPreFilteredLocationOptTimeZoneHelper extends SlickCommonLocationTimeZone {

  override def locationTimeZoneJoinCondition(locations: LocationsTable, others: Table): Rep[Boolean] = true

}

trait SlickCommonLocationTimeZone extends SlickDao {

  def locationDao: LocationDao

  def locationTimeZoneJoinCondition(locations: LocationsTable, others: Table): Rep[Boolean]

  def itemIdsAtOrAfterCreatedAtDate(threshold: LocalDateTime) =
    itemIdsAtOrAfterDate(threshold)(_.createdAt)

  def itemIdsBeforeCreatedAtDate(threshold: LocalDateTime) =
    itemIdsBeforeDate(threshold)(_.createdAt)

  def itemIdsAtOrAfterDate(
      threshold: LocalDateTime,
      locationIds: Option[Seq[UUID]] = None,
    )(
      timeExtractor: Table => Rep[ZonedDateTime],
    ) = {
    val op = (x: Rep[LocalDateTime], y: Rep[LocalDateTime]) => x >= y
    querySelectItemIdsWithComparableLocationDate(threshold, locationIds)(timeExtractor, op)
  }

  def itemIdsBeforeDate(
      threshold: LocalDateTime,
      locationIds: Option[Seq[UUID]] = None,
    )(
      timeExtractor: Table => Rep[ZonedDateTime],
    ) = {
    val op = (x: Rep[LocalDateTime], y: Rep[LocalDateTime]) => x < y
    querySelectItemIdsWithComparableLocationDate(threshold, locationIds)(timeExtractor, op)
  }

  def itemIdsAtOrAfterOptDate(
      threshold: LocalDateTime,
      locationIds: Option[Seq[UUID]] = None,
    )(
      timeExtractor: Table => Rep[Option[ZonedDateTime]],
    ) = {
    val op = (x: Rep[LocalDateTime], y: Rep[LocalDateTime]) => x >= y
    querySelectItemIdsWithComparableLocationOptDate(threshold, locationIds)(timeExtractor, op)
  }

  def itemIdsAtOrBeforeOptDate(
      threshold: LocalDateTime,
      locationIds: Option[Seq[UUID]] = None,
    )(
      timeExtractor: Table => Rep[Option[ZonedDateTime]],
    ) = {
    val op = (x: Rep[LocalDateTime], y: Rep[LocalDateTime]) => x <= y
    querySelectItemIdsWithComparableLocationOptDate(threshold, locationIds)(timeExtractor, op)
  }

  def itemIdsBeforeOptDate(
      threshold: LocalDateTime,
      locationIds: Option[Seq[UUID]] = None,
    )(
      timeExtractor: Table => Rep[Option[ZonedDateTime]],
    ) = {
    val op = (x: Rep[LocalDateTime], y: Rep[LocalDateTime]) => x < y
    querySelectItemIdsWithComparableLocationOptDate(threshold, locationIds)(timeExtractor, op)
  }

  private def querySelectItemIdsWithComparableLocationDate(
      threshold: LocalDateTime,
      locationIds: Option[Seq[UUID]],
    )(
      timeExtractor: Table => Rep[ZonedDateTime],
      op: (Rep[LocalDateTime], Rep[LocalDateTime]) => Rep[Boolean],
    ) = {
    val optTimeExtractor: Table => Rep[Option[ZonedDateTime]] = { t =>
      timeExtractor(t).asColumnOf[Option[ZonedDateTime]]
    }
    querySelectItemIdsWithComparableLocationOptDate(threshold, locationIds)(optTimeExtractor, op)
  }

  private def querySelectItemIdsWithComparableLocationOptDate(
      threshold: LocalDateTime,
      locationIds: Option[Seq[UUID]],
    )(
      timeExtractor: Table => Rep[Option[ZonedDateTime]],
      op: (Rep[LocalDateTime], Rep[LocalDateTime]) => Rep[Boolean],
    ) =
    locationIds
      .fold[Query[LocationDao#Table, LocationDao#Record, Seq]](locationDao.baseQuery)(locationDao.queryByIds)
      .join(baseQuery)
      .on(locationTimeZoneJoinCondition)
      .filter {
        case (locations, others) =>
          val tz = locations.timezone.asColumnOf[String]
          val dateTimeAtTz = timeExtractor(others).map(_.atTimeZone[LocalDateTime](tz))
          dateTimeAtTz.map(op(_, threshold))
      }
      .map { case (_, others) => others.id }

  protected def hasTimeOverlap(
      t: Table,
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
      locationIds: Option[Seq[UUID]] = None,
    )(
      fromTimeExtractor: Table => Rep[Option[ZonedDateTime]],
      toTimeExtractor: Table => Rep[Option[ZonedDateTime]],
    ): Option[Rep[Boolean]] =
    (from, to) match {
      case (Some(start), None) => Some(t.id in itemIdsAtOrAfterOptDate(start, locationIds)(fromTimeExtractor))
      case (None, Some(end))   => Some(t.id in itemIdsBeforeOptDate(end, locationIds)(fromTimeExtractor))
      case (Some(start), Some(end)) =>
        Some(hasPartialOrTotalOverlap(t, start, end, locationIds)(fromTimeExtractor, toTimeExtractor))
      case _ => None
    }

  private def hasPartialOrTotalOverlap(
      t: Table,
      start: LocalDateTime,
      end: LocalDateTime,
      locationIds: Option[Seq[UUID]],
    )(
      fromTimeExtractor: Table => Rep[Option[ZonedDateTime]],
      toTimeExtractor: Table => Rep[Option[ZonedDateTime]],
    ): Rep[Boolean] = {
    def totalOverlap =
      (t.id in itemIdsAtOrAfterOptDate(start, locationIds)(fromTimeExtractor)) && (t.id in itemIdsAtOrBeforeOptDate(
        end,
        locationIds,
      )(toTimeExtractor))
    def partialOverlapWithStart =
      (t.id in itemIdsAtOrAfterOptDate(start, locationIds)(fromTimeExtractor)) && (t.id in itemIdsAtOrBeforeOptDate(
        end,
        locationIds,
      )(fromTimeExtractor))
    def partialOverlapWithEnd =
      (t.id in itemIdsAtOrAfterOptDate(start, locationIds)(toTimeExtractor)) && (t.id in itemIdsAtOrBeforeOptDate(
        end,
        locationIds,
      )(toTimeExtractor))
    partialOverlapWithStart || partialOverlapWithEnd || totalOverlap
  }
}
