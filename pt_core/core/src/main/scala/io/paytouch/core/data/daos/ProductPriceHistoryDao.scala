package io.paytouch.core.data.daos

import io.paytouch.core.data.daos.features.SlickProductHistoryDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ ProductPriceHistoryRecord, ProductPriceHistoryUpdate }
import io.paytouch.core.data.tables.ProductPriceHistoryTable
import io.paytouch.core.filters.ProductHistoryFilters

import scala.concurrent.ExecutionContext

class ProductPriceHistoryDao(val locationDao: LocationDao)(implicit val ec: ExecutionContext, val db: Database)
    extends SlickProductHistoryDao {

  type Record = ProductPriceHistoryRecord
  type Update = ProductPriceHistoryUpdate
  type Filters = ProductHistoryFilters
  type Table = ProductPriceHistoryTable

  val table = TableQuery[Table]
}
