package io.paytouch.core.data.daos.features

import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.ProductIdColumn
import io.paytouch.core.data.model._
import io.paytouch.core.data.tables.{ SlickMerchantTable, SlickTable }

import scala.concurrent._

trait SlickProductDao extends SlickFindByProductDao {
  def queryBulkUpsertAndDeleteTheRestByProductId(upsertions: Seq[Update], productId: UUID) =
    queryBulkUpsertAndDeleteTheRestByProductIds(upsertions, Seq(productId))

  def queryBulkUpsertAndDeleteTheRestByProductIds(upsertions: Seq[Update], productIds: Seq[UUID]) =
    for {
      us <- queryBulkUpsert(upsertions)
      records = us.map { case (_, record) => record }
      _ <- queryDeleteTheRestByDeleteFilter(records, _.productId inSet productIds)
    } yield records

  def bulkUpsertAndDeleteTheRestByProductId(upsertions: Seq[Update], productId: UUID): Future[Seq[Record]] =
    bulkUpsertAndDeleteTheRestByProductIds(upsertions, Seq(productId))

  def bulkUpsertAndDeleteTheRestByProductIds(upsertions: Seq[Update], productIds: Seq[UUID]): Future[Seq[Record]] =
    runWithTransaction(queryBulkUpsertAndDeleteTheRestByProductIds(upsertions, productIds))
}

trait SlickFindByProductDao extends SlickMerchantDao {
  type Record <: SlickProductRecord
  type Table <: SlickMerchantTable[Record] with ProductIdColumn

  def findByProductId(productId: UUID): Future[Seq[Record]] =
    findByProductIds(Seq(productId))

  def findByProductIds(productIds: Seq[UUID]): Future[Seq[Record]] =
    if (productIds.isEmpty)
      Future.successful(Seq.empty)
    else
      run(queryFindByProductIds(productIds).result)

  def queryFindByProductId(productId: UUID) =
    queryFindByProductIds(Seq(productId))

  def queryFindByProductIds(productIds: Seq[UUID]) =
    baseQuery.filter(_.productId inSet productIds)
}
