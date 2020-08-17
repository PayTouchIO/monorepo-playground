package io.paytouch.core.filters

import java.time.LocalDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.PaymentStatus
import io.paytouch.core.data.model.enums.PaymentStatus._
import io.paytouch.core.entities.UserContext

final case class ReportSalesSummaryFilters(
    paymentStatuses: Seq[PaymentStatus],
    locationIds: Seq[UUID],
    isInvoice: Boolean,
    from: Option[LocalDateTime],
    to: Option[LocalDateTime],
  ) extends BaseFilters

object ReportSalesSummaryFilters {

  def apply(
      locationId: Option[UUID],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
    )(implicit
      user: UserContext,
    ): ReportSalesSummaryFilters = {
    val paymentStatuses = Seq(Paid, PartiallyPaid, PartiallyRefunded)
    val locationIds = user.accessibleLocations(locationId)
    val isInvoice = false
    apply(paymentStatuses, locationIds, isInvoice, from, to)
  }
}
