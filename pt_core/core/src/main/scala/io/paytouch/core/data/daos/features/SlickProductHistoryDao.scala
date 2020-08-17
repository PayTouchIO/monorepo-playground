package io.paytouch.core.data.daos.features

import java.time.LocalDateTime
import java.util.UUID

import io.paytouch.core.data.daos.LocationDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.{ LocationIdColumn, ProductHistoryColumns }
import io.paytouch.core.data.tables.SlickMerchantTable
import io.paytouch.core.filters.ProductHistoryFilters

import scala.concurrent._

trait SlickProductHistoryDao extends SlickMerchantDao with SlickFindAllDao with SlickLocationTimeZoneHelper {
  type Table <: SlickMerchantTable[Record] with ProductHistoryColumns with LocationIdColumn
  type Filters <: ProductHistoryFilters

  def locationDao: LocationDao

  def findAllWithFilters(merchantId: UUID, f: Filters)(offset: Int, limit: Int): Future[Seq[Record]] =
    run(queryFindAllByMerchantIdAndProductId(merchantId, f.productId, f.from, f.to).drop(offset).take(limit).result)

  def countAllWithFilters(merchantId: UUID, f: Filters): Future[Int] =
    run(queryFindAllByMerchantIdAndProductId(merchantId, f.productId, f.from, f.to).length.result)

  def queryFindAllByMerchantIdAndProductId(
      merchantId: UUID,
      productId: UUID,
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
    ) =
    queryFindAllByMerchantId(merchantId)
      .filter(_.productId === productId)
      .filter { t =>
        all(
          from.map(start => t.id in itemIdsAtOrAfterDate(start)(_.date)),
          to.map(end => t.id in itemIdsBeforeDate(end)(_.date)),
        )
      }
      .sortBy(_.date.desc)

  def findAllByProductIds(productIds: Seq[UUID]): Future[Seq[Record]] =
    if (productIds.isEmpty)
      Future.successful(Seq.empty)
    else
      run(queryFindAllByProductIds(productIds).result)

  def findAllByProductIdsAndLocationId(productIds: Seq[UUID], locationId: UUID): Future[Seq[Record]] =
    if (productIds.isEmpty)
      Future.successful(Seq.empty)
    else
      run(queryFindAllByProductIds(productIds).filter(_.locationId === locationId).result)

  private def queryFindAllByProductIds(productIds: Seq[UUID]) =
    baseQuery.filter(_.productId inSet productIds)
}
