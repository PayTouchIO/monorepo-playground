package io.paytouch.core.data.queries

import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.ItemIdColumn
import io.paytouch.core.data.model.SlickRecord
import io.paytouch.core.data.tables.SlickTable

class ItemIdQuery[R <: SlickRecord, T <: SlickTable[R] with ItemIdColumn](q: Query[T, T#TableElementType, Seq]) {

  def filterByItemId(itemId: UUID) = filterByItemIds(Seq(itemId))

  def filterByItemIds(itemIds: Seq[UUID]) = q.filter(_.itemId inSet itemIds).sortBy(_.createdAt)
}
