package io.paytouch.core.data.queries

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.SlickRecord
import io.paytouch.core.data.tables.SlickTable
import slick.lifted.Query

class SlickQuery[R <: SlickRecord, T <: SlickTable[R]](q: Query[T, T#TableElementType, Seq]) {
  def filterById(id: UUID) = q.filter(_.id === id)
  def filterByIds(ids: Seq[UUID]) = q.filter(_.id inSet ids)
}
