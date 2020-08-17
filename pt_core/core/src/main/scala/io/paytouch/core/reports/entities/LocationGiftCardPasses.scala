package io.paytouch.core.reports.entities

import java.util.UUID

final case class LocationGiftCardPasses(
    id: UUID,
    name: String,
    addressLine1: Option[String],
    data: GiftCardPassAggregate,
  )
