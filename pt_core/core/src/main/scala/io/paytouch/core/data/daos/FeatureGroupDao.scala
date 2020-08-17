package io.paytouch.core.data.daos

import io.paytouch.core.data.daos.features.SlickDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.FeatureGroupRecord
import io.paytouch.core.data.tables.FeatureGroupsTable

import scala.concurrent._

class FeatureGroupDao(implicit val ec: ExecutionContext, val db: Database) extends SlickDao {
  type Record = FeatureGroupRecord
  type Table = FeatureGroupsTable

  val table = TableQuery[Table]

  def findAll() = run(table.sortBy(_.name).result)
}
