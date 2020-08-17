package io.paytouch.core.data.daos

import java.util.UUID

import io.paytouch.core.data.daos.features.SlickMerchantDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.upsertions.BundleSetUpsertion
import io.paytouch.core.data.model.{ BundleSetRecord, BundleSetUpdate }
import io.paytouch.core.data.tables.BundleSetsTable

import scala.concurrent.ExecutionContext

class BundleSetDao(val bundleOptionDao: BundleOptionDao)(implicit val ec: ExecutionContext, val db: Database)
    extends SlickMerchantDao {

  type Record = BundleSetRecord
  type Update = BundleSetUpdate
  type Table = BundleSetsTable
  type Upsertion = BundleSetUpsertion

  val table = TableQuery[Table]

  def queryBulkUpsertAndDeleteTheRestByProductIds(upsertions: Seq[Upsertion], productIds: Seq[UUID]) =
    for {
      us <- queryBulkUpsert(upsertions.map(_.bundleSetUpdate))
      records = us.map { case (_, record) => record }
      _ <- queryDeleteTheRestByDeleteFilter(records, _.productId inSet productIds)
      _ <- bundleOptionDao.queryBulkUpsertAndDeleteTheRestByBundleSetIds(
        upsertions.flatMap(_.bundleOptionUpdates),
        records.map(_.id),
      )
    } yield records

  def findByProductId(productId: UUID) = run(queryFindByProductId(productId).result)

  def findByProductIds(productIds: Seq[UUID]) = run(queryFindByProductIds(productIds).result)

  private def queryFindByProductId(productId: UUID) = queryFindByProductIds(Seq(productId))

  private def queryFindByProductIds(productIds: Seq[UUID]) = table.filter(_.productId inSet productIds)

}
