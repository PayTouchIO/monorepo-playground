package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.data.model.enums.{ ArticleScope, ChangeReason, UnitType }

final case class PartCreation(
    name: String,
    description: ResettableString,
    categoryIds: Seq[UUID],
    brandId: ResettableUUID,
    supplierIds: Seq[UUID],
    sku: ResettableString,
    upc: ResettableString,
    cost: ResettableBigDecimal,
    unit: UnitType,
    trackInventory: Option[Boolean],
    trackInventoryParts: Option[Boolean],
    active: Option[Boolean],
    applyPricingToAllLocations: Option[Boolean],
    variants: Seq[VariantOptionTypeUpsertion],
    variantProducts: Seq[VariantPartCreation],
    locationOverrides: Map[UUID, Option[PartLocationUpdate]],
  )

object PartCreation extends ToArticleCreation[PartCreation] {

  def convert(t: PartCreation)(implicit user: UserContext): ArticleCreation =
    ArticleCreation(
      name = t.name,
      description = t.description,
      categoryIds = t.categoryIds,
      brandId = t.brandId,
      supplierIds = t.supplierIds,
      sku = t.sku,
      upc = t.upc,
      cost = t.cost,
      price = 0,
      unit = t.unit,
      margin = None,
      trackInventory = t.trackInventory,
      trackInventoryParts = t.trackInventoryParts,
      active = t.active,
      applyPricingToAllLocations = t.applyPricingToAllLocations,
      discountable = None,
      avatarBgColor = None,
      isService = None,
      orderRoutingBar = None,
      orderRoutingKitchen = None,
      orderRoutingEnabled = None,
      variants = t.variants,
      variantProducts = t.variantProducts.map(VariantPartCreation.convert),
      locationOverrides = t.locationOverrides.transform((_, v) => v.map(PartLocationUpdate.convert)),
      imageUploadIds = Seq.empty,
      scope = ArticleScope.Part,
      `type` = None,
      isCombo = false,
      makesQuantity = None,
      bundleSets = Seq.empty,
    )
}

final case class PartUpdate(
    name: Option[String],
    description: ResettableString,
    categoryIds: Option[Seq[UUID]],
    brandId: ResettableUUID,
    supplierIds: Option[Seq[UUID]],
    sku: ResettableString,
    upc: ResettableString,
    cost: ResettableBigDecimal,
    unit: Option[UnitType],
    trackInventory: Option[Boolean],
    trackInventoryParts: Option[Boolean],
    active: Option[Boolean],
    applyPricingToAllLocations: Option[Boolean],
    variants: Option[Seq[VariantOptionTypeUpsertion]],
    variantProducts: Option[Seq[VariantPartUpdate]],
    locationOverrides: Map[UUID, Option[PartLocationUpdate]],
    reason: ChangeReason = ChangeReason.Manual,
    notes: Option[String],
  )

object PartUpdate extends ToArticleUpdate[PartUpdate] {

  def convert(t: PartUpdate)(implicit user: UserContext): ArticleUpdate =
    ArticleUpdate(
      name = t.name,
      description = t.description,
      categoryIds = t.categoryIds,
      brandId = t.brandId,
      supplierIds = t.supplierIds,
      sku = t.sku,
      upc = t.upc,
      cost = t.cost,
      price = None,
      unit = t.unit,
      margin = None,
      trackInventory = t.trackInventory,
      trackInventoryParts = t.trackInventoryParts,
      active = t.active,
      applyPricingToAllLocations = t.applyPricingToAllLocations,
      discountable = None,
      avatarBgColor = None,
      isService = None,
      orderRoutingBar = None,
      orderRoutingKitchen = None,
      orderRoutingEnabled = None,
      variants = t.variants,
      variantProducts = t.variantProducts.map(_.map(VariantPartUpdate.convert)),
      locationOverrides = t.locationOverrides.transform((_, v) => v.map(PartLocationUpdate.convert)),
      imageUploadIds = None,
      reason = t.reason,
      notes = t.notes,
      scope = Some(ArticleScope.Part),
      `type` = None,
      isCombo = None,
      makesQuantity = None,
      bundleSets = None,
    )
}
