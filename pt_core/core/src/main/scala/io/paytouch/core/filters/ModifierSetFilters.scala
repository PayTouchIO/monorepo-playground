package io.paytouch.core.filters

import java.time.ZonedDateTime
import java.util.UUID

final case class ModifierSetFilters(
    ids: Option[Seq[UUID]] = None,
    locationIds: Option[Seq[UUID]] = None,
    query: Option[String] = None,
    updatedSince: Option[ZonedDateTime] = None,
  ) extends BaseFilters
