package io.paytouch.core.data.daos

import java.time.LocalDateTime
import java.util.UUID

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.core.data.daos.features.{ SlickFindAllDao, SlickSoftDeleteDao }
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ UserRecord, UserUpdate }
import io.paytouch.core.data.tables.UsersTable
import io.paytouch.core.filters.PayrollFilters

class PayrollDao(val userLocationDao: UserLocationDao)(implicit val ec: ExecutionContext, val db: Database)
    extends SlickFindAllDao
       with SlickSoftDeleteDao {
  type Record = UserRecord
  type Update = UserUpdate
  type Filters = PayrollFilters
  type Table = UsersTable

  val table = TableQuery[Table]

  def findAllWithFilters(merchantId: UUID, f: Filters)(offset: Int, limit: Int): Future[Seq[Record]] =
    if (f.locationIds.isEmpty)
      Future.successful(Seq.empty)
    else
      queryFindAllByMerchantId(merchantId, f.locationIds, f.from, f.to, f.query)
        .sortBy(t => (t.firstName, t.lastName))
        .drop(offset)
        .take(limit)
        .result
        .pipe(run)

  def countAllWithFilters(merchantId: UUID, f: Filters): Future[Int] =
    if (f.locationIds.isEmpty)
      Future.successful(0)
    else
      queryFindAllByMerchantId(merchantId, f.locationIds, f.from, f.to, f.query)
        .length
        .result
        .pipe(run)

  def queryFindAllByMerchantId(
      merchantId: UUID,
      locationIds: Seq[UUID] = Seq.empty,
      from: Option[LocalDateTime] = None,
      to: Option[LocalDateTime] = None,
      query: Option[String],
    ) =
    nonDeletedTable.filter { t =>
      all(
        Some(t.merchantId === merchantId),
        Some(t.id in userLocationDao.queryFindByLocationIds(locationIds).map(_.userId)),
        query.map(q => querySearchUser(t, q)),
      )
    }

  private def querySearchUser(t: Table, q: String) =
    querySearchByFirstName(t, q) || querySearchByLastName(t, q) || querySearchByEmail(t, q)

  private def querySearchByFirstName(t: Table, q: String) =
    t.firstName.toLowerCase like s"%${q.toLowerCase}%"

  private def querySearchByLastName(t: Table, q: String) =
    t.lastName.toLowerCase like s"%${q.toLowerCase}%"

  private def querySearchByEmail(t: Table, q: String) =
    t.email.toLowerCase like s"%${q.toLowerCase}%"
}
