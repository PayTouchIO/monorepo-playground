package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.data.model.enums.{ ArticleScope, ArticleType, ChangeReason, UnitType }

final case class ArticleCreation(
    name: String,
    description: ResettableString,
    categoryIds: Seq[UUID],
    brandId: ResettableUUID,
    supplierIds: Seq[UUID],
    sku: ResettableString,
    upc: ResettableString,
    cost: ResettableBigDecimal,
    price: BigDecimal,
    unit: UnitType,
    margin: ResettableBigDecimal,
    trackInventory: Option[Boolean],
    trackInventoryParts: Option[Boolean],
    active: Option[Boolean],
    applyPricingToAllLocations: Option[Boolean],
    discountable: Option[Boolean],
    avatarBgColor: ResettableString,
    isService: Option[Boolean],
    orderRoutingBar: Option[Boolean],
    orderRoutingKitchen: Option[Boolean],
    orderRoutingEnabled: Option[Boolean],
    variants: Seq[VariantOptionTypeUpsertion],
    variantProducts: Seq[VariantArticleCreation],
    locationOverrides: Map[UUID, Option[ArticleLocationUpdate]],
    imageUploadIds: Seq[UUID],
    scope: ArticleScope,
    `type`: Option[ArticleType],
    isCombo: Boolean,
    makesQuantity: Option[BigDecimal],
    bundleSets: Seq[BundleSetCreation],
  ) extends CreationEntity[Product, ArticleUpdate] {
  def asUpdate =
    ArticleUpdate(
      name = Some(name),
      description = description,
      categoryIds = Some(categoryIds),
      brandId = brandId,
      supplierIds = Some(supplierIds),
      sku = sku,
      upc = upc,
      cost = cost,
      price = Some(price),
      unit = Some(unit),
      margin = margin,
      trackInventory = trackInventory,
      trackInventoryParts = trackInventoryParts,
      active = active,
      applyPricingToAllLocations = applyPricingToAllLocations,
      discountable = discountable,
      avatarBgColor = avatarBgColor,
      isService = isService,
      orderRoutingBar = orderRoutingBar,
      orderRoutingKitchen = orderRoutingKitchen,
      orderRoutingEnabled = orderRoutingEnabled,
      variants = Some(variants),
      variantProducts = Some(variantProducts.map(_.asUpdate(this))),
      locationOverrides = locationOverrides,
      imageUploadIds = Some(imageUploadIds),
      reason = ChangeReason.Manual,
      notes = None,
      scope = Some(scope),
      `type` = `type`,
      isCombo = Some(isCombo),
      makesQuantity = makesQuantity,
      bundleSets = Some(bundleSets.map(_.asUpdate)),
    )
}

trait ToArticleCreation[T] {

  def convert(t: T)(implicit user: UserContext): ArticleCreation

}

final case class ArticleUpdate(
    name: Option[String],
    description: ResettableString,
    categoryIds: Option[Seq[UUID]],
    brandId: ResettableUUID,
    supplierIds: Option[Seq[UUID]],
    sku: ResettableString,
    upc: ResettableString,
    cost: ResettableBigDecimal,
    price: Option[BigDecimal],
    unit: Option[UnitType],
    margin: ResettableBigDecimal,
    trackInventory: Option[Boolean],
    trackInventoryParts: Option[Boolean],
    active: Option[Boolean],
    applyPricingToAllLocations: Option[Boolean],
    discountable: Option[Boolean],
    avatarBgColor: ResettableString,
    isService: Option[Boolean],
    orderRoutingBar: Option[Boolean],
    orderRoutingKitchen: Option[Boolean],
    orderRoutingEnabled: Option[Boolean],
    variants: Option[Seq[VariantOptionTypeUpsertion]],
    variantProducts: Option[Seq[VariantArticleUpdate]],
    locationOverrides: Map[UUID, Option[ArticleLocationUpdate]],
    imageUploadIds: Option[Seq[UUID]],
    reason: ChangeReason = ChangeReason.Manual,
    notes: Option[String],
    scope: Option[ArticleScope],
    `type`: Option[ArticleType],
    isCombo: Option[Boolean],
    makesQuantity: Option[BigDecimal],
    bundleSets: Option[Seq[BundleSetUpdate]],
  ) extends UpdateEntity[Product]

trait ToArticleUpdate[T] {

  def convert(t: T)(implicit user: UserContext): ArticleUpdate

}

final case class ArticleInfo(
    id: UUID,
    name: String,
    sku: Option[String],
    upc: Option[String],
    options: Option[Seq[VariantOptionWithType]],
  )
