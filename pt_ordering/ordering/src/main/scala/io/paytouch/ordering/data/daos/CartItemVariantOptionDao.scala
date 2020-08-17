package io.paytouch.ordering.data.daos

import io.paytouch.ordering.data.daos.features.{ SlickCartItemDao, SlickStoreDao }
import io.paytouch.ordering.data.driver.PaytouchPostgresDriver.api._
import io.paytouch.ordering.data.model.{ CartItemVariantOptionRecord, CartItemVariantOptionUpdate }
import io.paytouch.ordering.data.tables.CartItemVariantOptionsTable

import scala.concurrent.ExecutionContext

class CartItemVariantOptionDao(implicit val ec: ExecutionContext, val db: Database)
    extends SlickStoreDao
       with SlickCartItemDao {

  type Record = CartItemVariantOptionRecord
  type Update = CartItemVariantOptionUpdate
  type Table = CartItemVariantOptionsTable

  val table = TableQuery[Table]

}
