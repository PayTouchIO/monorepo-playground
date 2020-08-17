package io.paytouch.ordering.data.daos

import io.paytouch.ordering.data.daos.features.{ SlickCartItemDao, SlickStoreDao }
import io.paytouch.ordering.data.driver.PaytouchPostgresDriver.api._
import io.paytouch.ordering.data.model.{ CartItemModifierOptionRecord, CartItemModifierOptionUpdate }
import io.paytouch.ordering.data.tables.CartItemModifierOptionsTable

import scala.concurrent.ExecutionContext

class CartItemModifierOptionDao(implicit val ec: ExecutionContext, val db: Database)
    extends SlickStoreDao
       with SlickCartItemDao {

  type Record = CartItemModifierOptionRecord
  type Update = CartItemModifierOptionUpdate
  type Table = CartItemModifierOptionsTable

  val table = TableQuery[Table]
}
