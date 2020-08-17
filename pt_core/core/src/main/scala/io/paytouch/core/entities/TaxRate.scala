package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.entities.enums.ExposedName

final case class TaxRate(
    id: UUID,
    name: String,
    value: BigDecimal,
    applyToPrice: Boolean,
    locationOverrides: Option[Map[UUID, ItemLocation]],
  ) extends ExposedEntity {
  val classShortName = ExposedName.TaxRate
}

final case class TaxRateCreation(
    name: String,
    value: BigDecimal,
    applyToPrice: Option[Boolean],
    locationOverrides: Map[UUID, Option[TaxRateLocationUpdate]] = Map.empty,
  ) extends CreationEntity[TaxRate, TaxRateUpdate] {
  def asUpdate =
    TaxRateUpdate(
      name = Some(name),
      value = Some(value),
      applyToPrice = applyToPrice,
      locationOverrides = locationOverrides,
    )
}

final case class TaxRateUpdate(
    name: Option[String],
    value: Option[BigDecimal],
    applyToPrice: Option[Boolean],
    locationOverrides: Map[UUID, Option[TaxRateLocationUpdate]] = Map.empty,
  ) extends UpdateEntity[TaxRate]

final case class TaxRateLocationUpdate(active: Option[Boolean])
