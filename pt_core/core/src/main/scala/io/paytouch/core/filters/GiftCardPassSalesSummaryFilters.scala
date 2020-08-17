package io.paytouch.core.filters

import java.time.LocalDateTime
import java.util.UUID

import io.paytouch.core.entities.UserContext

final case class GiftCardPassSalesSummaryFilters(
    locationIds: Seq[UUID],
    from: Option[LocalDateTime],
    to: Option[LocalDateTime],
  ) extends BaseFilters

object GiftCardPassSalesSummaryFilters {

  def apply(
      locationId: Option[UUID],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
    )(implicit
      user: UserContext,
    ): GiftCardPassSalesSummaryFilters = {
    val locationIds = user.accessibleLocations(locationId)
    new GiftCardPassSalesSummaryFilters(locationIds, from, to)
  }
}
