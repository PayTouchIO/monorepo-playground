package io.paytouch.core.data.daos

import io.paytouch.core.data.daos.features.SlickMerchantDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ OrderDeliveryAddressRecord, OrderDeliveryAddressUpdate }
import io.paytouch.core.data.tables.OrderDeliveryAddressesTable

import scala.concurrent.ExecutionContext

class OrderDeliveryAddressDao(implicit val ec: ExecutionContext, val db: Database) extends SlickMerchantDao {

  type Record = OrderDeliveryAddressRecord
  type Update = OrderDeliveryAddressUpdate
  type Table = OrderDeliveryAddressesTable

  val table = TableQuery[Table]

}
