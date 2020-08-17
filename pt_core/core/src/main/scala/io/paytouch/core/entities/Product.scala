package io.paytouch.core.entities

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.{ ArticleScope, ArticleType, ChangeReason, UnitType }
import io.paytouch.core.entities.enums.ExposedName

final case class Product(
    id: UUID,
    `type`: ArticleType,
    scope: ArticleScope,
    isCombo: Boolean,
    name: String,
    description: Option[String],
    brandId: Option[UUID],
    price: MonetaryAmount,
    priceRange: Option[MonetaryRange],
    cost: Option[MonetaryAmount],
    costRange: Option[MonetaryRange],
    averageCost: Option[MonetaryAmount],
    unit: UnitType,
    margin: Option[BigDecimal],
    upc: Option[String],
    sku: Option[String],
    isVariantOfProductId: Option[UUID],
    hasVariant: Boolean,
    trackInventory: Boolean,
    trackInventoryParts: Boolean,
    active: Boolean,
    applyPricingToAllLocations: Boolean,
    discountable: Boolean,
    avatarBgColor: Option[String],
    avatarImageUrls: Seq[ImageUrls],
    isService: Boolean,
    orderRoutingBar: Boolean,
    orderRoutingKitchen: Boolean,
    orderRoutingEnabled: Boolean,
    hasParts: Boolean,
    options: Seq[VariantOptionWithType],
    categories: Option[Seq[Category]],
    categoryIds: Option[Seq[UUID]],
    locationOverrides: Map[UUID, ProductLocation],
    variants: Option[Seq[VariantOptionType]],
    variantProducts: Option[Seq[Product]],
    modifiers: Option[Seq[ModifierSet]],
    modifierIds: Option[Seq[UUID]],
    modifierPositions: Option[Seq[ModifierPosition]],
    stockLevel: Option[BigDecimal],
    reorderAmount: Option[BigDecimal],
    suppliers: Option[Seq[SupplierInfo]],
    recipeDetails: Option[Seq[RecipeDetail]],
    categoryPositions: Option[Seq[CategoryPosition]],
    catalogCategories: Option[Seq[Category]],
    catalogCategoryOptions: Option[Seq[CatalogCategoryOption]],
    catalogCategoryPositions: Option[Seq[CategoryPosition]],
    bundleSets: Seq[BundleSet],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends ExposedEntity {
  val classShortName = ExposedName.Product
}

final case class CategoryPosition(categoryId: UUID, position: Int)

final case class ModifierPosition(modifierSetId: UUID, position: Option[Int])

final case class ProductCreation(
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
    variantProducts: Seq[VariantProductCreation],
    locationOverrides: Map[UUID, Option[ProductLocationUpdate]],
    imageUploadIds: Seq[UUID],
  )
object ProductCreation extends ToArticleCreation[ProductCreation] {
  def convert(t: ProductCreation)(implicit user: UserContext): ArticleCreation =
    ArticleCreation(
      name = t.name,
      description = t.description,
      categoryIds = t.categoryIds,
      brandId = t.brandId,
      supplierIds = t.supplierIds,
      sku = t.sku,
      upc = t.upc,
      cost = t.cost,
      price = t.price,
      unit = t.unit,
      margin = t.margin,
      trackInventory = t.trackInventory,
      trackInventoryParts = t.trackInventoryParts,
      active = t.active,
      applyPricingToAllLocations = t.applyPricingToAllLocations,
      discountable = t.discountable,
      avatarBgColor = t.avatarBgColor,
      isService = t.isService,
      orderRoutingBar = t.orderRoutingBar,
      orderRoutingKitchen = t.orderRoutingKitchen,
      orderRoutingEnabled = t.orderRoutingEnabled,
      variants = t.variants,
      variantProducts = t.variantProducts.map(VariantProductCreation.convert),
      locationOverrides = t.locationOverrides.transform((_, v) => v.map(ProductLocationUpdate.convert)),
      imageUploadIds = t.imageUploadIds,
      scope = ArticleScope.Product,
      `type` = None,
      isCombo = false,
      makesQuantity = None,
      bundleSets = Seq.empty,
    )
}

final case class ProductUpdate(
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
    variantProducts: Option[Seq[VariantProductUpdate]],
    locationOverrides: Map[UUID, Option[ProductLocationUpdate]],
    imageUploadIds: Option[Seq[UUID]],
    reason: ChangeReason = ChangeReason.Manual,
    notes: Option[String],
  )

object ProductUpdate extends ToArticleUpdate[ProductUpdate] {

  def convert(t: ProductUpdate)(implicit user: UserContext): ArticleUpdate =
    ArticleUpdate(
      name = t.name,
      description = t.description,
      categoryIds = t.categoryIds,
      brandId = t.brandId,
      supplierIds = t.supplierIds,
      sku = t.sku,
      upc = t.upc,
      cost = t.cost,
      price = t.price,
      unit = t.unit,
      margin = t.margin,
      trackInventory = t.trackInventory,
      trackInventoryParts = t.trackInventoryParts,
      active = t.active,
      applyPricingToAllLocations = t.applyPricingToAllLocations,
      discountable = t.discountable,
      avatarBgColor = t.avatarBgColor,
      isService = t.isService,
      orderRoutingBar = t.orderRoutingBar,
      orderRoutingKitchen = t.orderRoutingKitchen,
      orderRoutingEnabled = t.orderRoutingEnabled,
      variants = t.variants,
      variantProducts = t.variantProducts.map(_.map(VariantProductUpdate.convert)),
      locationOverrides = t.locationOverrides.transform((_, v) => v.map(ProductLocationUpdate.convert)),
      imageUploadIds = t.imageUploadIds,
      reason = t.reason,
      notes = t.notes,
      scope = Some(ArticleScope.Product),
      `type` = None,
      isCombo = None,
      makesQuantity = None,
      bundleSets = None,
    )
}
