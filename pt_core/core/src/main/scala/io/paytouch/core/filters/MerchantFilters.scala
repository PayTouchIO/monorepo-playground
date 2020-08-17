package io.paytouch.core.filters

import java.time.ZonedDateTime
import java.util.UUID
import io.paytouch.core.data.model.enums.BusinessType

final case class MerchantFilters(
    businessType: Option[BusinessType] = None,
    query: Option[String] = None,
    ids: Option[Seq[UUID]] = None,
  ) extends BaseFilters
