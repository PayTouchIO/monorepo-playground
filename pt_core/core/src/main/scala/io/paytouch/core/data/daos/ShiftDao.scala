package io.paytouch.core.data.daos

import java.time.LocalDate
import java.util.UUID

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.core.data.daos.features.{ SlickDefaultUpsertDao, SlickDeleteAccessibleLocationsDao, SlickFindAllDao }
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ ShiftRecord, ShiftUpdate }
import io.paytouch.core.data.model.enums.ShiftStatus
import io.paytouch.core.data.tables.ShiftsTable
import io.paytouch.core.filters.ShiftFilters

class ShiftDao(
    val locationDao: LocationDao,
    val timeCardDao: TimeCardDao,
    val userDao: UserDao,
    val userLocationDao: UserLocationDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickFindAllDao
       with SlickDefaultUpsertDao
       with SlickDeleteAccessibleLocationsDao {
  type Record = ShiftRecord
  type Update = ShiftUpdate
  type Filters = ShiftFilters
  type Table = ShiftsTable

  val table = TableQuery[Table]

  implicit val l: LocationDao = locationDao

  def findAllWithFilters(merchantId: UUID, f: Filters)(offset: Int, limit: Int): Future[Seq[Record]] =
    if (f.locationIds.isEmpty)
      Future.successful(Seq.empty)
    else
      queryFindAllByMerchantId(merchantId, f.locationIds, f.from, f.to, f.userRoleId, f.status)
        .sortBy(_.createdAt)
        .drop(offset)
        .take(limit)
        .result
        .pipe(run)

  def countAllWithFilters(merchantId: UUID, f: Filters): Future[Int] =
    if (f.locationIds.isEmpty)
      Future.successful(0)
    else
      queryFindAllByMerchantId(merchantId, f.locationIds, f.from, f.to, f.userRoleId, f.status)
        .length
        .result
        .pipe(run)

  def queryFindAllByMerchantId(
      merchantId: UUID,
      locationIds: Seq[UUID] = Seq.empty,
      from: Option[LocalDate] = None,
      to: Option[LocalDate] = None,
      userRoleId: Option[UUID] = None,
      status: Option[ShiftStatus] = None,
    ) =
    table.filter { t =>
      all(
        Some(t.merchantId === merchantId),
        Some(t.locationId inSet locationIds),
        status.map(st => (t.status === st).getOrElse(false)),
        userRoleId.map(uRId => t.userId in userDao.queryFindByUserRoleId(uRId).map(_.id)),
        from.map(start => t.endDate >= start),
        to.map(end => t.startDate <= end),
      )
    }

  def findByIdsAndLocationIds(ids: Seq[UUID], locationIds: Seq[UUID]) =
    queryFindByIdsAndLocationIds(ids, locationIds)
      .result
      .pipe(run)

  def queryFindByIdsAndLocationIds(ids: Seq[UUID], locationIds: Seq[UUID]) =
    queryFindByIds(ids).filterByLocationIds(locationIds)

  def findByUserIdLocationIdAndStartDate(
      userId: UUID,
      locationId: UUID,
      startDate: LocalDate,
    ): Future[Seq[ShiftRecord]] =
    table
      .filter(_.userId === userId)
      .filter(_.locationId === locationId)
      .filter(t => (startDate: Rep[LocalDate]).between(t.startDate, t.endDate))
      .result
      .pipe(run)
}
