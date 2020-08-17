package io.paytouch.core.data.daos

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.daos.features.{ SlickFindAllDao, SlickMerchantDao }
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.EventRecord
import io.paytouch.core.data.tables.EventsTable
import io.paytouch.core.entities.enums.{ ExposedName, TrackableAction }
import io.paytouch.core.filters.EventFilters

import scala.concurrent._

class EventDao(implicit val ec: ExecutionContext, val db: Database) extends SlickMerchantDao with SlickFindAllDao {

  type Record = EventRecord
  type Filters = EventFilters
  type Table = EventsTable

  lazy val table = TableQuery[Table]

  def insert(event: EventRecord): Future[EventRecord] = runWithTransaction(queryInsert(event))

  def findAllWithFilters(merchantId: UUID, f: Filters)(offset: Int, limit: Int): Future[Seq[EventRecord]] =
    run(
      queryFindAllByMerchantId(merchantId, f.updatedSince, f.action, f.`object`)
        .sortBy(_.receivedAt)
        .drop(offset)
        .take(limit)
        .result,
    )

  def countAllWithFilters(merchantId: UUID, f: Filters): Future[Int] =
    run(queryFindAllByMerchantId(merchantId, f.updatedSince, f.action, f.`object`).length.result)

  def queryFindAllByMerchantId(
      merchantId: UUID,
      updatedSince: ZonedDateTime,
      action: Option[TrackableAction],
      `object`: Option[ExposedName],
    ) =
    table
      .filter(_.merchantId === merchantId)
      .filter(_.receivedAt >= updatedSince)
      .filter(t =>
        all(
          action.map(a => t.action === a),
          `object`.map(o => t.`object` === o),
        ),
      )
}
