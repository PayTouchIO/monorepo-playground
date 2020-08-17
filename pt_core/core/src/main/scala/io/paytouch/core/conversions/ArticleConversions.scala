package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.LocationOverridesPer
import io.paytouch.core.data.model.enums.{ ArticleType, KitchenType }
import io.paytouch.core.data.model.{ ArticleUpdate => ArticleUpdateModel, _ }
import io.paytouch.core.entities.{ ArticleUpdate => ArticleUpdateEntity, Product => ProductEntity, _ }

trait ArticleConversions
    extends EntityConversion[ArticleRecord, ProductEntity]
       with ModelConversion[ArticleUpdateEntity, ArticleUpdateModel] {

  def fromRecordsAndOptionsToEntities(
      products: Seq[ArticleRecord],
      locationOverridesPerProduct: LocationOverridesPer[ArticleRecord, ProductLocation] = Map.empty,
      systemCategoriesPerProduct: Option[Map[ArticleRecord, Seq[Category]]] = None,
      systemCategoryIdsPerProduct: Option[Map[ArticleRecord, Seq[UUID]]] = None,
      variantProductsPerProduct: Option[Map[ArticleRecord, Seq[ProductEntity]]] = None,
      variantsPerProduct: Option[Map[ArticleRecord, Seq[VariantOptionType]]] = None,
      modifiersPerProduct: Option[Map[ArticleRecord, Seq[ModifierSet]]] = None,
      modifierIdsPerProduct: Option[Map[ArticleRecord, Seq[UUID]]] = None,
      modifierPositionsPerProduct: Option[Map[ArticleRecord, Seq[ModifierPosition]]] = None,
      imageUrlsPerProduct: Map[ArticleRecord, Seq[ImageUrls]] = Map.empty,
      stockLevelPerProduct: Option[Map[ArticleRecord, BigDecimal]] = None,
      reorderAmountPerProduct: Option[Map[ArticleRecord, BigDecimal]] = None,
      suppliersPerProduct: Option[Map[ArticleRecord, Seq[SupplierInfo]]] = None,
      recipeDetailsPerProduct: Option[Map[ArticleRecord, Seq[RecipeDetail]]] = None,
      priceRangePerProduct: Option[Map[ArticleRecord, MonetaryRange]] = None,
      costRangePerProduct: Option[Map[ArticleRecord, MonetaryRange]] = None,
      variantOptionsPerProduct: Map[ArticleRecord, Seq[VariantOptionWithType]] = Map.empty,
      systemCategoryPositionsPerProduct: Option[Map[ArticleRecord, Seq[CategoryPosition]]] = None,
      catalogCategoriesPerProduct: Option[Map[ArticleRecord, Seq[Category]]] = None,
      catalogCategoryOptionsPerProduct: Option[Map[ArticleRecord, Seq[CatalogCategoryOption]]] = None,
      catalogCategoryPositionsPerProduct: Option[Map[ArticleRecord, Seq[CategoryPosition]]] = None,
      bundleSetsPerProduct: Map[ArticleRecord, Seq[BundleSet]] = Map.empty,
      kitchens: Seq[Kitchen] = Seq.empty,
    )(implicit
      user: UserContext,
    ) =
    products.map { product =>
      val locationOverrides = locationOverridesPerProduct.getOrElse(product, Map.empty)
      val variantOptions = variantOptionsPerProduct.getOrElse(product, Seq.empty)
      val systemCategories = systemCategoriesPerProduct.map(_.getOrElse(product, Seq.empty))
      val systemCategoryIds = systemCategoryIdsPerProduct.map(_.getOrElse(product, Seq.empty))
      val variantProducts = variantProductsPerProduct.map(_.getOrElse(product, Seq.empty))
      val variants = variantsPerProduct.map(_.getOrElse(product, Seq.empty))
      val modifiers = modifiersPerProduct.map(_.getOrElse(product, Seq.empty))
      val modifierIds = modifierIdsPerProduct.map(_.getOrElse(product, Seq.empty))
      val modifierPositions = modifierPositionsPerProduct.map(_.getOrElse(product, Seq.empty))
      val imageUrls = imageUrlsPerProduct.getOrElse(product, Seq.empty)
      val suppliers = suppliersPerProduct.map(_.getOrElse(product, Seq.empty))
      val recipeDetails = recipeDetailsPerProduct.map(_.getOrElse(product, Seq.empty))
      val priceRange = priceRangePerProduct.flatMap(_.get(product))
      val costRange = costRangePerProduct.flatMap(_.get(product))
      val stockLevel = stockLevelPerProduct.map(_.getOrElse(product, BigDecimal(0)))
      val reorderAmount = reorderAmountPerProduct.map(_.getOrElse(product, BigDecimal(0)))
      val systemCategoryPositions = systemCategoryPositionsPerProduct.map(_.getOrElse(product, Seq.empty))
      val catalogCategories = catalogCategoriesPerProduct.map(_.getOrElse(product, Seq.empty))
      val catalogCategoryOptions = catalogCategoryOptionsPerProduct.map(_.getOrElse(product, Seq.empty))
      val catalogCategoryPositions = catalogCategoryPositionsPerProduct.map(_.getOrElse(product, Seq.empty))
      val bundleSets = bundleSetsPerProduct.getOrElse(product, Seq.empty)
      fromRecordAndOptionsToEntity(
        product,
        locationOverrides,
        variantOptions,
        systemCategories,
        systemCategoryIds,
        variantProducts,
        variants,
        modifiers,
        modifierIds,
        modifierPositions,
        imageUrls,
        suppliers,
        recipeDetails,
        priceRange,
        costRange,
        stockLevel,
        reorderAmount,
        systemCategoryPositions,
        catalogCategories,
        catalogCategoryOptions,
        catalogCategoryPositions,
        bundleSets,
        kitchens,
      )
    }

  def fromRecordToEntity(product: ArticleRecord)(implicit user: UserContext) =
    fromRecordAndOptionsToEntity(product)

  private def fromRecordAndOptionsToEntity(
      product: ArticleRecord,
      locationOverrides: Map[UUID, ProductLocation] = Map.empty,
      variantOptions: Seq[VariantOptionWithType] = Seq.empty,
      systemCategories: Option[Seq[Category]] = None,
      systemCategoryIds: Option[Seq[UUID]] = None,
      variantProducts: Option[Seq[ProductEntity]] = None,
      variants: Option[Seq[VariantOptionType]] = None,
      modifiers: Option[Seq[ModifierSet]] = None,
      modifierIds: Option[Seq[UUID]] = None,
      modifierPositions: Option[Seq[ModifierPosition]] = None,
      imageUrls: Seq[ImageUrls] = Seq.empty,
      suppliers: Option[Seq[SupplierInfo]] = None,
      recipeDetails: Option[Seq[RecipeDetail]] = None,
      priceRange: Option[MonetaryRange] = None,
      costRange: Option[MonetaryRange] = None,
      stockLevel: Option[BigDecimal] = None,
      reorderAmount: Option[BigDecimal] = None,
      systemCategoryPositions: Option[Seq[CategoryPosition]] = None,
      catalogCategories: Option[Seq[Category]] = None,
      catalogCategoryOptions: Option[Seq[CatalogCategoryOption]] = None,
      catalogCategoryPositions: Option[Seq[CategoryPosition]] = None,
      bundleSets: Seq[BundleSet] = Seq.empty,
      kitchens: Seq[Kitchen] = Seq.empty,
    )(implicit
      user: UserContext,
    ) = {
    // backward compatible layer until orderRouting* fields are removed.
    // we decide to mark a product as routable to kitchen type if at least one of its location overrides point to a kitchen of that type
    def hasRoutesToKitchensWithType(targetType: KitchenType) = {
      val kitchenIdsByType = kitchens.filter(_.`type` == targetType).map(_.id).toSet
      val routeToKitchenIds = locationOverrides.flatMap(_._2.routeToKitchenId).toSet
      val intersection = routeToKitchenIds intersect kitchenIdsByType
      routeToKitchenIds.nonEmpty && intersection.nonEmpty
    }
    val orderRoutingBar = hasRoutesToKitchensWithType(KitchenType.Bar)
    val orderRoutingKitchen = hasRoutesToKitchensWithType(KitchenType.Kitchen)
    ProductEntity(
      id = product.id,
      name = product.name,
      `type` = product.`type`,
      scope = product.scope,
      isCombo = product.isCombo,
      description = product.description,
      brandId = product.brandId,
      price = MonetaryAmount(product.priceAmount),
      priceRange = priceRange,
      cost = MonetaryAmount.extract(product.costAmount),
      costRange = costRange,
      averageCost = MonetaryAmount.extract(product.averageCostAmount),
      unit = product.unit,
      margin = product.margin,
      upc = product.upc,
      sku = product.sku,
      isVariantOfProductId = product.isVariantOfProductId,
      hasVariant = product.hasVariant,
      trackInventory = product.trackInventory,
      trackInventoryParts = product.trackInventoryParts,
      active = product.active,
      applyPricingToAllLocations = product.applyPricingToAllLocations,
      discountable = product.discountable,
      avatarBgColor = product.avatarBgColor,
      avatarImageUrls = imageUrls,
      isService = product.isService,
      orderRoutingBar = orderRoutingBar,
      orderRoutingKitchen = orderRoutingKitchen,
      orderRoutingEnabled = product.orderRoutingEnabled,
      options = variantOptions,
      categories = systemCategories,
      categoryIds = systemCategoryIds,
      locationOverrides = locationOverrides,
      variants = variants,
      variantProducts = variantProducts,
      hasParts = product.hasParts,
      modifiers = modifiers,
      modifierIds = modifierIds,
      modifierPositions = modifierPositions,
      stockLevel = stockLevel,
      reorderAmount = reorderAmount,
      suppliers = suppliers,
      recipeDetails = recipeDetails,
      categoryPositions = systemCategoryPositions,
      catalogCategories = catalogCategories,
      catalogCategoryOptions = catalogCategoryOptions,
      catalogCategoryPositions = catalogCategoryPositions,
      bundleSets = bundleSets,
      createdAt = product.createdAt,
      updatedAt = product.updatedAt,
    )
  }

  def fromUpsertionToUpdate(id: UUID, update: ArticleUpdateEntity)(implicit user: UserContext): ArticleUpdateModel = {
    val `type` = {
      val typeOnVariants =
        update.variantProducts.map(vps => if (vps.isEmpty) ArticleType.Simple else ArticleType.Template)
      update.`type`.orElse(typeOnVariants)
    }
    val isVariantOfProductId = if (`type`.exists(_.isSimple)) Some(id) else None
    val hasVariant = update.variantProducts.map(_.nonEmpty)
    ArticleUpdateModel(
      id = Some(id),
      `type` = `type`,
      isCombo = update.isCombo,
      merchantId = Some(user.merchantId),
      name = update.name,
      description = update.description,
      brandId = update.brandId,
      priceAmount = update.price,
      costAmount = update.cost,
      averageCostAmount = None,
      unit = update.unit,
      margin = update.margin,
      upc = if (`type`.exists(_.isTemplate)) None else update.upc,
      sku = if (`type`.exists(_.isTemplate)) None else update.sku,
      isVariantOfProductId = isVariantOfProductId,
      hasVariant = hasVariant,
      trackInventory = update.trackInventory,
      active = update.active,
      applyPricingToAllLocations = update.applyPricingToAllLocations,
      discountable = update.discountable,
      avatarBgColor = update.avatarBgColor,
      isService = update.isService,
      orderRoutingBar = update.orderRoutingBar,
      orderRoutingKitchen = update.orderRoutingKitchen,
      orderRoutingEnabled = update.orderRoutingEnabled,
      trackInventoryParts = update.trackInventoryParts,
      hasParts = None,
      scope = update.scope,
      deletedAt = None,
    )
  }

  def toCategoryPositions(productCategories: Seq[ProductCategoryRecord]): Seq[CategoryPosition] =
    productCategories.map(pc => CategoryPosition(pc.categoryId, pc.position))

  def toModifierPositions(modifierSetProducts: Seq[ModifierSetProductRecord]): Seq[ModifierPosition] =
    modifierSetProducts.map(mp => ModifierPosition(mp.modifierSetId, mp.position))

  private type DataSeqPerRecord[T] = Option[Map[ArticleRecord, Seq[T]]]

  protected def extractSystemCategoriesPerProduct(
      categoriesPerProduct: DataSeqPerRecord[Category],
    )(
      withSystemCategories: Boolean,
    ): DataSeqPerRecord[Category] =
    if (withSystemCategories) categoriesPerProduct
    else None

  protected def extractSystemCategoryIdsPerProduct(
      categoriesPerProduct: DataSeqPerRecord[Category],
    )(
      withSystemCategoryIds: Boolean,
    ): DataSeqPerRecord[UUID] =
    if (withSystemCategoryIds)
      categoriesPerProduct.map(_.transform((_, v) => v.map(_.id)))
    else
      None

  def fromRecordsAndOptionsToInfoEntities(
      articles: Seq[ArticleRecord],
      optionsPerRecord: DataSeqPerRecord[VariantOptionWithType],
    ) =
    articles.map { article =>
      val options = optionsPerRecord.flatMap(_.get(article))
      fromRecordAndOptionsToInfoEntity(article, options)
    }

  def fromRecordAndOptionsToInfoEntity(
      article: ArticleRecord,
      options: Option[Seq[VariantOptionWithType]],
    ): ArticleInfo =
    ArticleInfo(
      id = article.id,
      name = article.name,
      sku = article.sku,
      upc = article.upc,
      options = options,
    )
}
