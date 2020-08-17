package io.paytouch.core.data.daos.features

import java.util.UUID

import scala.concurrent._

import slick.dbio.{ DBIOAction, NoStream }
import slick.lifted.Ordered

import io.paytouch.implicits._

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ SlickMerchantRecord, SlickMerchantUpdate }
import io.paytouch.core.data.tables.SlickMerchantTable

trait SlickMerchantDao extends SlickDao {
  type Record <: SlickMerchantRecord
  type Update <: SlickMerchantUpdate[Record]
  type Table <: SlickMerchantTable[Record]

  def findAllByMerchantIdWithOrdering(
      merchantId: UUID,
      offset: Int,
      limit: Int,
    )(
      ordering: Table => Ordered,
    ): Future[Seq[Record]] =
    queryFindAllByMerchantId(merchantId)
      .sortBy(ordering)
      .drop(offset)
      .take(limit)
      .result
      .pipe(run)

  def countAllByMerchantId(merchantId: UUID): Future[Int] =
    queryFindAllByMerchantId(merchantId)
      .length
      .result
      .pipe(run)

  def queryFindAllByMerchantId(merchantId: UUID) =
    queryFindAllByMerchantIds(Seq(merchantId))

  def queryFindAllByMerchantIds(merchantIds: Seq[UUID]) =
    baseQuery
      .filter(_.merchantId inSet merchantIds)
      .sortBy(_.createdAt)

  def findByIdAndMerchantId(id: UUID, merchantId: UUID): Future[Option[Record]] =
    findByIdsAndMerchantId(Seq(id), merchantId).map(_.headOption)

  def findByIdsAndMerchantId(ids: Seq[UUID], merchantId: UUID): Future[Seq[Record]] =
    queryFindAllByMerchantId(merchantId)
      .filter(idColumnSelector(_) inSet ids)
      .result
      .pipe(run)

  def countByIdsAndMerchantId(ids: Seq[UUID], merchantId: UUID): Future[Int] =
    if (ids.isEmpty)
      Future.successful(0)
    else
      queryFindAllByMerchantId(merchantId)
        .filter(idColumnSelector(_) inSet ids)
        .length
        .result
        .pipe(run)

  def deleteByIdAndMerchantId(id: UUID, merchantId: UUID): Future[UUID] =
    deleteByIdsAndMerchantId(Seq(id), merchantId).map(_ => id)

  def deleteByIdsAndMerchantId(ids: Seq[UUID], merchantId: UUID): Future[Seq[UUID]] =
    if (ids.isEmpty)
      Future.successful(Seq.empty)
    else
      queryDeleteByIdsAndMerchantId(ids, merchantId)
        .pipe(runWithTransaction)

  def queryDeleteByIdsAndMerchantId(ids: Seq[UUID], merchantId: UUID): DBIOAction[Seq[UUID], NoStream, Effect.Write] =
    queryFindAllByMerchantId(merchantId)
      .filter(idColumnSelector(_) inSet ids)
      .delete
      .map(_ => ids)

  def findAllByMerchantId(merchantId: UUID): Future[Seq[Record]] =
    queryFindAllByMerchantId(merchantId)
      .result
      .pipe(run)

  def findOneByMerchantId(merchantId: UUID): Future[Option[Record]] =
    queryFindAllByMerchantId(merchantId)
      .result
      .headOption
      .pipe(run)

  def findAllByMerchantIds(merchantIds: Seq[UUID]): Future[Map[UUID, Seq[Record]]] =
    queryFindAllByMerchantIds(merchantIds)
      .result
      .pipe(run)
      .map(_.groupBy(_.merchantId))
}
