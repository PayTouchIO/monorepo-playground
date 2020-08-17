package io.paytouch.core.data.daos

import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.enums.AvailabilityItemType

import scala.concurrent.ExecutionContext

class CategoryLocationAvailabilityDao(
    categoryLocationDao: => CategoryLocationDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends AvailabilityDao {
  def itemType = AvailabilityItemType.CategoryLocation

  def queryDeleteByCategoryIdsAndMerchantId(categoryIds: Seq[UUID], merchantId: UUID) =
    queryByCategoryIds(categoryIds, merchantId).delete

  def queryByCategoryIds(categoryIds: Seq[UUID], merchantId: UUID) =
    baseTable
      .filter(_.itemId in categoryLocationDao.queryFindByItemIds(categoryIds).map(_.id))
      .filter(_.merchantId === merchantId)
}
