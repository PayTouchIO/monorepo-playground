package io.paytouch.core.reports.filters

import java.time.LocalDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.OrderType
import io.paytouch.core.entities.{ Pagination, UserContext }
import io.paytouch.core.reports.entities.enums.ReportInterval
import io.paytouch.core.reports.views.ReportListView
import io.paytouch.core.utils.EnumEntrySnake

final case class ReportListFilters[V <: ReportListView](
    view: V,
    from: LocalDateTime,
    to: LocalDateTime,
    locationIds: Seq[UUID],
    fields: Seq[V#OrderBy],
    orderByFields: Seq[V#OrderBy],
    pagination: Pagination,
    ids: Option[Seq[UUID]],
    categoryIds: Option[Seq[UUID]],
    orderTypes: Option[Seq[OrderType]],
    merchantId: UUID,
  ) extends ReportFilters {

  override def toFieldHeader(f: EnumEntrySnake, name: Option[String] = None): String =
    if (fields.exists(f.isEquivalent)) name.getOrElse(toHumanReadable(f)) else ""

  override def toFieldOnDemand[T](f: EnumEntrySnake, value: T): Option[T] =
    if (fields.exists(f.isEquivalent)) Some(value) else None

  override def fieldsInCsv = (view.orderByEnum.alwaysExpanded ++ fields).map(_.entryName)

  val interval = ReportInterval.detectInterval(from, to, withInterval = false)

}

object ReportListFilters {

  def apply[V <: ReportListView](
      view: V,
      from: LocalDateTime,
      to: LocalDateTime,
      locationId: Option[UUID],
      fields: Seq[V#OrderBy],
      orderByFields: Option[Seq[V#OrderBy]],
      ids: Option[Seq[UUID]],
      categoryIds: Option[Seq[UUID]],
      orderTypes: Option[Seq[OrderType]],
    )(implicit
      user: UserContext,
      pagination: Pagination,
    ): ReportListFilters[V] = {
    val ordering = orderByFields.getOrElse(view.orderByEnum.defaultOrdering) ++ view.orderByEnum.defaultOrdering
    ReportListFilters[V](
      view,
      from,
      to,
      user.defaultedToUserLocations(locationId),
      fields,
      ordering,
      pagination,
      ids,
      categoryIds,
      orderTypes,
      user.merchantId,
    )
  }
}
