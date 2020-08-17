package io.paytouch.core.services

import java.time.Duration
import java.util.UUID

import akka.actor.ActorRef
import cats.data.Validated.{ Invalid, Valid }
import io.paytouch.core.LocationOverridesPer
import io.paytouch.core.async.monitors.ProductChange
import io.paytouch.core.conversions.ArticleConversions
import io.paytouch.core.data.daos.GenericArticleDao
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.data.model.upsertions.{ ArticleUpsertion => ArticleUpsertionModel }
import io.paytouch.core.data.model.{
  ArticleRecord,
  ImageUploadRecord,
  ProductCategoryRecord,
  ProductLocationRecord,
  ArticleUpdate => ProductUpdateModel,
}
import io.paytouch.core.entities.enums.{ ExposedName, MerchantSetupSteps }
import io.paytouch.core.entities.{ UserContext, ArticleUpdate => ArticleUpdateEntity, Product => ProductEntity, _ }
import io.paytouch.core.expansions.ArticleExpansions
import io.paytouch.core.filters.ArticleFilters
import io.paytouch.core.services.features._
import io.paytouch.core.utils.FindResult._
import io.paytouch.core.utils.ResultType
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.utils.{ Implicits, Multiple }
import io.paytouch.core.validators.{ GenericArticleValidator, MainArticleValidator }

import scala.concurrent._

trait GenericArticleService
    extends ArticleConversions
       with FindAllFeature
       with FindByIdFeature
       with SoftDeleteFeature
       with UpdateActiveLocationsFeature
       with CreateAndUpdateFeatureWithStateProcessing
       with Implicits {

  type Creation = ArticleCreation
  type Dao <: GenericArticleDao
  type Entity = ProductEntity
  type Expansions = ArticleExpansions
  type Filters = ArticleFilters
  type Model = ArticleUpsertionModel
  type Record = ArticleRecord
  type Update = ArticleUpdateEntity
  type Validator <: GenericArticleValidator

  val classShortName = ExposedName.Product

  def monitor: ActorRef
  def bundleSetService: BundleSetService
  def catalogCategoryService: CatalogCategoryService
  def systemCategoryService: SystemCategoryService
  def imageUploadService: ImageUploadService
  def kitchenService: KitchenService
  def modifierSetProductService: ModifierSetProductService
  def modifierSetService: ModifierSetService
  def productCategoryService: ProductCategoryService
  def productCategoryOptionService: ProductCategoryOptionService
  def productLocationService: ProductLocationService
  def recipeDetailService: RecipeDetailService
  def taxRateLocationService: TaxRateLocationService
  def taxRateService: TaxRateService
  def setupStepService: SetupStepService
  def stockService: StockService
  def supplierProductService: SupplierProductService
  def supplierService: SupplierService
  def variantArticleService: VariantArticleService
  def variantService: VariantService
  def variantOptionService: VariantService

  type State = (ArticleRecord, Seq[ArticleRecord], Seq[ProductLocationRecord], Seq[ImageUploadRecord])

  val variantProductDao = daos.variantProductDao
  val defaultFilters = ArticleFilters()

  val mainArticleValidator = new MainArticleValidator

  val imageUploadDao = daos.imageUploadDao
  val productLocationDao = daos.productLocationDao

  val itemLocationService = productLocationService

  def assignModifierSets(
      productId: UUID,
      assignment: ProductModifierSetsAssignment,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Unit]] =
    mainArticleValidator.accessOneById(productId).flatMap {
      case Valid(_) =>
        if (assignment.modifierSets.isDefined)
          modifierSetProductService.associateModifierSetsToProduct(productId, assignment.modifierSets.get)
        else
          // Legacy parameter format
          modifierSetProductService.associateModifierSetIdsToProduct(productId, assignment.modifierSetIds.get)
      case i @ Invalid(_) => Future.successful(i)
    }

  def enrich(products: Seq[Record], f: Filters)(e: Expansions)(implicit user: UserContext): Future[Seq[Entity]] = {
    val locationOverridesPerProductR =
      getLocationOverridesPerProduct(products, f.locationIds)(
        withReorderAmount = e.withReorderAmount,
        withStockLevel = e.withStockLevel,
        withTaxRates = e.withTaxRates,
        withTaxRateLocations = e.withTaxRateLocations,
        withTaxRateIds = e.withTaxRateIds,
      )

    val systemCategoriesPerProductR =
      getOptionalSystemCategoriesPerProducts(products)(e.withSystemCategories || e.withSystemCategoryIds)

    val catalogCategoriesPerProductR = getOptionalCatalogCategoriesPerProducts(products)(e.withCatalogCategories)

    val systemCategoryPositionsPerProductR =
      getOptionalSystemCategoryPositionsPerProducts(products)(e.withSystemCategoryPositions)

    val catalogCategoryPositionsPerProductR =
      getOptionalCatalogCategoryPositionsPerProducts(products)(e.withCatalogCategoryPositions)

    val catalogCategoryOptionsPerProductR =
      getOptionalCatalogCategoryOptionsPerProducts(products)(e.withCatalogCategoryOptions)

    val variantProductPerProductR =
      getOptionalVariantProductsPerProducts(products, f.locationIds)(e.withVariants)(e)

    val variantsPerProductR = getOptionalVariantsPerProducts(products)(e.withVariants)
    val variantOptionsPerProductR = getOptionalVariantOptionsPerProducts(products)(e.withVariants)
    val modifiersPerProductR = getOptionalModifiersPerProducts(products)(e.withModifiers, e.withModifierIds)
    val modifierPositionsPerProductR = getOptionalModifierPositionsPerProducts(products)(e.withModifierPositions)
    val stockLevelPerProductR = getOptionalStockLevelPerProducts(products, f.locationIds)(e.withStockLevel)
    val reorderAmountPerProductR = getOptionalReorderAmountPerProducts(products, f.locationIds)(e.withReorderAmount)
    val suppliersPerProductR = getOptionalSuppliersPerProducts(products)(e.withSuppliers)
    val recipeDetailsPerProductR = getOptionalRecipeDetailsPerProduct(products)(e.withRecipeDetails)
    val priceRangePerProductR = getOptionalPriceRangePerProduct(products, f.locationIds)(e.withPriceRanges)
    val costRangePerProductR = getOptionalCostRangePerProduct(products, f.locationIds)(e.withCostRanges)
    val imageUrlsPerProductR = getImageUrlsPerProduct(products)
    val bundleSetsPerProductR = getBundleSetsPerProduct(products)

    for {
      locationOverridesPerProduct <- locationOverridesPerProductR
      systemCategoriesPerProductData <- systemCategoriesPerProductR
      systemCategoriesPerProduct =
        extractSystemCategoriesPerProduct(systemCategoriesPerProductData)(e.withSystemCategories)
      systemCategoryIdsPerProduct =
        extractSystemCategoryIdsPerProduct(systemCategoriesPerProductData)(e.withSystemCategoryIds)
      catalogCategoriesPerProduct <- catalogCategoriesPerProductR
      systemCategoryPositionsPerProduct <- systemCategoryPositionsPerProductR
      catalogCategoryOptionsPerProduct <- catalogCategoryOptionsPerProductR
      catalogCategoryPositionsPerProduct <- catalogCategoryPositionsPerProductR
      variantProductsPerProduct <- variantProductPerProductR
      variantsPerProduct <- variantsPerProductR
      variantOptionsPerProduct <- variantOptionsPerProductR
      (modifiersPerProduct, modifierIdsPerProduct) <- modifiersPerProductR
      modifierPositionsPerProduct <- modifierPositionsPerProductR
      stockLevelPerProduct <- stockLevelPerProductR
      reorderAmountPerProduct <- reorderAmountPerProductR
      suppliersPerProduct <- suppliersPerProductR
      recipeDetailsPerProduct <- recipeDetailsPerProductR
      priceRangePerProduct <- priceRangePerProductR
      costRangePerProduct <- costRangePerProductR
      imageUrlsPerProduct <- imageUrlsPerProductR
      bundleSetsPerProduct <- bundleSetsPerProductR
      kitchenIds =
        locationOverridesPerProduct
          .flatMap { case (_, v) => v.flatMap { case (_, w) => w.routeToKitchenId } }
          .toSeq
          .distinct
      kitchens <- kitchenService.findByIds(kitchenIds)
    } yield fromRecordsAndOptionsToEntities(
      products,
      locationOverridesPerProduct,
      systemCategoriesPerProduct,
      systemCategoryIdsPerProduct,
      variantProductsPerProduct,
      variantsPerProduct,
      modifiersPerProduct,
      modifierIdsPerProduct,
      modifierPositionsPerProduct,
      imageUrlsPerProduct,
      stockLevelPerProduct,
      reorderAmountPerProduct,
      suppliersPerProduct,
      recipeDetailsPerProduct,
      priceRangePerProduct,
      costRangePerProduct,
      variantOptionsPerProduct.getOrElse(Map.empty),
      systemCategoryPositionsPerProduct,
      catalogCategoriesPerProduct,
      catalogCategoryOptionsPerProduct,
      catalogCategoryPositionsPerProduct,
      bundleSetsPerProduct,
      kitchens,
    )
  }

  private def getImageUrlsPerProduct(products: Seq[Record]): Future[Map[Record, Seq[ImageUrls]]] = {
    val productIds = products.map(_.id)
    imageUploadService.findByObjectIds(productIds, ImageUploadType.Product).map(_.mapKeysToRecords(products))
  }

  private def getBundleSetsPerProduct(products: Seq[Record]): Future[Map[Record, Seq[BundleSet]]] = {
    val productIds = products.map(_.id)
    bundleSetService.findAllPerProduct(productIds).map(_.mapKeysToRecords(products))
  }

  private def getLocationOverridesPerProduct(
      products: Seq[Record],
      locationIds: Option[Seq[UUID]],
    )(
      withReorderAmount: Boolean,
      withStockLevel: Boolean,
      withTaxRates: Boolean,
      withTaxRateLocations: Boolean,
      withTaxRateIds: Boolean,
    )(implicit
      user: UserContext,
    ): Future[LocationOverridesPer[Record, ProductLocation]] =
    itemLocationService
      .findAllByItemIdsAsMap(products, locationIds = locationIds)(
        withReorderAmount = withReorderAmount,
        withStockLevel = withStockLevel,
        withTaxRates = withTaxRates,
        withTaxRateLocations = withTaxRateLocations,
        withTaxRateIds = withTaxRateIds,
      )

  private def getOptionalSystemCategoriesPerProducts(
      products: Seq[Record],
    )(
      withCategories: Boolean,
    )(implicit
      user: UserContext,
    ): Future[DataSeqByRecord[Category]] =
    if (withCategories) systemCategoryService.getCategoriesPerProducts(products).map(Some(_))
    else Future.successful(None)

  private def getOptionalCatalogCategoriesPerProducts(
      products: Seq[Record],
    )(
      withCategories: Boolean,
    )(implicit
      user: UserContext,
    ): Future[DataSeqByRecord[Category]] =
    if (withCategories) catalogCategoryService.getCategoriesPerProducts(products).map(Some(_))
    else Future.successful(None)

  private def getOptionalSystemCategoryPositionsPerProducts(
      products: Seq[Record],
    )(
      withCategoryPositions: Boolean,
    ): Future[DataSeqByRecord[CategoryPosition]] =
    getOptionalCategoryPositionsPerProducts(products, withCategoryPositions)(
      productCategoryService.findSystemCategoryPerProduct,
    )

  private def getOptionalCatalogCategoryPositionsPerProducts(
      products: Seq[Record],
    )(
      withCategoryPositions: Boolean,
    ): Future[DataSeqByRecord[CategoryPosition]] =
    getOptionalCategoryPositionsPerProducts(products, withCategoryPositions)(
      productCategoryService.findPerProduct,
    )

  private def getOptionalCatalogCategoryOptionsPerProducts(
      products: Seq[Record],
    )(
      withCatalogCategoryOptions: Boolean,
    ): Future[DataSeqByRecord[CatalogCategoryOption]] =
    getExpandedMappedField[Seq[CatalogCategoryOption]](
      productCategoryOptionService.getOptionalCategoryOptionsPerProducts,
      _.id,
      products,
      withCatalogCategoryOptions,
    )

  private def getOptionalCategoryPositionsPerProducts(
      products: Seq[Record],
      withPositions: Boolean,
    )(
      f: Seq[UUID] => Future[Map[UUID, Seq[ProductCategoryRecord]]],
    ): Future[DataSeqByRecord[CategoryPosition]] = {
    val categoryPositionExtractor = { pIds: Seq[UUID] =>
      f(pIds).map(_.transform((_, v) => toCategoryPositions(v)))
    }

    getExpandedMappedField[Seq[CategoryPosition]](categoryPositionExtractor, _.id, products, withPositions)
  }

  private def getOptionalVariantProductsPerProducts(
      products: Seq[Record],
      locationIds: Option[Seq[UUID]],
    )(
      withVariants: Boolean,
    )(
      e: Expansions,
    )(implicit
      user: UserContext,
    ): Future[DataSeqByRecord[Entity]] =
    if (withVariants)
      variantArticleService
        .findVariantsByParentIds(products.map(_.id), locationIds)(e)
        .map(variantProductsPerProduct => Some(variantProductsPerProduct.mapKeysToRecords(products)))
    else
      Future.successful(None)

  private def getOptionalModifiersPerProducts(
      products: Seq[Record],
    )(
      withModifiers: Boolean,
      withModifierIds: Boolean,
    )(implicit
      user: UserContext,
    ): Future[(DataSeqByRecord[ModifierSet], DataSeqByRecord[UUID])] =
    if (withModifiers || withModifierIds) {
      val mainProductIds = products.map(_.mainProductId)
      modifierSetService.findByProductIds(mainProductIds).map { result =>
        val modifierSetsPerProduct = result.flatMap {
          case (mainProductId, modifierSets) =>
            val productsPerMainProduct = products.filter(_.mainProductId == mainProductId)
            productsPerMainProduct.map(product => product -> modifierSets)
        }
        val modifiers = if (withModifiers) Some(modifierSetsPerProduct) else None
        val modifierIds =
          if (withModifierIds) Some(modifierSetsPerProduct.transform((_, v) => v.map(_.id))) else None
        (modifiers, modifierIds)
      }
    }
    else Future.successful((None, None))

  private def getOptionalModifierPositionsPerProducts(
      products: Seq[Record],
    )(
      withModifierPositions: Boolean,
    ): Future[DataSeqByRecord[ModifierPosition]] =
    if (withModifierPositions) {
      val mainProductIds = products.map(_.mainProductId)
      modifierSetProductService.findPerProductIds(mainProductIds).map { modifierSetProductsPerProduct =>
        val modifierPositionsPerProduct = modifierSetProductsPerProduct.flatMap {
          case (mainProductId, modifierSetProducts) =>
            val modifierPositions = toModifierPositions(modifierSetProducts)
            val productsPerMainProduct = products.filter(_.mainProductId == mainProductId)
            productsPerMainProduct.map(product => product -> modifierPositions)
        }
        Some(modifierPositionsPerProduct)
      }
    }
    else Future.successful(None)

  private def getOptionalVariantsPerProducts(
      products: Seq[Record],
    )(
      withVariants: Boolean,
    )(implicit
      user: UserContext,
    ): Future[DataSeqByRecord[VariantOptionType]] =
    getExpandedMappedField[Seq[VariantOptionType]](
      variantService.findVariantOptionTypesByProductIds,
      _.id,
      products,
      withVariants,
    )

  protected def getOptionalVariantOptionsPerProducts(
      products: Seq[Record],
    )(
      withVariants: Boolean,
    ): Future[DataSeqByRecord[VariantOptionWithType]] =
    getExpandedMappedField[Seq[VariantOptionWithType]](
      variantOptionService.findVariantOptionsByVariantIds,
      _.id,
      products,
      withVariants,
    )

  private def getOptionalStockLevelPerProducts(
      products: Seq[Record],
      locationIds: Option[Seq[UUID]],
    )(
      withStockLevel: Boolean,
    )(implicit
      user: UserContext,
    ): Future[DataByRecord[BigDecimal]] =
    getExpandedMappedField(
      stockService.findStockLevelByArticleIds(_, locationIds),
      _.id,
      products,
      withStockLevel,
    ).map(_.map(_.transform((_, v) => v.values.sum)))

  private def getOptionalReorderAmountPerProducts(
      products: Seq[Record],
      locationIds: Option[Seq[UUID]],
    )(
      withReorderAmount: Boolean,
    )(implicit
      user: UserContext,
    ): Future[DataByRecord[BigDecimal]] =
    getExpandedMappedField(
      stockService.findReorderAmountByArticleIds(_, locationIds),
      _.id,
      products,
      withReorderAmount,
    ).map(_.map(_.transform((_, v) => v.values.sum)))

  private def getOptionalSuppliersPerProducts(
      products: Seq[Record],
    )(
      withSuppliers: Boolean,
    ): Future[DataSeqByRecord[SupplierInfo]] =
    if (withSuppliers) {
      val mainProductIds = products.map(_.mainProductId)
      supplierService.findSupplierInfoByProductIds(mainProductIds).map { result =>
        val suppliersPerProduct = result.flatMap {
          case (mainProductId, suppliers) =>
            val productsPerMainProduct = products.filter(_.mainProductId == mainProductId)
            productsPerMainProduct.map(product => product -> suppliers)
        }
        Some(suppliersPerProduct)
      }
    }
    else Future.successful(None)

  private def getOptionalRecipeDetailsPerProduct(
      products: Seq[Record],
    )(
      withRecipeDetails: Boolean,
    )(implicit
      user: UserContext,
    ): Future[DataSeqByRecord[RecipeDetail]] =
    getExpandedMappedField[Seq[RecipeDetail]](
      recipeDetailService.findPerProductIds,
      _.id,
      products,
      withRecipeDetails,
    )

  private def getOptionalPriceRangePerProduct(
      products: Seq[Record],
      locationIds: Option[Seq[UUID]],
    )(
      withPriceRanges: Boolean,
    )(implicit
      user: UserContext,
    ): Future[DataByRecord[MonetaryRange]] =
    if (withPriceRanges)
      productLocationService.findPriceRangesByProductIds(products.map(_.id), locationIds).map { priceRangePerProduct =>
        Some(priceRangePerProduct.mapKeysToRecords(products))
      }
    else Future.successful(None)

  private def getOptionalCostRangePerProduct(
      products: Seq[Record],
      locationIds: Option[Seq[UUID]],
    )(
      withCostRanges: Boolean,
    )(implicit
      user: UserContext,
    ): Future[DataByRecord[MonetaryRange]] =
    if (withCostRanges)
      productLocationService.findCostRangesByProductIds(products.map(_.id), locationIds).map { costRangePerProduct =>
        Some(costRangePerProduct.mapKeysToRecords(products))
      }
    else
      Future.successful(None)

  protected def convertToUpsertionModel(
      productId: UUID,
      update: Update,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Model]] = {

    def toUpsertionModel(productId: UUID, enrichedUpdate: Update) =
      for {
        product <- convertToProductUpdate(productId, enrichedUpdate)
        variantProducts <-
          variantArticleService
            .convertToVariantArticleUpsertions(productId, enrichedUpdate.variantProducts, product.toOption)
        variantOptionTypes <- variantService.convertToVariantOptionTypeUpdates(productId, enrichedUpdate)
        variantOptions <- variantService.convertToVariantOptionUpdates(productId, enrichedUpdate)
        productLocations <-
          itemLocationService
            .convertToItemLocationUpdates(productId, enrichedUpdate.locationOverrides)
        productLocationUpdates = productLocations.getOrElse(Map.empty).values.flatten.toSeq
        productLocationTaxRates <-
          taxRateLocationService
            .convertToProductLocationTaxRateUpdates(productId, enrichedUpdate, productLocationUpdates)
        productCategories <- productCategoryService.convertToProductSystemCategoryUpdates(productId, enrichedUpdate)
        supplierProducts <- supplierProductService.convertToSupplierProductUpdates(productId, enrichedUpdate)
        imageUploads <-
          imageUploadService
            .convertToImageUploadUpdates(productId, ImageUploadType.Product, update.imageUploadIds)
        recipeDetails <- recipeDetailService.convertToRecipeDetailUpdate(productId, enrichedUpdate)
        bundleSets <- bundleSetService.convertToBundleSetUpdates(productId, enrichedUpdate)
      } yield Multiple.combine(
        product,
        variantProducts,
        variantOptionTypes,
        variantOptions,
        productLocations,
        productLocationTaxRates,
        productCategories,
        supplierProducts,
        imageUploads,
        recipeDetails,
        bundleSets,
      )(ArticleUpsertionModel)

    validator.validateUpsertion(productId, update).flatMap {
      case Valid(enrichedUpdate) => toUpsertionModel(productId, enrichedUpdate)
      case i @ Invalid(_)        => Future.successful(i)
    }
  }

  private def convertToProductUpdate(
      productId: UUID,
      update: Update,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[ProductUpdateModel]] =
    Future.successful {
      Multiple.success(fromUpsertionToUpdate(productId, update))
    }

  override implicit def toFutureResultTypeEntity(
      f: Future[(ResultType, Record)],
    )(implicit
      user: UserContext,
    ): Future[(ResultType, Entity)] =
    for {
      (resultType, record) <- f
      enrichedRecord <- enrich(record, defaultFilters)(ArticleExpansions.withVariantsOnly)
    } yield (resultType, enrichedRecord)

  protected def saveCurrentState(record: Record)(implicit user: UserContext): Future[State] =
    for {
      variants <- variantProductDao.findVariantByParentId(record.id)
      productIds = (record +: variants).map(_.id)
      imageUploads <- imageUploadDao.findByObjectIds(productIds, ImageUploadType.Product)
      productLocations <- productLocationDao.findByItemIds(productIds)
    } yield (record, variants, productLocations, imageUploads)

  protected def saveCreationState(id: UUID, creation: Creation)(implicit user: UserContext): Future[Option[State]] =
    Future.successful(None)

  protected def sendSyncedMessages(entity: Entity, locationIds: Seq[UUID])(implicit user: UserContext): Future[Unit] =
    Future.unit

  protected def processChangeOfState(
      maybeState: Option[State],
      update: Update,
      resultType: ResultType,
      entity: Entity,
    )(implicit
      user: UserContext,
    ): Future[Unit] = {
    maybeState.map { state =>
      monitor ! ProductChange(state, update, user)

      val (_, _, productLocations, _) = state
      sendSyncedMessages(entity, productLocations.map(_.locationId))
    }
    setupStepService.simpleCheckStepCompletion(user.merchantId, MerchantSetupSteps.ImportProducts)
  }

  def getPopularProducts(
      locationIds: Option[Seq[UUID]],
    )(
      duration: Duration,
    )(implicit
      user: UserContext,
      pagination: Pagination,
    ): Future[FindResult[Entity]] = {
    val filters = defaultFilters.copy(locationIds = locationIds)
    val expansions = ArticleExpansions.empty
    val merchantId = user.merchantId
    val itemsResp =
      dao
        .findTopPopularProducts(merchantId, user.accessibleLocations(locationIds))(
          duration,
          pagination.offset,
          pagination.limit,
        )

    val countResp = dao.countAllWithFilters(merchantId, filters)

    for {
      items <- itemsResp
      enrichedData <- enrich(items, filters)(expansions)
      count <- countResp
    } yield (enrichedData, count)
  }

  def findByIds(ids: Seq[UUID])(implicit user: UserContext): Future[Seq[Entity]] = dao.findByIds(ids)

  def getByIds(ids: Seq[UUID])(e: Expansions)(implicit user: UserContext): Future[Seq[Entity]] =
    for {
      records <- dao.findByIds(ids)
      entities <- enrich(records, defaultFilters)(e)
    } yield entities
}
