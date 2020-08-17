package io.paytouch.ordering.data.daos

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.ordering.data.daos.features._
import io.paytouch.ordering.data.driver.PaytouchPostgresDriver.api._
import io.paytouch.ordering.data.model._
import io.paytouch.ordering.data.model.upsertions.CartItemUpsertion
import io.paytouch.ordering.data.tables.CartItemsTable
import io.paytouch.ordering.Result

class CartItemDao(
    val cartItemModifierOptionDao: CartItemModifierOptionDao,
    val cartItemVariantOptionDao: CartItemVariantOptionDao,
    val cartItemTaxRateDao: CartItemTaxRateDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickStoreDao
       with SlickCartDao
       with SlickUpsertDao {
  type Record = CartItemRecord
  type Update = CartItemUpdate
  type Table = CartItemsTable
  type Upsertion = CartItemUpsertion

  val table = TableQuery[Table]

  def upsert(upsertion: Upsertion): Future[Result[Record]] =
    queryUpsertion(upsertion)
      .pipe(runWithTransaction)

  def queryUpsertion(upsertion: Upsertion) =
    for {
      (resultType, result) <- queryUpsert(upsertion.cartItem)
      _ <- asOption(
        upsertion
          .cartItemModifierOptions
          .map(
            cartItemModifierOptionDao.queryBulkUpsertAndDeleteTheRestByCartItemId(_, result.id),
          ),
      )
      _ <- asOption(
        upsertion
          .cartItemVariantOptions
          .map(
            cartItemVariantOptionDao.queryBulkUpsertAndDeleteTheRestByCartItemId(_, result.id),
          ),
      )
      _ <- asOption(
        upsertion
          .cartItemTaxRates
          .map(
            cartItemTaxRateDao.queryBulkUpsertAndDeleteTheRestByCartItemId(_, result.id),
          ),
      )
    } yield resultType -> result
}
