package io.paytouch.core.data.daos

import io.paytouch.core.data.daos.features.SlickProductHistoryDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ ProductCostHistoryRecord, ProductCostHistoryUpdate }
import io.paytouch.core.data.tables.ProductCostHistoryTable
import io.paytouch.core.filters.ProductHistoryFilters

import scala.concurrent.ExecutionContext

class ProductCostHistoryDao(val locationDao: LocationDao)(implicit val ec: ExecutionContext, val db: Database)
    extends SlickProductHistoryDao {

  type Record = ProductCostHistoryRecord
  type Update = ProductCostHistoryUpdate
  type Filters = ProductHistoryFilters
  type Table = ProductCostHistoryTable

  val table = TableQuery[Table]
}
