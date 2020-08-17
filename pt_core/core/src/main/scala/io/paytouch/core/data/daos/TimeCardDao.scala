package io.paytouch.core.data.daos

import java.time.LocalDateTime
import java.util.UUID

import scala.concurrent._

import io.paytouch.core.data.daos.features._
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model._
import io.paytouch.core.data.tables.TimeCardsTable
import io.paytouch.core.entities.enums.TimeCardStatus
import io.paytouch.core.filters.TimeCardFilters

class TimeCardDao(
    val locationDao: LocationDao,
    val userDao: UserDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickFindAllDao
       with SlickDefaultUpsertDao
       with SlickDeleteAccessibleLocationsDao
       with SlickLocationTimeZoneHelper {
  type Record = TimeCardRecord
  type Update = TimeCardUpdate
  type Filters = TimeCardFilters
  type Table = TimeCardsTable

  val table = TableQuery[Table]

  def findAllWithFilters(merchantId: UUID, f: Filters)(offset: Int, limit: Int): Future[Seq[Record]] =
    findAllByMerchantId(merchantId, f.locationIds, f.from, f.to, f.status, f.query)(offset, limit)

  def findAllByMerchantId(
      merchantId: UUID,
      locationIds: Seq[UUID],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
      status: Option[TimeCardStatus],
      query: Option[String],
    )(
      offset: Int,
      limit: Int,
    ): Future[Seq[Record]] =
    run(
      queryFindAllByMerchantId(
        merchantId = merchantId,
        locationIds = locationIds,
        from = from,
        to = to,
        status = status,
        query = query,
      ).sortBy(_.startAt.desc).drop(offset).take(limit).result,
    )

  def countAllWithFilters(merchantId: UUID, f: Filters): Future[Int] =
    countAllByMerchantId(merchantId, f.locationIds, f.from, f.to, f.status, f.query)

  def countAllByMerchantId(
      merchantId: UUID,
      locationIds: Seq[UUID],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
      status: Option[TimeCardStatus],
      query: Option[String],
    ): Future[Int] =
    run(
      queryFindAllByMerchantId(
        merchantId = merchantId,
        locationIds = locationIds,
        from = from,
        to = to,
        status = status,
        query = query,
      ).length.result,
    )

  def queryFindAllByMerchantId(
      merchantId: UUID,
      locationIds: Seq[UUID],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
      status: Option[TimeCardStatus],
      query: Option[String] = None,
    ) =
    queryFilterByOptionalStatus(status).filter(t =>
      all(
        Some(t.merchantId === merchantId),
        Some(t.locationId inSet locationIds),
        hasTimeOverlap(t, from, to)(_.startAt, _.endAt),
        query.map(q => t.userId in userDao.querySearchByFullName(q).map(_.id)),
      ),
    )

  def queryFilterByOptionalStatus(status: Option[TimeCardStatus]) =
    status match {
      case Some(TimeCardStatus.Open)   => table.filter(isTimeCardOpen)
      case Some(TimeCardStatus.Closed) => table.filter(isTimeCardClosed)
      case _                           => table
    }

  private def isTimeCardOpen(t: Table) = t.endAt.isEmpty
  private def isTimeCardClosed(t: Table) = t.startAt.isDefined && t.endAt.isDefined

  def findMinuteTotalsByUserIds(
      ids: Seq[UUID],
      locationIds: Seq[UUID],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
    ): Future[Map[UUID, TimeCardTotals]] =
    run(queryFindMinuteTotalsByUserIds(ids, locationIds, from, to).result)
      .map(_.toMap.transform {
        case (_, (totalMins, deltaMins, regularMins, overtimeMins)) =>
          TimeCardTotals(
            totalMins = totalMins,
            deltaMins = deltaMins,
            regularMins = regularMins,
            overtimeMins = overtimeMins,
          )
      })

  def queryFindMinuteTotalsByUserIds(
      userIds: Seq[UUID],
      locationIds: Seq[UUID],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
    ) =
    table
      .filter(t =>
        all(
          Some(isTimeCardClosed(t)),
          Some(t.userId inSet userIds),
          Some(t.locationId inSet locationIds),
          from.map(start => t.id in itemIdsAtOrAfterOptDate(start)(_.startAt)),
          to.map(end => t.id in itemIdsBeforeOptDate(end)(_.startAt)),
        ),
      )
      .groupBy {
        case timeCardsTable => timeCardsTable.userId
      }
      .map {
        case (userId, rows) =>
          userId -> (
            rows.map(_.totalMins).sum.getOrElse(0),
            rows.map(_.deltaMins).sum.getOrElse(0),
            rows.map(_.regularMins).sum.getOrElse(0),
            rows.map(_.overtimeMins).sum.getOrElse(0),
          )
      }

  def findOpenTimeCardByUserIdAndLocationId(userId: UUID, locationId: UUID): Future[Option[Record]] = {
    val q = table
      .filter(t =>
        all(
          Some(isTimeCardOpen(t)),
          Some(t.userId === userId),
          Some(t.locationId === locationId),
        ),
      )

    run(q.result.headOption)
  }

}
