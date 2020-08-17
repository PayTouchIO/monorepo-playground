package io.paytouch.core.data.daos

import io.paytouch.core.data.daos.features.SlickMerchantDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ MerchantConfigurationRecord, MerchantConfigurationUpdate }
import io.paytouch.core.data.tables.MerchantConfigurationsTable

import scala.concurrent.ExecutionContext

class MerchantConfigurationDao(implicit val ec: ExecutionContext, val db: Database) extends SlickMerchantDao {

  type Record = MerchantConfigurationRecord
  type Update = MerchantConfigurationUpdate
  type Table = MerchantConfigurationsTable

  val table = TableQuery[Table]
}
