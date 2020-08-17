package io.paytouch.ordering.data.daos

import java.util.UUID

import io.paytouch.ordering.data.daos.features.SlickCartDao
import io.paytouch.ordering.data.driver.PaytouchPostgresDriver.api._
import io.paytouch.ordering.data.model.{ CartTaxRateRecord, CartTaxRateUpdate, SlickRelDao }
import io.paytouch.ordering.data.tables.CartTaxRatesTable

import scala.concurrent.ExecutionContext

class CartTaxRateDao(implicit val ec: ExecutionContext, val db: Database) extends SlickRelDao with SlickCartDao {

  type Record = CartTaxRateRecord
  type Update = CartTaxRateUpdate
  type Table = CartTaxRatesTable

  val table = TableQuery[Table]

  def queryByRelIds(upsertion: CartTaxRateUpdate) = {
    requires("cart id" -> upsertion.cartId, "tax rate id" -> upsertion.taxRateId)
    queryByCartIdAndTaxRateId(upsertion.cartId.get, upsertion.taxRateId.get)
  }

  private def queryByCartIdAndTaxRateId(cartId: UUID, taxRateId: UUID) =
    table.filter(_.cartId === cartId).filter(_.taxRateId === taxRateId)
}
