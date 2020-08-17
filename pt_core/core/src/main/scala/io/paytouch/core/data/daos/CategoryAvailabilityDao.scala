package io.paytouch.core.data.daos

import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.enums.AvailabilityItemType

import scala.concurrent.ExecutionContext

class CategoryAvailabilityDao(implicit val ec: ExecutionContext, val db: Database) extends AvailabilityDao {
  def itemType = AvailabilityItemType.Category

  def queryDeleteByCategoryIdsAndMerchantId(categoryIds: Seq[UUID], merchantId: UUID) =
    queryByCategoryIds(categoryIds, merchantId).delete

  def queryByCategoryIds(categoryIds: Seq[UUID], merchantId: UUID) =
    baseTable
      .filter(_.itemId inSet categoryIds)
      .filter(_.merchantId === merchantId)
}
