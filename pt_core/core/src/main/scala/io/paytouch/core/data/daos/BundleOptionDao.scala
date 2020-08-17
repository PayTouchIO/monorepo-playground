package io.paytouch.core.data.daos

import java.util.UUID

import io.paytouch.core.data.daos.features.SlickMerchantDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ BundleOptionRecord, BundleOptionUpdate }
import io.paytouch.core.data.tables.BundleOptionsTable

import scala.concurrent.ExecutionContext

class BundleOptionDao(implicit val ec: ExecutionContext, val db: Database) extends SlickMerchantDao {

  type Record = BundleOptionRecord
  type Update = BundleOptionUpdate
  type Table = BundleOptionsTable

  val table = TableQuery[Table]

  def queryBulkUpsertAndDeleteTheRestByBundleSetIds(upsertions: Seq[BundleOptionUpdate], bundleSetIds: Seq[UUID]) =
    for {
      us <- queryBulkUpsert(upsertions)
      records = us.map { case (_, record) => record }
      _ <- queryDeleteTheRestByDeleteFilter(records, _.bundleSetId inSet bundleSetIds)
    } yield records

  def findByBundleSetIds(bundleSetIds: Seq[UUID]) = run(queryFindByBundleSetIds(bundleSetIds).result)

  private def queryFindByBundleSetIds(bundleSetIds: Seq[UUID]) = table.filter(_.bundleSetId inSet bundleSetIds)
}
