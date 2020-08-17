package io.paytouch.core.services

import java.util.UUID

import akka.actor.ActorRef
import io.paytouch.core.async.monitors.ArticleMonitor
import io.paytouch.core.async.trackers.EventTracker
import io.paytouch.core.data.daos.{ ArticleDao, Daos }
import io.paytouch.core.data.model.enums.{ ArticleScope, ArticleType, UnitType }
import io.paytouch.core.data.model.{ ArticleRecord, GiftCardUpdate => GiftCardUpdateModel }
import io.paytouch.core.entities.{
  ArticleInfo,
  ArticleUpdate,
  UserContext,
  VariantOptionWithType,
  GiftCardUpdate => GiftCardUpdateEntity,
}
import io.paytouch.utils.Tagging.withTag
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.ArticleValidator

import scala.concurrent._

class ArticleService(
    val bundleSetService: BundleSetService,
    val catalogCategoryService: CatalogCategoryService,
    val eventTracker: ActorRef withTag EventTracker,
    val imageUploadService: ImageUploadService,
    val kitchenService: KitchenService,
    val modifierSetProductService: ModifierSetProductService,
    val modifierSetService: ModifierSetService,
    val monitor: ActorRef withTag ArticleMonitor,
    val productCategoryOptionService: ProductCategoryOptionService,
    val productCategoryService: ProductCategoryService,
    val productLocationService: ProductLocationService,
    val recipeDetailService: RecipeDetailService,
    val taxRateLocationService: TaxRateLocationService,
    val taxRateService: TaxRateService,
    val setupStepService: SetupStepService,
    val systemCategoryService: SystemCategoryService,
    val stockService: StockService,
    val supplierProductService: SupplierProductService,
    val supplierService: SupplierService,
    val variantArticleService: VariantArticleService,
    val variantService: VariantService,
    val variantOptionService: VariantService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends GenericArticleService {

  type Dao = ArticleDao
  type Validator = ArticleValidator

  protected val dao = daos.articleDao
  protected val validator = new ArticleValidator

  def convertToUpsertionModel(
      updateEntity: GiftCardUpdateEntity,
      updateModel: Option[GiftCardUpdateModel],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Model]] = {
    val productId = updateModel.flatMap(_.productId).getOrElse(UUID.randomUUID)
    convertToUpsertionModel(productId, toArticleUpdate(updateEntity))
  }

  private def toArticleUpdate(updateEntity: GiftCardUpdateEntity)(implicit user: UserContext): Update =
    ArticleUpdate(
      name = updateEntity.name,
      description = None,
      categoryIds = None,
      brandId = None,
      supplierIds = None,
      sku = updateEntity.sku,
      upc = updateEntity.upc,
      cost = None,
      price = Some(0),
      unit = Some(UnitType.`Unit`),
      margin = None,
      trackInventory = None,
      trackInventoryParts = None,
      active = None,
      applyPricingToAllLocations = None,
      discountable = None,
      avatarBgColor = None,
      isService = None,
      orderRoutingBar = None,
      orderRoutingKitchen = None,
      orderRoutingEnabled = None,
      variants = None,
      variantProducts = None,
      locationOverrides = Map.empty,
      imageUploadIds = None,
      notes = None,
      scope = Some(ArticleScope.Product),
      `type` = Some(ArticleType.GiftCard),
      isCombo = Some(false),
      makesQuantity = None,
      bundleSets = None,
    )

  def getArticleInfoPerArticleId(articleIds: Seq[UUID]): Future[Seq[ArticleInfo]] =
    for {
      articles <- dao.findByIds(articleIds)
      optionsPerRecord <- getOptionalVariantOptionsPerProducts(articles)(true)
    } yield fromRecordsAndOptionsToInfoEntities(articles, optionsPerRecord)
}
