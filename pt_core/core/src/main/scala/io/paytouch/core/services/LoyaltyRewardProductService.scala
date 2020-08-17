package io.paytouch.core.services

import java.time.ZonedDateTime
import java.util.UUID

import cats.implicits._

import io.paytouch.core.conversions.LoyaltyRewardProductConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.entities.{ Id, Pagination, UserContext }
import io.paytouch.core.utils.FindResult.FindResult
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.validators.ProductValidator

import scala.concurrent._

class LoyaltyRewardProductService(implicit val ec: ExecutionContext, val daos: Daos)
    extends LoyaltyRewardProductConversions {

  protected val dao = daos.loyaltyRewardProductDao
  val productValidator = new ProductValidator

  def associateLoyaltyRewardToProducts(
      loyaltyRewardId: UUID,
      productIds: Seq[UUID],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Unit]] =
    productValidator.validateStorableByIds(productIds).flatMapTraverse { storableProducts =>
      val updates = toLoyaltyRewardProductUpdates(loyaltyRewardId, productIds)
      dao.bulkUpsertAndDeleteTheRestByLoyaltyRewardId(updates, loyaltyRewardId).void
    }

  def listProducts(
      loyaltyRewardId: UUID,
      updatedSince: Option[ZonedDateTime],
    )(implicit
      user: UserContext,
      pagination: Pagination,
    ): Future[FindResult[Id]] = {
    val merchantId = user.merchantId

    val recordsR =
      dao.findByMerchantIdAndLoyaltyRewardId(merchantId, loyaltyRewardId, updatedSince)(
        pagination.offset,
        pagination.limit,
      )
    val countR = dao.countAllByMerchantIdAndLoyaltyRewardId(merchantId, loyaltyRewardId, updatedSince)
    for {
      records <- recordsR
      entities = records.map(record => Id(record.productId))
      count <- countR
    } yield (entities, count)
  }
}
