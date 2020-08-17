package io.paytouch.core.data.daos.features

import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.{ ActiveColumn, ManyItemsToManyLocationsColumns }
import io.paytouch.core.data.model.SlickToggleableRecord
import io.paytouch.core.data.tables.SlickMerchantTable
import io.paytouch.core.entities.UpdateActiveLocation
import io.paytouch.core.utils.UtcTime

trait SlickItemLocationToggleableDao extends SlickItemLocationDao {
  type Record <: SlickToggleableRecord
  type Table <: SlickMerchantTable[Record] with ActiveColumn with ManyItemsToManyLocationsColumns

  def itemDao: SlickMerchantDao

  def updateActiveByRelIds(id: UUID, updateActiveLocations: Seq[UpdateActiveLocation]) =
    runWithTransaction(queryBulkUpdateActiveByRelIds(id, updateActiveLocations))

  private def queryBulkUpdateActiveByRelIds(id: UUID, updateActiveLocations: Seq[UpdateActiveLocation]) =
    asTraversable(updateActiveLocations.map(ual => queryUpdateActiveByRelIds(id, ual.locationId, ual.active)))

  private def queryUpdateActiveByRelIds(
      itemId: UUID,
      locationId: UUID,
      active: Boolean,
    ) =
    for {
      updates <- updateItemLocations(itemId, locationId, active)
      _ <- locationDao.queryMarkAsUpdatedById(locationId)
      _ <- itemDao.queryMarkAsUpdatedById(itemId)
    } yield updates

  private def updateItemLocations(
      itemId: UUID,
      locationId: UUID,
      active: Boolean,
    ) =
    queryFindByItemIdAndLocationId(itemId, locationId)
      .map(r => r.active -> r.updatedAt)
      .update(active, UtcTime.now)
}
