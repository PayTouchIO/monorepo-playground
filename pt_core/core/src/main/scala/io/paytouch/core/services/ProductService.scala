package io.paytouch.core.services

import java.util.UUID

import akka.actor.ActorRef
import io.paytouch.core.async.monitors.ArticleMonitor
import io.paytouch.core.async.trackers.EventTracker
import io.paytouch.core.data.daos.{ Daos, ProductDao }
import io.paytouch.core.entities._
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.utils.Tagging._
import io.paytouch.core.utils._
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.validators.ProductValidator

import scala.concurrent._

class ProductService(
    val bundleSetService: BundleSetService,
    val catalogCategoryService: CatalogCategoryService,
    val eventTracker: ActorRef withTag EventTracker,
    val imageUploadService: ImageUploadService,
    val kitchenService: KitchenService,
    val messageHandler: SQSMessageHandler,
    val modifierSetProductService: ModifierSetProductService,
    val modifierSetService: ModifierSetService,
    val monitor: ActorRef withTag ArticleMonitor,
    val productCategoryOptionService: ProductCategoryOptionService,
    val productCategoryService: ProductCategoryService,
    val productLocationService: ProductLocationService,
    val receivingOrderProductService: ReceivingOrderProductService,
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

  type Dao = ProductDao
  type Validator = ProductValidator

  protected val dao = daos.productDao
  val receivingOrderProductDao = daos.receivingOrderProductDao

  protected val validator = new ProductValidator

  def create(id: UUID, creation: ProductCreation)(implicit user: UserContext): Future[ErrorsOr[Result[Entity]]] =
    create(id, ProductCreation.convert(creation))

  def update(
      id: UUID,
      productUpdate: ProductUpdate,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Result[Entity]]] =
    update(id, ProductUpdate.convert(productUpdate))

  def create(id: UUID, creation: BundleCreation)(implicit user: UserContext): Future[ErrorsOr[Result[Entity]]] =
    create(id, BundleCreation.convert(creation))

  def update(id: UUID, bundleUpdate: BundleUpdate)(implicit user: UserContext): Future[ErrorsOr[Result[Entity]]] =
    update(id, BundleUpdate.convert(bundleUpdate))

  def updateAverageCost(productIds: Seq[UUID], locationId: UUID): Future[Unit] =
    for {
      productAverageCosts <- receivingOrderProductService.findAverageCostByProductIds(productIds)
      productAverageCostsByLocation <- receivingOrderProductService.findAverageCostByProductIdsAndLocationId(
        productIds,
        locationId,
      )
      _ <- dao.updateAverageCosts(productAverageCosts)
      _ <- productLocationService.updateAverageCosts(productAverageCostsByLocation, locationId)
    } yield ()

  override protected def sendSyncedMessages(
      entity: Entity,
      locationIds: Seq[UUID],
    )(implicit
      user: UserContext,
    ): Future[Unit] =
    Future.successful {
      locationIds.map(messageHandler.sendEntitySynced(entity, _))
    }
}
