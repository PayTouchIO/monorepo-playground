package io.paytouch.ordering.data.daos.features

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.ordering.Result
import io.paytouch.ordering.data.driver.CustomColumnMappers
import io.paytouch.ordering.data.driver.PaytouchPostgresDriver.api._
import io.paytouch.ordering.data.model.{ SlickRecord, SlickUpdate }
import io.paytouch.ordering.data.tables.features.SlickTable
import io.paytouch.ordering.utils.{ ResultType, UtcTime }
import org.postgresql.util.PSQLException
import slick.dbio.{ DBIOAction, Effect, NoStream }
import slick.lifted.{ CanBeQueryCondition, Rep }

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

trait SlickDao extends SlickCommonDao {

  type Update <: SlickUpdate[Record]

  implicit def db: Database
  implicit def ec: ExecutionContext

  def queryUpsert(entityUpdate: Update) = {
    val id = entityUpdate.id.getOrElse(UUID.randomUUID)
    queryUpsertByQuery(entityUpdate, queryById(id))
  }

  def queryBulkUpsert(upsertions: Seq[Update]) = asSeq(upsertions.map(queryUpsert))

  def queryBulkUpsertByQuery[S <: Effect](upsertions: Seq[Update], query: Update => Query[Table, Record, Seq]) =
    for {
      us <- asSeq(upsertions.map(update => queryUpsertByQuery(update, query(update))))
    } yield us

  def upsert(upsertion: Update): Future[Result[Record]] =
    withOneRetry(runWithTransaction(queryUpsert(upsertion)))

  def bulkUpsert(upsertions: Seq[Update]): Future[Seq[Result[Record]]] =
    if (upsertions.isEmpty) Future.successful(Seq.empty)
    else withOneRetry(runWithTransaction(queryBulkUpsert(upsertions)))

  private def withOneRetry[A](f: => A): A = Try(f).getOrElse(f)

  def queryUpsertByQuery[S <: Effect](upsertion: Update, query: Query[Table, Record, Seq]) =
    for {
      existing <- query.result.headOption
      merged = upsertion.merge(existing)
      upserted <- queryInsertOrUpdate(merged, existing.isEmpty)
    } yield {
      val resultType = if (existing.isEmpty) ResultType.Created else ResultType.Updated
      (resultType, upserted)
    }

  private def queryInsertOrUpdate(record: Record, isCreation: Boolean) =
    if (isCreation)
      try queryInsert(record)
      catch { case _: PSQLException => table.insertOrUpdate(record).map(_ => record) }
    else table.insertOrUpdate(record).map(_ => record)
}

trait SlickCommonDao extends CustomColumnMappers {

  type Record <: SlickRecord
  type Table <: SlickTable[Record]

  implicit def db: Database
  implicit def ec: ExecutionContext

  def table: Query[Table, Record, Seq]

  def idColumnSelector: Table => Rep[UUID] = _.id

  def queryCountAll = table.length.result

  def queryAll = table.result

  def queryDeleteByIds(ids: Seq[UUID]) = queryByIds(ids).delete.map(_ => ids)

  def queryFindById(id: UUID) = queryFindByIds(Seq(id)).result.headOption

  def queryFindByIds(ids: Seq[UUID]) = queryByIds(ids)

  def queryByIds(ids: Seq[UUID]) = table.filter(idColumnSelector(_) inSet ids)

  def queryById(id: UUID) = queryByIds(Seq(id))

  def queryByNotId(id: UUID) = table.filterNot(idColumnSelector(_) === id)

  def asSeq[R, S <: Effect](queries: Seq[DBIOAction[R, NoStream, S]]) = DBIO.sequence(queries)

  def asTraversable[R, S <: Effect](queries: Iterable[DBIOAction[R, NoStream, S]]) = DBIO.sequence(queries)

  def asOption[R, S <: Effect](optionalQuery: Option[DBIOAction[R, NoStream, S]]) = asSeq(optionalQuery.toSeq)

  def any(criterias: Rep[Boolean]*): Rep[Boolean] = optionalFiltering(criterias.map(Some(_)): _*)(_ || _)

  def all(criterias: Option[Rep[Boolean]]*): Rep[Boolean] = optionalFiltering(criterias: _*)(_ && _)

  private def optionalFiltering(criterias: Option[Rep[Boolean]]*)(op: (Rep[Boolean], Rep[Boolean]) => Rep[Boolean]) =
    criterias.toSeq.collect({ case Some(criteria) => criteria }).reduceLeftOption(op).getOrElse(true: Rep[Boolean])

  def run[R](query: DBIOAction[R, NoStream, _]): Future[R] = db.run[R](query)

  def runWithTransaction[R](query: DBIOAction[R, NoStream, _]): Future[R] =
    run[R](query.transactionally)

  def deleteById(id: UUID): Future[UUID] = run(queryDeleteByIds(Seq(id)).map(_ => id))

  def deleteByIds(ids: Seq[UUID]): Future[Seq[UUID]] = {
    if (ids.isEmpty) return Future.successful(Seq.empty)

    run(queryDeleteByIds(ids))
  }

  def findById(id: UUID): Future[Option[Record]] = run(queryFindById(id))

  def findByIds(ids: Seq[UUID]): Future[Seq[Record]] = {
    if (ids.isEmpty) return Future.successful(Seq.empty)

    run(queryFindByIds(ids).result)
  }

  def countAll: Future[Int] = run(queryCountAll)

  protected def queryInsert(entity: Record): DBIOAction[Record, NoStream, Effect.Write with Effect.Read] =
    table returning table += entity

  def queryDeleteTheRestByDeleteFilter[R <: Rep[_]](
      validEntities: Seq[Record],
      deleteFilter: Table => R,
    )(implicit
      wt: CanBeQueryCondition[R],
    ): DBIOAction[Int, NoStream, Effect.Write with Effect.Read] =
    table.filter(deleteFilter).filterNot(idColumnSelector(_) inSet validEntities.map(_.id)).delete

  def queryUpdatedSince(date: ZonedDateTime) = table.filter(_.updatedAt >= date)

  def queryMarkAsUpdatedById(id: UUID) = queryMarkAsUpdatedByIds(Seq(id))

  def queryMarkAsUpdatedByIds(ids: Seq[UUID]) = {
    val field = for { o <- table.filter(_.id inSet ids) } yield o.updatedAt
    field.update(UtcTime.now)
  }
}
