package io.paytouch.core.data.daos

import java.time.LocalDateTime
import java.util.UUID

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.core.data.daos.features._
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ TimeOffCardRecord, TimeOffCardUpdate }
import io.paytouch.core.data.tables.TimeOffCardsTable
import io.paytouch.core.filters.TimeOffCardFilters

class TimeOffCardDao(
    val locationDao: LocationDao,
    val userDao: UserDao,
    val userLocationDao: UserLocationDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickMerchantDao
       with SlickFindAllDao
       with SlickDefaultUpsertDao
       with SlickPreFilteredLocationOptTimeZoneHelper {
  type Record = TimeOffCardRecord
  type Update = TimeOffCardUpdate
  type Filters = TimeOffCardFilters
  type Table = TimeOffCardsTable

  val table = TableQuery[Table]

  def findAllWithFilters(merchantId: UUID, f: Filters)(offset: Int, limit: Int): Future[Seq[Record]] =
    if (f.locationIds.isEmpty)
      Future.successful(Seq.empty)
    else
      queryFindAllByMerchantId(merchantId, f.locationIds, f.query, f.from, f.to)
        .drop(offset)
        .take(limit)
        .result
        .pipe(run)

  def countAllWithFilters(merchantId: UUID, f: Filters): Future[Int] =
    if (f.locationIds.isEmpty)
      Future.successful(0)
    else
      queryFindAllByMerchantId(merchantId, f.locationIds, f.query, f.from, f.to)
        .length
        .result
        .pipe(run)

  def queryFindAllByMerchantId(
      merchantId: UUID,
      locationIds: Seq[UUID] = Seq.empty,
      query: Option[String] = None,
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
    ) =
    table.filter { t =>
      all(
        Some(t.merchantId === merchantId),
        Some(t.userId in userLocationDao.queryFindByLocationIds(locationIds).map(_.userId)),
        query.map(q => t.userId in userDao.querySearchByFullName(q).map(_.id)),
        hasTimeOverlap(t, from, to, Some(locationIds.headOption.toSeq))(_.startAt, _.endAt),
      )
    }
}
