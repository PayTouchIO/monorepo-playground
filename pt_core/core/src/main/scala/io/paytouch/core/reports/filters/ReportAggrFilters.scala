package io.paytouch.core.reports.filters

import java.time.LocalDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.OrderType
import io.paytouch.core.entities.UserContext
import io.paytouch.core.reports.entities.enums.ReportInterval
import io.paytouch.core.reports.views.ReportAggrView
import io.paytouch.core.utils.EnumEntrySnake

final case class ReportAggrFilters[V <: ReportAggrView](
    view: V,
    from: LocalDateTime,
    to: LocalDateTime,
    locationIds: Seq[UUID],
    interval: ReportInterval,
    groupBy: Option[V#GroupBy],
    fields: Seq[V#Field],
    ids: Option[Seq[UUID]],
    orderTypes: Option[Seq[OrderType]],
    merchantId: UUID,
  ) extends ReportFilters {

  val categoryIds: Option[Seq[UUID]] = None

  override def toFieldHeader(f: EnumEntrySnake, name: Option[String] = None): String =
    if (fields.exists(f.isEquivalent)) name.getOrElse(toHumanReadable(f)) else ""

  override def toGroupByHeader(name: Option[String]): String =
    groupBy.map(g => name.getOrElse(toHumanReadable(g))).getOrElse("")

  override def toFieldOnDemand[T](f: EnumEntrySnake, value: T): Option[T] =
    if (fields.exists(f.isEquivalent)) Some(value) else None

  override def fieldsInCsv = (view.fieldsEnum.alwaysExpanded ++ fields).map(_.entryName)
}

object ReportAggrFilters {

  def apply[V <: ReportAggrView](
      view: V,
      from: LocalDateTime,
      to: LocalDateTime,
      locationId: Option[UUID],
      groupBy: Option[V#GroupBy] = None,
      fields: Seq[V#Field] = Seq.empty,
      ids: Option[Seq[UUID]],
      orderTypes: Option[Seq[OrderType]],
      interval: ReportInterval,
    )(implicit
      user: UserContext,
    ): ReportAggrFilters[V] =
    ReportAggrFilters[V](
      view,
      from,
      to,
      user.defaultedToUserLocations(locationId),
      interval,
      groupBy,
      fields,
      ids,
      orderTypes,
      user.merchantId,
    )
}
