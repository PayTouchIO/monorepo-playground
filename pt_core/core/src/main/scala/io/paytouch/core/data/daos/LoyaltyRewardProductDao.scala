package io.paytouch.core.data.daos

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.daos.features.SlickRelDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ LoyaltyRewardProductRecord, LoyaltyRewardProductUpdate }
import io.paytouch.core.data.tables.LoyaltyRewardProductsTable

import scala.concurrent._

class LoyaltyRewardProductDao(
    articleDao: => ArticleDao,
    loyaltyRewardDao: => LoyaltyRewardDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickRelDao {

  type Record = LoyaltyRewardProductRecord
  type Update = LoyaltyRewardProductUpdate
  type Table = LoyaltyRewardProductsTable

  val table = TableQuery[Table]

  def queryByRelIds(upsertion: Update) = {
    require(
      upsertion.productId.isDefined,
      "LoyaltyRewardProductDao - Impossible to find by product id and loyalty reward id without a product id",
    )
    require(
      upsertion.loyaltyRewardId.isDefined,
      "LoyaltyRewardProductDao - Impossible to find by product id and loyalty reward id without a loyal reward id",
    )
    queryFindByProductIdAndLoyaltyRewardId(upsertion.productId.get, upsertion.loyaltyRewardId.get)
  }

  def queryFindByProductIdAndLoyaltyRewardId(productId: UUID, loyaltyRewardId: UUID) =
    queryFindByLoyaltyRewardId(loyaltyRewardId).filter(_.productId === productId)

  def queryFindByLoyaltyRewardId(loyaltyRewardId: UUID) = queryFindByLoyaltyRewardIds(Seq(loyaltyRewardId))

  def queryFindByLoyaltyRewardIds(loyaltyRewardIds: Seq[UUID]) =
    table.filter(_.loyaltyRewardId inSet loyaltyRewardIds)

  def queryBulkUpsertAndDeleteTheRestByLoyaltyRewardId(loyaltyRewardProducts: Seq[Update], loyaltyRewardId: UUID) =
    for {
      oldProductIds <- queryFindByLoyaltyRewardId(loyaltyRewardId).map(_.productId).result
      newProductIds = loyaltyRewardProducts.flatMap(_.productId)
      updates <- queryBulkUpsertAndDeleteTheRestByRelIds(loyaltyRewardProducts, _.loyaltyRewardId === loyaltyRewardId)
      _ <- loyaltyRewardDao.queryMarkAsUpdatedById(loyaltyRewardId)
      _ <- articleDao.queryMarkAsUpdatedByIds(oldProductIds ++ newProductIds)
    } yield updates

  def bulkUpsertAndDeleteTheRestByLoyaltyRewardId(loyaltyRewardProducts: Seq[Update], loyaltyRewardId: UUID) =
    runWithTransaction(queryBulkUpsertAndDeleteTheRestByLoyaltyRewardId(loyaltyRewardProducts, loyaltyRewardId))

  def findByLoyaltyRewardId(loyaltyRewardId: UUID) =
    run(queryFindByLoyaltyRewardId(loyaltyRewardId).result)

  def findByMerchantIdAndLoyaltyRewardId(
      merchantId: UUID,
      loyaltyRewardId: UUID,
      updatedSince: Option[ZonedDateTime],
    )(
      offset: Int,
      limit: Int,
    ): Future[Seq[Record]] = {
    val query = queryFindAllByMerchantIdAndLoyaltyRewardId(merchantId, loyaltyRewardId, updatedSince)
      .sortBy(_.createdAt)
      .drop(offset)
      .take(limit)
    run(query.result)
  }

  def countAllByMerchantIdAndLoyaltyRewardId(
      merchantId: UUID,
      loyaltyRewardId: UUID,
      updatedSince: Option[ZonedDateTime],
    ): Future[Int] =
    run(queryFindAllByMerchantIdAndLoyaltyRewardId(merchantId, loyaltyRewardId, updatedSince).length.result)

  def queryFindAllByMerchantIdAndLoyaltyRewardId(
      merchantId: UUID,
      loyaltyRewardId: UUID,
      updatedSince: Option[ZonedDateTime],
    ) =
    queryFindByLoyaltyRewardId(loyaltyRewardId)
      .filterByMerchantId(merchantId)
      .filterByOptUpdatedSince(updatedSince)

}
