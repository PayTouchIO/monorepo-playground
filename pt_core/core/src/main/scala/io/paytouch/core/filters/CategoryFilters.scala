package io.paytouch.core.filters

import java.time.ZonedDateTime
import java.util.UUID

final case class CategoryFilters(
    locationId: Option[UUID] = None,
    query: Option[String] = None,
    updatedSince: Option[ZonedDateTime] = None,
    catalogId: Option[UUID] = None,
  ) extends BaseFilters

object CategoryFilters {

  def forCatalogCategories(
      catalogId: UUID,
      locationId: Option[UUID] = None,
      query: Option[String] = None,
      updatedSince: Option[ZonedDateTime] = None,
    ): CategoryFilters =
    apply(locationId, query, updatedSince, Some(catalogId))

  def forSystemCategories(
      locationId: Option[UUID] = None,
      query: Option[String] = None,
      updatedSince: Option[ZonedDateTime] = None,
    ): CategoryFilters =
    apply(locationId, query, updatedSince)
}
