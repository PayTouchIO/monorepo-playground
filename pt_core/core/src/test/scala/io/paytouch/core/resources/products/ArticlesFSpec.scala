package io.paytouch.core.resources.products

import java.util.UUID

import io.paytouch.core.SequenceOfOptionIds
import io.paytouch.core.data.model.enums.{ ArticleScope, ArticleType }
import io.paytouch.core.data.model.{ ProductLocationUpdate => _, _ }
import io.paytouch.core.entities.{
  ArticleUpdate => ArticleUpdateEntity,
  Product => ProductEntity,
  BundleSetUpdate => BundleSetUpdateEntity,
  _,
}
import io.paytouch.core.utils._

abstract class ArticlesFSpec extends FSpec {

  abstract class ArticleResourceFSpecContext extends FSpecContext with MultipleLocationFixtures {
    lazy val articleDao = daos.articleDao
    lazy val imageUploadDao = daos.imageUploadDao
    lazy val modifierSetProductDao = daos.modifierSetProductDao
    lazy val productCategoryDao = daos.productCategoryDao
    lazy val productCategoryOptionDao = daos.productCategoryOptionDao
    lazy val productLocationDao = articleDao.productLocationDao
    lazy val productLocationTaxRateDao = articleDao.productLocationTaxRateDao
    lazy val productVariantOptionDao = daos.productVariantOptionDao
    lazy val recipeDetailDao = daos.recipeDetailDao
    lazy val supplierProductDao = daos.supplierProductDao
    lazy val variantOptionDao = daos.variantOptionDao
    lazy val variantOptionTypeDao = daos.variantOptionTypeDao

    def buildVariantTypeWithOptions(name: String, optionNames: Seq[String]) = {
      val options =
        optionNames.map(optName => VariantOptionUpsertion(id = UUID.randomUUID, name = optName, position = Some(0)))
      VariantOptionTypeUpsertion(id = UUID.randomUUID, name = name, position = Some(0), options = options)
    }

    val variantType1 = buildVariantTypeWithOptions("color", Seq("yellow", "blue"))
    val variantType2 = buildVariantTypeWithOptions("size", Seq("small", "medium", "large"))

    val variants = Seq(variantType1, variantType2)

    val variantSelections = variants.map(_.options.map(_.id)).combine

    def assertResponse(
        entity: ProductEntity,
        record: ArticleRecord,
        systemCategories: Option[Seq[CategoryRecord]] = None,
        systemCategoryIds: Option[Seq[UUID]] = None,
        locationIds: Seq[UUID] = Seq.empty,
        variantIds: Seq[UUID] = Seq.empty,
        modifierSets: Option[Seq[ModifierSetRecord]] = None,
        modifierSetIds: Option[Seq[UUID]] = None,
        taxRatesMap: Option[Map[UUID, Seq[TaxRateRecord]]] = None,
        taxRateIdsMap: Option[Map[UUID, Seq[UUID]]] = None,
        withTaxRateLocations: Boolean = false,
        stockLevel: Option[BigDecimal] = None,
        variantStockLevels: Seq[Option[BigDecimal]] = Seq.empty,
        optionIds: Seq[UUID] = Seq.empty,
        supplierIds: Seq[UUID] = Seq.empty,
        imageUploads: Seq[ImageUploadRecord] = Seq.empty,
        recipeDetails: Option[Seq[RecipeDetailRecord]] = None,
        priceRange: Option[MonetaryRange] = None,
        costRange: Option[MonetaryRange] = None,
        singleVariantOptionIds: Seq[UUID] = Seq.empty,
        productSystemCategories: Seq[ProductCategoryRecord] = Seq.empty,
        reorderAmount: Option[BigDecimal] = None,
        catalogCategories: Option[Seq[CategoryRecord]] = None,
        productCatalogCategories: Seq[ProductCategoryRecord] = Seq.empty,
        productCategoryOptions: Seq[CatalogCategoryOption] = Seq.empty,
        bundleSets: Map[BundleSetRecord, Seq[BundleOptionRecord]] = Map.empty,
        modifierPositions: Option[Seq[ModifierPosition]] = None,
      ) = {
      entity.id ==== record.id
      entity.`type` ==== record.`type`
      entity.scope ==== record.scope
      entity.isCombo ==== record.isCombo
      entity.name ==== record.name
      entity.description ==== record.description
      entity.brandId ==== record.brandId
      entity.price ==== MonetaryAmount(record.priceAmount, currency)
      entity.priceRange ==== priceRange
      entity.cost ==== MonetaryAmount.extract(record.costAmount, currency)
      entity.costRange ==== costRange
      entity.unit ==== record.unit
      entity.margin ==== record.margin
      entity.upc ==== (if (entity.`type`.isStorable) record.upc else None)
      entity.sku ==== (if (entity.`type`.isStorable) record.sku else None)
      entity.isVariantOfProductId ==== record.isVariantOfProductId
      entity.hasVariant ==== record.hasVariant
      entity.trackInventory ==== record.trackInventory
      entity.trackInventoryParts ==== record.trackInventoryParts
      entity.active ==== record.active
      entity.applyPricingToAllLocations ==== record.applyPricingToAllLocations
      entity.orderRoutingBar ==== record.orderRoutingBar
      entity.orderRoutingKitchen ==== record.orderRoutingKitchen
      entity.hasParts ==== record.hasParts
      entity.options.map(_.id) ==== singleVariantOptionIds

      entity.categories.map(_.map(_.id)).getOrElse(Seq.empty) must containTheSameElementsAs(
        systemCategories.map(_.map(_.id)).getOrElse(Seq.empty),
      )
      entity.categoryIds.getOrElse(Seq.empty) must containTheSameElementsAs(systemCategoryIds.getOrElse(Seq.empty))

      entity.catalogCategories.map(_.map(_.id)).getOrElse(Seq.empty) must containTheSameElementsAs(
        catalogCategories.map(_.map(_.id)).getOrElse(Seq.empty),
      )

      entity.variantProducts.getOrElse(Seq.empty).map(_.id) must containTheSameElementsAs(variantIds)

      val entityOptionIds = entity.variantProducts.getOrElse(Seq.empty).flatMap(_.options.map(_.id))
      entityOptionIds must containTheSameElementsAs(optionIds)

      val entityStockLevel = entity.variantProducts.getOrElse(Seq.empty).map(_.stockLevel)
      entityStockLevel must containTheSameElementsAs(variantStockLevels)

      entity.modifiers.getOrElse(Seq.empty).map(_.id) must containTheSameElementsAs(
        modifierSets.getOrElse(Seq.empty).map(_.id),
      )

      entity.modifierIds.getOrElse(Seq.empty) must containTheSameElementsAs(
        modifierSetIds.getOrElse(Seq.empty),
      )

      if (modifierPositions.isDefined)
        entity.modifierPositions.getOrElse(Seq.empty) must containTheSameElementsAs(
          modifierPositions.getOrElse(Seq.empty),
        )

      entity.locationOverrides.keySet ==== locationIds.toSet

      if (taxRatesMap.isDefined) {
        val entityTaxRateIds =
          entity.locationOverrides.transform((_, v) => v.taxRates.getOrElse(Seq.empty).map(_.id))

        taxRatesMap.get.transform((_, v) => v.map(_.id)) ==== entityTaxRateIds
      }

      if (withTaxRateLocations)
        entity.locationOverrides.values.toSeq.flatMap(_.taxRates).flatMap { taxRates =>
          taxRates.map(taxRate => taxRate.locationOverrides must beSome)
        }

      if (taxRateIdsMap.isDefined) {
        val entityTaxRateIds =
          entity.locationOverrides.transform((_, v) => v.taxRateIds.orElse(Some(Seq.empty)))

        taxRateIdsMap.map(_.transform((_, v) => Option(v))) ==== Some(entityTaxRateIds)
      }

      entity.stockLevel ==== stockLevel
      entity.reorderAmount ==== reorderAmount
      entity.suppliers.getOrElse(Seq.empty).map(_.id) ==== supplierIds

      val entityImgUrls = entity.avatarImageUrls
      entityImgUrls.map(_.imageUploadId) should containTheSameElementsAs(imageUploads.map(_.id))
      entityImgUrls.map(_.urls) should containTheSameElementsAs(imageUploads.map(_.urls.getOrElse(Map.empty)))

      entity.recipeDetails.map(_.map(_.id)).getOrElse(Seq.empty) should containTheSameElementsAs(
        recipeDetails.map(_.map(_.id)).getOrElse(Seq.empty),
      )

      assertCategoryPositions(entity.categoryPositions.getOrElse(Seq.empty), productSystemCategories)
      assertCategoryPositions(entity.catalogCategoryPositions.getOrElse(Seq.empty), productCatalogCategories)
      assertCategoryOptions(entity.catalogCategoryOptions.getOrElse(Seq.empty), productCategoryOptions)

      assertBundleSets(entity.bundleSets, bundleSets)
    }

    private def assertBundleSets(entities: Seq[BundleSet], records: Map[BundleSetRecord, Seq[BundleOptionRecord]]) = {
      def assertBundleSet(entity: BundleSet, records: Map[BundleSetRecord, Seq[BundleOptionRecord]]) = {

        val maybeRecord = records.keys.find(_.id == entity.id)
        maybeRecord must beSome
        val record = maybeRecord.get
        val options = records.getOrElse(record, Seq.empty)

        entity.name ==== record.name
        entity.position ==== record.position
        entity.minQuantity ==== record.minQuantity
        entity.maxQuantity ==== record.maxQuantity

        entities.map(_.options.map(assertBundleOption(_, options)))
      }
      entities.size ==== records.keys.size
      entities.map(assertBundleSet(_, records))
    }

    private def assertBundleOption(entity: BundleOption, options: Seq[BundleOptionRecord]) = {
      val maybeRecord = options.find(_.id == entity.id)
      maybeRecord must beSome
      val record = maybeRecord.get

      entity.article.id ==== record.articleId
      entity.priceAdjustment ==== record.priceAdjustment
      entity.position ==== record.position
    }

    private def assertCategoryOptions(
        entities: Seq[CatalogCategoryOption],
        expectations: Seq[CatalogCategoryOption],
      ) = {
      entities.size ==== expectations.size
      entities.map(assertCategoryOption(_, expectations))
    }

    private def assertCategoryOption(entity: CatalogCategoryOption, expectations: Seq[CatalogCategoryOption]) = {
      val maybeExpectation = expectations.find(_.categoryId == entity.categoryId)
      maybeExpectation must beSome

      val expectation = maybeExpectation.get
      expectation.deliveryEnabled ==== entity.deliveryEnabled
      expectation.takeAwayEnabled ==== entity.takeAwayEnabled
    }

    private def assertCategoryPositions(entities: Seq[CategoryPosition], records: Seq[ProductCategoryRecord]) = {
      entities.size ==== records.size
      entities.foreach { entity =>
        val record = records.find(_.categoryId == entity.categoryId).get
        entity.categoryId ==== record.categoryId
        entity.position ==== record.position
      }
    }

    protected def assertArticleCreation(
        creation: ArticleCreation,
        scope: ArticleScope,
        productId: UUID,
        systemCategories: Option[Seq[UUID]] = None,
        catalogCategories: Option[Seq[UUID]] = None,
        suppliers: Option[Seq[UUID]] = None,
      ) =
      assertArticleUpdate(
        creation.asUpdate,
        scope,
        productId,
        systemCategories = systemCategories,
        catalogCategories = catalogCategories,
        suppliers = suppliers,
      )

    protected def assertArticleUpdate(
        update: ArticleUpdateEntity,
        scope: ArticleScope,
        productId: UUID,
        systemCategories: Option[Seq[UUID]] = None,
        catalogCategories: Option[Seq[UUID]] = None,
        suppliers: Option[Seq[UUID]] = None,
      ) = {
      val mainProduct = articleDao.findById(productId).await.get
      mainProduct.scope ==== scope

      if (update.name.isDefined) update.name ==== Some(mainProduct.name)
      if (update.description.isDefined) update.description ==== mainProduct.description
      if (update.brandId.isDefined) update.brandId ==== mainProduct.brandId
      if (update.sku.isDefined && !mainProduct.hasVariant) update.sku ==== mainProduct.sku
      if (update.upc.isDefined && !mainProduct.hasVariant) update.upc ==== mainProduct.upc
      if (update.price.isDefined) update.price ==== Some(mainProduct.priceAmount)
      if (update.cost.isDefined) update.cost ==== mainProduct.costAmount

      if (update.unit.isDefined) update.unit ==== Some(mainProduct.unit)
      if (update.margin.isDefined) update.margin ==== mainProduct.margin
      if (update.variantProducts.isDefined) update.variantProducts.get.nonEmpty ==== mainProduct.hasVariant

      if (update.applyPricingToAllLocations.isDefined)
        update.applyPricingToAllLocations ==== Some(mainProduct.applyPricingToAllLocations)
      if (update.discountable.isDefined) update.discountable ==== Some(mainProduct.discountable)
      if (update.trackInventory.isDefined) update.trackInventory ==== Some(mainProduct.trackInventory)
      if (update.trackInventoryParts.isDefined) update.trackInventoryParts ==== Some(mainProduct.trackInventoryParts)
      if (update.active.isDefined) update.active ==== Some(mainProduct.active)
      if (update.avatarBgColor.isDefined) update.avatarBgColor ==== mainProduct.avatarBgColor
      if (update.orderRoutingBar.isDefined) update.orderRoutingBar ==== Some(mainProduct.orderRoutingBar)
      if (update.orderRoutingKitchen.isDefined) update.orderRoutingKitchen ==== Some(mainProduct.orderRoutingKitchen)
      if (update.orderRoutingEnabled.isDefined) update.orderRoutingEnabled ==== Some(mainProduct.orderRoutingEnabled)

      if (systemCategories.isDefined) {
        val productSystemCategories =
          productCategoryDao.findByProductId(productId, legacySystemCategoriesOnly = Some(true)).await
        systemCategories.map(_.toSet) ==== Some(productSystemCategories.map(_.categoryId).toSet)
      }

      if (catalogCategories.isDefined) {
        val productCatalogCategories = productCategoryDao.findByProductId(productId).await
        catalogCategories.map(_.toSet) ==== Some(productCatalogCategories.map(_.categoryId).toSet)
      }

      if (suppliers.isDefined) {
        val supplierProducts = supplierProductDao.findByProductId(mainProduct.id).await
        suppliers.map(_.toSet) === Some(supplierProducts.map(_.supplierId).toSet)
      }

      update.variantProducts.map { variantProducts =>
        val variantOptions = variantOptionDao.findByProductId(productId).await
        variantOptions.map(_.id).toSet ==== variantProducts.map(_.optionIds).flatten.toSet

        variantProducts.map { variantProduct =>
          assertVariantUpsertion(variantProduct, mainProduct)
          update.locationOverrides.map { mainLocationPricing =>
            assertLocationPrice(variantProduct.id, variantProduct.locationOverrides)
          }
        }
      }
      assertLocationPrice(productId, update.locationOverrides)
      if (update.makesQuantity.isDefined) assertRecipeDetail(productId, update.makesQuantity.get)

      if (update.bundleSets.isDefined) assertBundleSets(productId, update.bundleSets.get)
    }

    def assertSimpleType(productId: UUID) = {
      val record = articleDao.findById(productId).await.get
      record.isVariantOfProductId ==== Some(record.id)
      record.hasVariant ==== false
      record.`type` ==== ArticleType.Simple
      record.isCombo should beFalse
    }

    def assertTemplateType(productId: UUID) = {
      val record = articleDao.findById(productId).await.get
      record.isVariantOfProductId ==== None
      record.hasVariant ==== true
      record.`type` ==== ArticleType.Template
      record.isCombo should beFalse
    }

    def assertVariantType(productId: UUID, parentId: UUID) = {
      val record = articleDao.findById(productId).await.get
      record.isVariantOfProductId ==== Some(parentId)
      record.hasVariant ==== false
      record.`type` ==== ArticleType.Variant
      record.isCombo should beFalse
    }

    def assertComboType(productId: UUID) = {
      val record = articleDao.findById(productId).await.get
      record.isVariantOfProductId ==== Some(productId)
      record.`type` ==== ArticleType.Simple
      record.isCombo should beTrue
    }

    private def assertVariantUpsertion(variantProductUpsertion: VariantArticleUpdate, parentProduct: ArticleRecord) = {
      val productVariant = articleDao.findById(variantProductUpsertion.id).await.get
      productVariant.name ==== parentProduct.name
      productVariant.description ==== parentProduct.description
      productVariant.brandId ==== parentProduct.brandId
      productVariant.isVariantOfProductId ==== Some(parentProduct.id)
      productVariant.hasVariant should beFalse
      productVariant.orderRoutingEnabled ==== parentProduct.orderRoutingEnabled

      if (variantProductUpsertion.sku.isDefined) variantProductUpsertion.sku ==== productVariant.sku
      if (variantProductUpsertion.upc.isDefined) variantProductUpsertion.upc ==== productVariant.upc
      if (variantProductUpsertion.price.isDefined) variantProductUpsertion.price ==== Some(productVariant.priceAmount)
      if (variantProductUpsertion.cost.isDefined) variantProductUpsertion.cost ==== productVariant.costAmount
      if (variantProductUpsertion.unit.isDefined) variantProductUpsertion.unit ==== Some(productVariant.unit)
      if (variantProductUpsertion.margin.isDefined) variantProductUpsertion.margin ==== productVariant.margin
      if (variantProductUpsertion.applyPricingToAllLocations.isDefined)
        variantProductUpsertion.applyPricingToAllLocations ==== Some(productVariant.applyPricingToAllLocations)
      if (variantProductUpsertion.discountable.isDefined)
        variantProductUpsertion.discountable ==== Some(productVariant.discountable)
      if (variantProductUpsertion.avatarBgColor.isDefined)
        variantProductUpsertion.avatarBgColor ==== productVariant.avatarBgColor

      val variantProducts = productVariantOptionDao.findByProductId(variantProductUpsertion.id).await
      variantProducts.map(_.variantOptionId) should containTheSameElementsAs(variantProductUpsertion.optionIds)
    }

    def assertLocationPrice(productId: UUID, locationOverrides: Map[UUID, Option[ArticleLocationUpdate]]) = {
      val productLocations = productLocationDao.findByItemId(productId).await
      val taxRateLocations = productLocationTaxRateDao.findByProductLocationIds(productLocations.map(_.id)).await
      locationOverrides.map {
        case (locationId, Some(locationOverride)) =>
          val productLocation = productLocations.find(_.locationId == locationId).get
          productLocation.priceAmount ==== locationOverride.price
          productLocation.unit ==== locationOverride.unit
          productLocation.active ==== locationOverride.active.getOrElse(true)
          taxRateLocations.map(_.taxRateId) should containTheSameElementsAs(locationOverride.taxRateIds)
        case (locationId, None) =>
          val productLocation = productLocations.find(_.locationId == locationId)
          productLocation ==== None
          taxRateLocations ==== List()
      }
    }

    def assertRecipeDetail(productId: UUID, makesQuantity: BigDecimal) = {
      val recipeDetails = recipeDetailDao.findByProductId(productId).await
      recipeDetails.size ==== 1
      recipeDetails.head.makesQuantity ==== makesQuantity
    }

    def assertBundleSets(productId: UUID, bundleSets: Seq[BundleSetUpdateEntity]) = {
      def assertBundleSet(bundleSetRecord: BundleSetRecord, bundleSet: BundleSetUpdateEntity) = {
        bundleSetRecord.bundleId ==== productId
        if (bundleSet.name.isDefined) bundleSetRecord.name ==== bundleSet.name
        if (bundleSet.position.isDefined) bundleSetRecord.position ==== bundleSet.position.get
        if (bundleSet.minQuantity.isDefined) bundleSetRecord.minQuantity ==== bundleSet.minQuantity.get
        if (bundleSet.maxQuantity.isDefined) bundleSetRecord.maxQuantity ==== bundleSet.maxQuantity.get
      }

      val bundleSetRecords = daos.bundleSetDao.findByProductId(productId).await
      bundleSetRecords.size ==== bundleSets.size

      bundleSets.map { bundleSet =>
        val bundleSetRecord = bundleSetRecords.find(_.id == bundleSet.id)
        bundleSetRecord must beSome
        assertBundleSet(bundleSetRecord.get, bundleSet)
      }
    }

  }
}
