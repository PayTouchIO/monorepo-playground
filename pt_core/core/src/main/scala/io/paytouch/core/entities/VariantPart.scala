package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.data.model.enums.UnitType

final case class VariantPartCreation(
    id: UUID,
    optionIds: Seq[UUID],
    sku: ResettableString,
    upc: ResettableString,
    cost: ResettableBigDecimal,
    unit: Option[UnitType],
    applyPricingToAllLocations: Option[Boolean],
    locationOverrides: Map[UUID, Option[PartLocationUpdate]],
  )

object VariantPartCreation extends ToVariantArticleCreation[VariantPartCreation] {

  def convert(t: VariantPartCreation)(implicit user: UserContext): VariantArticleCreation =
    VariantArticleCreation(
      id = t.id,
      optionIds = t.optionIds,
      sku = t.sku,
      upc = t.upc,
      cost = t.cost,
      price = None,
      unit = t.unit,
      margin = None,
      applyPricingToAllLocations = t.applyPricingToAllLocations,
      discountable = None,
      avatarBgColor = None,
      isService = None,
      orderRoutingBar = None,
      orderRoutingKitchen = None,
      orderRoutingEnabled = None,
      locationOverrides = t.locationOverrides.transform((_, v) => v.map(PartLocationUpdate.convert)),
    )
}

final case class VariantPartUpdate(
    id: UUID,
    optionIds: Seq[UUID],
    sku: ResettableString,
    upc: ResettableString,
    cost: ResettableBigDecimal,
    unit: Option[UnitType],
    applyPricingToAllLocations: Option[Boolean],
    locationOverrides: Map[UUID, Option[PartLocationUpdate]],
  )

object VariantPartUpdate extends ToVariantArticleUpdate[VariantPartUpdate] {

  def convert(t: VariantPartUpdate)(implicit user: UserContext): VariantArticleUpdate =
    VariantArticleUpdate(
      id = t.id,
      optionIds = t.optionIds,
      sku = t.sku,
      upc = t.upc,
      cost = t.cost,
      price = None,
      unit = t.unit,
      margin = None,
      applyPricingToAllLocations = t.applyPricingToAllLocations,
      discountable = None,
      avatarBgColor = None,
      isService = None,
      orderRoutingBar = None,
      orderRoutingKitchen = None,
      orderRoutingEnabled = None,
      locationOverrides = t.locationOverrides.transform((_, v) => v.map(PartLocationUpdate.convert)),
    )
}
