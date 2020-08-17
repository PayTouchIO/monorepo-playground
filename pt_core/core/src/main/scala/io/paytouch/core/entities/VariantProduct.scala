package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.data.model.enums.UnitType

final case class VariantProductCreation(
    id: UUID,
    optionIds: Seq[UUID],
    sku: ResettableString,
    upc: ResettableString,
    cost: ResettableBigDecimal,
    price: Option[BigDecimal],
    unit: Option[UnitType],
    margin: ResettableBigDecimal,
    applyPricingToAllLocations: Option[Boolean],
    discountable: Option[Boolean],
    avatarBgColor: ResettableString,
    isService: Option[Boolean],
    orderRoutingBar: Option[Boolean],
    orderRoutingKitchen: Option[Boolean],
    orderRoutingEnabled: Option[Boolean],
    locationOverrides: Map[UUID, Option[ProductLocationUpdate]],
  )

object VariantProductCreation extends ToVariantArticleCreation[VariantProductCreation] {

  def convert(t: VariantProductCreation)(implicit user: UserContext): VariantArticleCreation =
    VariantArticleCreation(
      id = t.id,
      optionIds = t.optionIds,
      sku = t.sku,
      upc = t.upc,
      cost = t.cost,
      price = t.price,
      unit = t.unit,
      margin = t.margin,
      applyPricingToAllLocations = t.applyPricingToAllLocations,
      discountable = t.discountable,
      avatarBgColor = t.avatarBgColor,
      isService = t.isService,
      orderRoutingBar = t.orderRoutingBar,
      orderRoutingKitchen = t.orderRoutingKitchen,
      orderRoutingEnabled = t.orderRoutingEnabled,
      locationOverrides = t.locationOverrides.transform((_, v) => v.map(ProductLocationUpdate.convert)),
    )
}

final case class VariantProductUpdate(
    id: UUID,
    optionIds: Seq[UUID],
    sku: ResettableString,
    upc: ResettableString,
    cost: ResettableBigDecimal,
    price: Option[BigDecimal],
    unit: Option[UnitType],
    margin: ResettableBigDecimal,
    applyPricingToAllLocations: Option[Boolean],
    discountable: Option[Boolean],
    avatarBgColor: ResettableString,
    isService: Option[Boolean],
    orderRoutingBar: Option[Boolean],
    orderRoutingKitchen: Option[Boolean],
    orderRoutingEnabled: Option[Boolean],
    locationOverrides: Map[UUID, Option[ProductLocationUpdate]],
  )

object VariantProductUpdate extends ToVariantArticleUpdate[VariantProductUpdate] {

  def convert(t: VariantProductUpdate)(implicit user: UserContext): VariantArticleUpdate =
    VariantArticleUpdate(
      id = t.id,
      optionIds = t.optionIds,
      sku = t.sku,
      upc = t.upc,
      cost = t.cost,
      price = t.price,
      unit = t.unit,
      margin = t.margin,
      applyPricingToAllLocations = t.applyPricingToAllLocations,
      discountable = t.discountable,
      avatarBgColor = t.avatarBgColor,
      isService = t.isService,
      orderRoutingBar = t.orderRoutingBar,
      orderRoutingKitchen = t.orderRoutingKitchen,
      orderRoutingEnabled = t.orderRoutingEnabled,
      locationOverrides = t.locationOverrides.transform((_, v) => v.map(ProductLocationUpdate.convert)),
    )
}
