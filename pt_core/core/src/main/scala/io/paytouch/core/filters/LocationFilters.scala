package io.paytouch.core.filters

import java.util.UUID

final case class LocationFilters(
    locationIds: Seq[UUID] = Seq.empty,
    query: Option[String] = None,
    showAll: Option[Boolean] = None,
  ) extends BaseFilters
