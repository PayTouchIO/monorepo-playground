package io.paytouch.core.data.queries

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.SlickMerchantRecord
import io.paytouch.core.data.tables.SlickMerchantTable
import slick.lifted.Query

class SlickMerchantQuery[R <: SlickMerchantRecord, T <: SlickMerchantTable[R]](q: Query[T, T#TableElementType, Seq]) {
  def filterByMerchantId(merchantId: UUID) = q.filter(_.merchantId === merchantId)

  def filterByUpdatedSince(date: ZonedDateTime) = q.filter(_.updatedAt >= date)

  def filterByOptUpdatedSince(maybeDate: Option[ZonedDateTime]) =
    maybeDate.fold(q)(date => q.filter(_.updatedAt >= date))
}
