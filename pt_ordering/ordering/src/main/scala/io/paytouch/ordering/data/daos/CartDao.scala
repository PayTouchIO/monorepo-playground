package io.paytouch.ordering.data.daos

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.ordering.data.daos.features.{ SlickStoreDao, SlickUpsertDao }
import io.paytouch.ordering.data.driver.PaytouchPostgresDriver.api._
import io.paytouch.ordering.data.model.{ CartRecord, CartUpdate }
import io.paytouch.ordering.data.model.upsertions.CartUpsertion
import io.paytouch.ordering.data.tables.CartsTable
import io.paytouch.ordering.utils.ResultType

class CartDao(
    cartItemDao: CartItemDao,
    cartTaxRateDao: CartTaxRateDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickUpsertDao
       with SlickStoreDao {
  type Record = CartRecord
  type Table = CartsTable
  type Update = CartUpdate
  type Upsertion = CartUpsertion

  val table = TableQuery[Table]

  def upsert(upsertion: CartUpsertion): Future[(ResultType, CartRecord)] =
    upsertion
      .pipe(queryUpsertion)
      .pipe(runWithTransaction)

  private def queryUpsertion(upsertion: CartUpsertion) =
    for {
      r @ (resultType, result) <- queryUpsert(upsertion.cart)
      _ <- asSeq(upsertion.cartItems.map(cartItemDao.queryUpsertion))
      _ <- asSeq(upsertion.cartTaxRates.map(cartTaxRateDao.queryUpsert))
    } yield r
}
