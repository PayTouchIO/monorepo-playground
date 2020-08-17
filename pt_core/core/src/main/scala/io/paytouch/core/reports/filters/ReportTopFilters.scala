package io.paytouch.core.reports.filters

import java.time.LocalDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.OrderType
import io.paytouch.core.entities.UserContext
import io.paytouch.core.reports.entities.enums.ReportInterval
import io.paytouch.core.reports.views.ReportTopView

final case class ReportTopFilters[V <: ReportTopView](
    view: V,
    from: LocalDateTime,
    to: LocalDateTime,
    locationIds: Seq[UUID],
    interval: ReportInterval,
    orderByFields: Seq[V#OrderBy],
    n: Int,
    merchantId: UUID,
  ) extends ReportFilters {
  require(1 <= n, "n (top items to return) must be at least 1")
  require(n <= 100, "n (top items to return) cannot be more than 100")

  val categoryIds: Option[Seq[UUID]] = None
  val ids: Option[Seq[UUID]] = None
  val orderTypes: Option[Seq[OrderType]] = None
}

object ReportTopFilters {

  def apply[V <: ReportTopView](
      view: V,
      from: LocalDateTime,
      to: LocalDateTime,
      locationId: Option[UUID],
      interval: ReportInterval,
      orderByFields: Seq[V#OrderBy],
      n: Int,
    )(implicit
      user: UserContext,
    ): ReportTopFilters[V] =
    ReportTopFilters[V](
      view,
      from,
      to,
      user.defaultedToUserLocations(locationId),
      interval,
      orderByFields,
      n,
      user.merchantId,
    )
}
