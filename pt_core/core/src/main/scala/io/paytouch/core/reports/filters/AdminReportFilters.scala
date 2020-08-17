package io.paytouch.core.reports.filters

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.messages.entities.OrderSynced

final case class AdminReportFilters(
    ids: Option[Seq[UUID]],
    merchantIds: Option[Seq[UUID]] = None,
    locationIds: Option[Seq[UUID]] = None,
    from: Option[ZonedDateTime] = None,
    to: Option[ZonedDateTime] = None,
  )

object AdminReportFilters {
  def fromMsg(msg: OrderSynced): AdminReportFilters = {
    val orderId = msg.payload.data
    AdminReportFilters(ids = Some(Seq(orderId)))
  }

  def apply(merchantId: UUID): AdminReportFilters =
    apply(ids = None, merchantIds = Some(Seq(merchantId)), locationIds = None, from = None, to = None)
}
