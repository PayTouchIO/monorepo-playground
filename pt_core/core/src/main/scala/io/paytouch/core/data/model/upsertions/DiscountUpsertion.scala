package io.paytouch.core.data.model.upsertions

import java.util.UUID

import io.paytouch.core.data.model._

case class DiscountUpsertion(
    discount: DiscountUpdate,
    discountLocations: Map[UUID, Option[DiscountLocationUpdate]],
    availabilities: Option[Seq[AvailabilityUpdate]],
  ) extends UpsertionModel[DiscountRecord]
