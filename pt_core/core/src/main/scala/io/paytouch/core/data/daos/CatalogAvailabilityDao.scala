package io.paytouch.core.data.daos

import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.enums.AvailabilityItemType

import scala.concurrent.ExecutionContext

class CatalogAvailabilityDao(implicit val ec: ExecutionContext, val db: Database) extends AvailabilityDao {
  def itemType = AvailabilityItemType.Catalog

  def queryDeleteByCatalogIdsAndMerchantId(catalogIds: Seq[UUID], merchantId: UUID) =
    queryByCatalogIds(catalogIds, merchantId).delete

  def queryByCatalogIds(catalogIds: Seq[UUID], merchantId: UUID) =
    baseTable
      .filter(_.itemId inSet catalogIds)
      .filter(_.merchantId === merchantId)
}
