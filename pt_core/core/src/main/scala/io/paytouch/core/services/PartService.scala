package io.paytouch.core.services

import java.util.UUID

import akka.actor.ActorRef
import io.paytouch.core.async.monitors.ArticleMonitor
import io.paytouch.core.async.trackers.EventTracker
import io.paytouch.core.data.daos.{ Daos, PartDao }
import io.paytouch.core.entities._
import io.paytouch.core.utils._
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.validators.PartValidator
import io.paytouch.core.withTag

import scala.concurrent._

class PartService(
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

  type Dao = PartDao
  type Validator = PartValidator

  protected val dao = daos.partDao
  protected val validator = new PartValidator

  def create(id: UUID, creation: PartCreation)(implicit user: UserContext): Future[ErrorsOr[Result[Entity]]] =
    create(id, PartCreation.convert(creation))

  def update(id: UUID, partUpdate: PartUpdate)(implicit user: UserContext): Future[ErrorsOr[Result[Entity]]] =
    update(id, PartUpdate.convert(partUpdate))

  def create(id: UUID, creation: RecipeCreation)(implicit user: UserContext): Future[ErrorsOr[Result[Entity]]] =
    create(id, RecipeCreation.convert(creation))

  def update(id: UUID, recipeUpdate: RecipeUpdate)(implicit user: UserContext): Future[ErrorsOr[Result[Entity]]] =
    update(id, RecipeUpdate.convert(recipeUpdate))

}
