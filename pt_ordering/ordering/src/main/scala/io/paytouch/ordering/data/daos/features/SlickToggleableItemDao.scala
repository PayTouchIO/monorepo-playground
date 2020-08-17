package io.paytouch.ordering.data.daos.features

import java.util.UUID

import io.paytouch.ordering.data.driver.PaytouchPostgresDriver.api._
import io.paytouch.ordering.data.model.SlickToggleableRecord
import io.paytouch.ordering.data.tables.features.{ ActiveColumn, LocationIdColumn, SlickTable }
import io.paytouch.ordering.entities.UpdateActiveItem
import io.paytouch.ordering.utils.UtcTime

import scala.concurrent.Future

trait SlickToggleableItemDao extends SlickLocationDao {

  type Record <: SlickToggleableRecord
  type Table <: SlickTable[Record] with ActiveColumn with LocationIdColumn

  def bulkUpdateActiveField(locationIds: Seq[UUID], updates: Seq[UpdateActiveItem]): Future[Int] = {
    val q = updates
      .groupBy(_.active)
      .map { case (active, items) => querySetActive(locationIds, items.map(_.itemId), active) }
      .toSeq
    runWithTransaction(asSeq(q)).map(_.sum)
  }

  private def querySetActive(
      locationIds: Seq[UUID],
      ids: Seq[UUID],
      active: Boolean,
    ) = {
    val field = for { o <- table if o.id.inSet(ids) && o.locationId.inSet(locationIds) } yield (o.active, o.updatedAt)
    field.update(active, UtcTime.now)
  }

}
