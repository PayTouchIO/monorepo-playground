package io.paytouch.core.data.daos.features

import java.util.UUID

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.ActiveColumn
import io.paytouch.core.data.model.SlickToggleableRecord
import io.paytouch.core.data.tables.SlickMerchantTable
import io.paytouch.core.entities.UpdateActiveItem
import io.paytouch.core.utils.UtcTime

trait SlickToggleableItemDao extends SlickMerchantDao {
  type Record <: SlickToggleableRecord
  type Table <: SlickMerchantTable[Record] with ActiveColumn

  def bulkUpdateActiveField(merchantId: UUID, updates: Seq[UpdateActiveItem]): Future[Unit] =
    (for {
      _ <- querySetActive(merchantId, updates.filter(_.active).map(_.itemId), true)
      _ <- querySetActive(merchantId, updates.filterNot(_.active).map(_.itemId), false)
    } yield ()).pipe(runWithTransaction)

  def querySetActive(
      merchantId: UUID,
      ids: Seq[UUID],
      active: Boolean,
    ) =
    table
      .filterByIds(ids)
      .filterByMerchantId(merchantId)
      .map(o => o.active -> o.updatedAt)
      .update(active, UtcTime.now)
}
