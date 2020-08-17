package io.paytouch.ordering.data.daos

import io.paytouch.ordering.data.daos.features.{ SlickCartItemDao, SlickStoreDao }
import io.paytouch.ordering.data.driver.PaytouchPostgresDriver.api._
import io.paytouch.ordering.data.model.{ CartItemTaxRateRecord, CartItemTaxRateUpdate }
import io.paytouch.ordering.data.tables.CartItemTaxRatesTable

import scala.concurrent.ExecutionContext

class CartItemTaxRateDao(implicit val ec: ExecutionContext, val db: Database)
    extends SlickStoreDao
       with SlickCartItemDao {

  type Record = CartItemTaxRateRecord
  type Update = CartItemTaxRateUpdate
  type Table = CartItemTaxRatesTable

  val table = TableQuery[Table]

}
