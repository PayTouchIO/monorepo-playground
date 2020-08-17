package io.paytouch.core.data.daos.features

import java.time.ZonedDateTime
import java.util.UUID

import scala.concurrent._
import scala.util.Try

import slick.dbio.{ DBIOAction, Effect, NoStream }
import slick.lifted.{ CanBeQueryCondition, ColumnOrdered, Rep, TableQuery }

import org.postgresql.util.PSQLException

import io.paytouch.implicits._

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ SlickRecord, SlickUpdate }
import io.paytouch.core.data.tables.SlickTable
import io.paytouch.core.utils._

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
    asSeq(upsertions.map(update => queryUpsertByQuery(update, query(update))))

  def upsert(upsertion: Update): Future[(ResultType, Record)] =
    withOneRetry(runWithTransaction(queryUpsert(upsertion)))

  def bulkUpsert(upsertions: Seq[Update]): Future[Seq[(ResultType, Record)]] =
    if (upsertions.isEmpty)
      Future.successful(Seq.empty)
    else
      withOneRetry(runWithTransaction(queryBulkUpsert(upsertions)))

  private def withOneRetry[A](f: => A): A = Try(f).getOrElse(f)

  final def queryUpsertByQuery[S <: Effect](upsertion: Update, query: Query[Table, Record, Seq]) =
    for {
      existing <- query.result.headOption
      merged = upsertion.merge(existing)
      upserted <- queryInsertOrUpdate(merged, existing.isEmpty)
    } yield {
      val resultType =
        if (existing.isEmpty)
          ResultType.Created
        else
          ResultType.Updated

      resultType -> upserted
    }

  private def queryInsertOrUpdate(record: Record, isCreation: Boolean) =
    if (isCreation)
      try queryInsert(record)
      catch {
        case _: PSQLException =>
          table
            .insertOrUpdate(record)
            .map(_ => record)
      }
    else
      table
        .insertOrUpdate(record)
        .map(_ => record)

  def queryBulkUpsertAndDeleteTheRest[R <: Rep[_], S <: Effect](
      upsertions: Seq[Update],
      query: Table => R,
    )(implicit
      wt: CanBeQueryCondition[R],
    ) =
    for {
      us <- queryBulkUpsert(upsertions)
      records = us.map { case (_, record) => record }
      _ <- queryDeleteTheRestByDeleteFilter(records, query)
    } yield records
}

trait SlickCommonDao {
  type Record <: SlickRecord
  type Table <: SlickTable[Record]

  implicit def db: Database
  implicit def ec: ExecutionContext

  protected def table: TableQuery[Table]

  def baseQuery: Query[Table, Record, Seq] =
    table

  def idColumnSelector: Table => Rep[UUID] =
    _.id

  def queryCountAll =
    baseQuery.length.result

  def queryAll =
    baseQuery.result

  def queryDeleteByIds(ids: Seq[UUID]) =
    queryByIds(ids).delete.map(_ => ids)

  def queryFindById(id: UUID) =
    queryFindByIds(Seq(id)).result.headOption

  def queryFindByIds(ids: Seq[UUID]) =
    queryByIds(ids)

  def queryByIds(ids: Seq[UUID]) =
    baseQuery.filter(idColumnSelector(_) inSet ids)

  def queryById(id: UUID) =
    queryByIds(Seq(id))

  def asSeq[R, S <: Effect](queries: Seq[DBIOAction[R, NoStream, S]]) =
    DBIO.sequence(queries)

  def asTraversable[R, S <: Effect](queries: Iterable[DBIOAction[R, NoStream, S]]) =
    DBIO.sequence(queries)

  def asOption[R, S <: Effect](optionalQuery: Option[DBIOAction[R, NoStream, S]]) =
    asSeq(optionalQuery.toSeq)

  def any(criterias: Rep[Boolean]*): Rep[Boolean] =
    optionalFiltering(criterias.map(Some(_)): _*)(_ || _)

  def all(criterias: Option[Rep[Boolean]]*): Rep[Boolean] =
    optionalFiltering(criterias: _*)(_ && _)

  private def optionalFiltering(
      criterias: Option[Rep[Boolean]]*,
    )(
      op: (Rep[Boolean], Rep[Boolean]) => Rep[Boolean],
    ): Rep[Boolean] = {
    val flattened: Seq[Rep[Boolean]] =
      criterias.flatten

    if (flattened.isEmpty)
      true
    else
      flattened.reduceLeft(op)
  }

  def run[R](query: DBIOAction[R, NoStream, _]): Future[R] =
    db.run[R](query)

  def runWithTransaction[R](query: DBIOAction[R, NoStream, _]): Future[R] =
    run[R](query.transactionally)

  def deleteById(id: UUID): Future[UUID] =
    run(queryDeleteByIds(Seq(id)).map(_ => id))

  def deleteByIds(ids: Seq[UUID]): Future[Seq[UUID]] =
    if (ids.isEmpty)
      Future.successful(Seq.empty)
    else
      run(queryDeleteByIds(ids))

  def findById(id: UUID): Future[Option[Record]] =
    run(queryFindById(id))

  def findByIds(ids: Seq[UUID]): Future[Seq[Record]] =
    if (ids.isEmpty)
      Future.successful(Seq.empty)
    else
      run(queryFindByIds(ids).result)

  def countAll: Future[Int] = run(queryCountAll)

  protected def queryInsert(entity: Record): DBIOAction[Record, NoStream, Effect.Write with Effect.Read] =
    table returning table += entity

  def queryDeleteTheRestByDeleteFilter[R <: Rep[_]](
      validEntities: Seq[Record],
      deleteFilter: Table => R,
    )(implicit
      wt: CanBeQueryCondition[R],
    ): DBIOAction[Int, NoStream, Effect.Write with Effect.Read] =
    table
      .filter(deleteFilter)
      .filterNot(idColumnSelector(_) inSet validEntities.map(_.id))
      .delete

  def queryUpdatedSince(date: ZonedDateTime) =
    baseQuery.filter(_.updatedAt >= date)

  def queryMarkAsUpdatedById(id: UUID) =
    queryMarkAsUpdatedByIds(Seq(id))

  def queryMarkAsUpdatedByIds(ids: Seq[UUID]) =
    table
      .filter(_.id inSet ids)
      .map(_.updatedAt)
      .update(UtcTime.now)
}
