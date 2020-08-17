package io.paytouch.core.filters

import java.time.ZonedDateTime

final case class GiftCardFilters(updatedSince: Option[ZonedDateTime] = None) extends BaseFilters
