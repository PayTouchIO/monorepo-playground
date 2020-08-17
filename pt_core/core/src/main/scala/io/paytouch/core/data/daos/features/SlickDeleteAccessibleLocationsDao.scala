package io.paytouch.core.data.daos.features

import io.paytouch.core.data.daos._
import java.util.UUID

import io.paytouch.core.data.daos.LocationDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.OneToOneLocationColumns
import io.paytouch.core.data.tables.SlickMerchantTable

import scala.concurrent._

trait SlickDeleteAccessibleLocationsDao extends SlickMerchantDao {

  type Table <: SlickMerchantTable[Record] with OneToOneLocationColumns

  implicit val locationDao: LocationDao

  def deleteByIdsAndMerchantIdAndLocationIds(
      ids: Seq[UUID],
      merchantId: UUID,
      locationIds: Seq[UUID],
    ): Future[Seq[UUID]] = {
    val q = for {
      items <- queryFindByIdsAndMerchantIdAndLocationIds(ids, merchantId, locationIds)
      deletedIds <- queryDeleteByIds(items.map(_.id))
    } yield deletedIds
    run(q)
  }

  def queryFindByIdsAndMerchantIdAndLocationIds(
      ids: Seq[UUID],
      merchantId: UUID,
      locationIds: Seq[UUID],
    ) =
    queryFindAllByMerchantId(merchantId).filter(idColumnSelector(_) inSet ids).filterByLocationIds(locationIds).result
}
