package io.paytouch.core.filters

import java.util.UUID

final case class CatalogFilters(
    ids: Option[Seq[UUID]] = None,
  ) extends BaseFilters
