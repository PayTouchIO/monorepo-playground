package io.paytouch.core.entities

import java.util.{ Currency, UUID }

import io.paytouch.core.Availabilities
import io.paytouch.core.data.model.enums.DiscountType
import io.paytouch.core.entities.enums.ExposedName

final case class Discount(
    id: UUID,
    title: String,
    `type`: DiscountType,
    currency: Option[Currency],
    amount: BigDecimal,
    requireManagerApproval: Boolean,
    locationOverrides: Option[Map[UUID, ItemLocation]],
    availabilityHours: Option[Availabilities],
  ) extends ExposedEntity {
  val classShortName = ExposedName.Discount
}

final case class DiscountCreation(
    title: String,
    `type`: DiscountType,
    amount: BigDecimal,
    requireManagerApproval: Option[Boolean],
    locationOverrides: Map[UUID, Option[ItemLocationUpdate]] = Map.empty,
    availabilityHours: Option[Availabilities],
  ) extends CreationEntity[Discount, DiscountUpdate] {
  def asUpdate =
    DiscountUpdate(
      title = Some(title),
      `type` = Some(`type`),
      amount = Some(amount),
      requireManagerApproval = requireManagerApproval,
      locationOverrides = locationOverrides,
      availabilityHours = availabilityHours,
    )
}

final case class DiscountUpdate(
    title: Option[String],
    `type`: Option[DiscountType],
    amount: Option[BigDecimal],
    requireManagerApproval: Option[Boolean],
    locationOverrides: Map[UUID, Option[ItemLocationUpdate]] = Map.empty,
    availabilityHours: Option[Availabilities],
  ) extends UpdateEntity[Discount]
