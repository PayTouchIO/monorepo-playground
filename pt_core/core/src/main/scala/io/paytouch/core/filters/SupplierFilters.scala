package io.paytouch.core.filters

import java.util.UUID

import cats.implicits._

final case class SupplierFilters(
    locationIds: Option[Seq[UUID]] = None,
    categoryIds: Option[Seq[UUID]] = None,
    query: Option[String] = None,
  ) extends BaseFilters

object SupplierFilters {
  def forList(
      locationIds: Option[Seq[UUID]] = None,
      categoryId: Option[UUID] = None,
      categoryIds: Option[Seq[UUID]] = None,
      query: Option[String] = None,
    ): SupplierFilters = {
    val allCategoryIds: Option[Seq[UUID]] =
      categoryId.map(List(_)) |+| categoryIds.map(_.toList)

    apply(locationIds, allCategoryIds, query)
  }
}
