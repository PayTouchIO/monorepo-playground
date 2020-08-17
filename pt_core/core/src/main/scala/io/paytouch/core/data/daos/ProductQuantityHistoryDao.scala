package io.paytouch.core.data.daos

import scala.concurrent._

import io.paytouch.core.data.daos.features.SlickProductHistoryDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model._
import io.paytouch.core.data.tables.ProductQuantityHistoryTable
import io.paytouch.core.filters.ProductHistoryFilters

class ProductQuantityHistoryDao(val locationDao: LocationDao)(implicit val ec: ExecutionContext, val db: Database)
    extends SlickProductHistoryDao {
  type Record = ProductQuantityHistoryRecord
  type Update = ProductQuantityHistoryUpdate
  type Filters = ProductHistoryFilters
  type Table = ProductQuantityHistoryTable

  val table = TableQuery[Table]
}
