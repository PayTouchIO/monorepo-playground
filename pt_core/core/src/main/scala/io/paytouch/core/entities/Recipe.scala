package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.data.model.enums.{ ArticleScope, ArticleType, ChangeReason, UnitType }

final case class RecipeCreation(
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
    locationOverrides: Map[UUID, Option[RecipeLocationUpdate]],
    makesQuantity: Option[BigDecimal],
  )

object RecipeCreation extends ToArticleCreation[RecipeCreation] {

  type Variant = VariantPartCreation

  def convert(t: RecipeCreation)(implicit user: UserContext): ArticleCreation =
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
      variants = Seq.empty,
      variantProducts = Seq.empty,
      locationOverrides = t.locationOverrides.transform((_, v) => v.map(RecipeLocationUpdate.convert)),
      imageUploadIds = Seq.empty,
      scope = ArticleScope.Part,
      `type` = Some(ArticleType.Simple),
      isCombo = true,
      makesQuantity = t.makesQuantity,
      bundleSets = Seq.empty,
    )
}

final case class RecipeUpdate(
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
    locationOverrides: Map[UUID, Option[RecipeLocationUpdate]],
    makesQuantity: Option[BigDecimal],
    reason: ChangeReason = ChangeReason.Manual,
    notes: Option[String],
  )

object RecipeUpdate extends ToArticleUpdate[RecipeUpdate] {

  def convert(t: RecipeUpdate)(implicit user: UserContext): ArticleUpdate =
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
      variants = None,
      variantProducts = None,
      locationOverrides = t.locationOverrides.transform((_, v) => v.map(RecipeLocationUpdate.convert)),
      imageUploadIds = None,
      reason = t.reason,
      notes = t.notes,
      scope = Some(ArticleScope.Part),
      `type` = Some(ArticleType.Simple),
      isCombo = None,
      makesQuantity = t.makesQuantity,
      bundleSets = None,
    )
}
