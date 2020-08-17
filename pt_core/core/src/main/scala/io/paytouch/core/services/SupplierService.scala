package io.paytouch.core.services

import java.util.UUID

import scala.concurrent._

import akka.actor.ActorRef

import cats.implicits._

import io.paytouch.core.async.trackers.EventTracker
import io.paytouch.core.calculations.CalculationUtils._
import io.paytouch.core.conversions.SupplierConversions
import io.paytouch.core.data.daos.{ Daos, SupplierDao }
import io.paytouch.core.data.model.upsertions.SupplierUpsertion
import io.paytouch.core.data.model.{ SupplierRecord, SupplierUpdate => SupplierUpdateModel }
import io.paytouch.core.entities.enums.ExposedName
import io.paytouch.core.entities.{
  MonetaryAmount,
  SupplierCreation,
  SupplierInfo,
  UserContext,
  Supplier => SupplierEntity,
  SupplierUpdate => SupplierUpdateEntity,
  _,
}
import io.paytouch.core.expansions.{ NoExpansions, SupplierExpansions }
import io.paytouch.core.filters.{ InventoryFilters, SupplierFilters }
import io.paytouch.core.services.features._
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.SupplierValidator
import io.paytouch.utils.Tagging._
import io.paytouch.core.{ LocationOverridesPer, RichMap }

class SupplierService(
    val eventTracker: ActorRef withTag EventTracker,
    val inventoryService: InventoryService,
    val supplierLocationService: SupplierLocationService,
    val supplierProductService: SupplierProductService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends SupplierConversions
       with FindByIdFeature
       with FindAllFeature
       with CreateAndUpdateFeature
       with DeleteFeature
       with UpdateActiveLocationsFeature {

  type Creation = SupplierCreation
  type Dao = SupplierDao
  type Entity = SupplierEntity
  type Expansions = SupplierExpansions
  type Filters = SupplierFilters
  type Model = SupplierUpsertion
  type Record = SupplierRecord
  type Update = SupplierUpdateEntity
  type Validator = SupplierValidator

  protected val dao = daos.supplierDao
  protected val validator = new SupplierValidator
  val defaultFilters = SupplierFilters()

  val classShortName = ExposedName.Supplier

  val productDao = daos.productDao
  val supplierProductDao = daos.supplierProductDao

  val itemLocationService = supplierLocationService

  def enrich(records: Seq[Record], f: Filters)(e: Expansions)(implicit user: UserContext): Future[Seq[Entity]] = {
    val productsCountBySupplierR =
      getOptionalProductsCountPerSupplier(records, f.locationIds, f.categoryIds)(e.withProductsCount)

    val stockValuesBySupplierR =
      getOptionalStockValuesPerSupplier(records, f.locationIds, f.categoryIds)(e.withStockValues)

    val locationOverridesPerSupplierR =
      getOptionalLocationOverridesPerSupplier(records, f.locationIds)(e.withLocations)

    for {
      productsCountBySupplier <- productsCountBySupplierR
      stockValuesBySupplier <- stockValuesBySupplierR
      locationOverridesPerSupplier <- locationOverridesPerSupplierR
    } yield fromRecordsAndOptionsToEntities(
      records,
      productsCountBySupplier,
      stockValuesBySupplier,
      locationOverridesPerSupplier,
    )
  }

  private def getOptionalProductsCountPerSupplier(
      suppliers: Seq[Record],
      locationIds: Option[Seq[UUID]],
      categoryIds: Option[Seq[UUID]],
    )(
      withProductsCount: Boolean,
    ): Future[Map[Record, Int]] =
    if (withProductsCount)
      supplierProductDao.countProductsBySupplierIds(suppliers.map(_.id), locationIds, categoryIds).map { result =>
        result.mapKeysToRecords(suppliers)
      }
    else
      Future.successful(Map.empty)

  private def getOptionalStockValuesPerSupplier(
      suppliers: Seq[Record],
      locationIds: Option[Seq[UUID]],
      categoryIds: Option[Seq[UUID]],
    )(
      withStockValues: Boolean,
    )(implicit
      user: UserContext,
    ): Future[Map[Record, MonetaryAmount]] =
    if (withStockValues)
      for {
        supplierProducts <- supplierProductDao.findBySupplierIds(suppliers.map(_.id))
        productIds = supplierProducts.map(_.productId).distinct
        products <- productDao.findByIds(productIds)
        inventories <-
          inventoryService
            .enrich(products, InventoryFilters(categoryIds = categoryIds, locationIds = locationIds))(NoExpansions())
      } yield {
        val inventoryPerProductId: Map[UUID, Inventory] =
          inventories.map(i => i.id -> i).toMap

        val productIdsPerSuppliers: Map[SupplierRecord, Seq[UUID]] =
          supplierProducts
            .groupBy(_.supplierId)
            .transform((_, v) => v.map(_.productId))
            .mapKeysToRecords(suppliers)

        val inventoriesPerSuppliers: Map[SupplierRecord, Seq[Inventory]] =
          productIdsPerSuppliers
            .transform((_, v) => v.flatMap(inventoryPerProductId.get))

        suppliers.map { supplier =>
          val supplierInventories = inventoriesPerSuppliers.getOrElse(supplier, Seq.empty)
          val stockValue = supplierInventories.map(_.stockValue).sumNonZero

          supplier -> stockValue
        }.toMap
      }
    else
      Future.successful(Map.empty)

  private def getOptionalLocationOverridesPerSupplier(
      suppliers: Seq[Record],
      locationIds: Option[Seq[UUID]],
    )(
      withLocations: Boolean,
    )(implicit
      user: UserContext,
    ): Future[Option[LocationOverridesPer[Record, ItemLocation]]] =
    if (withLocations)
      itemLocationService
        .findAllByItemIdsAsMap(suppliers.map(_.id), locationIds = locationIds)
        .map(_.mapKeysToRecords(suppliers).some)
    else
      Future.successful(None)

  def convertToUpsertionModel(id: UUID, update: Update)(implicit user: UserContext): Future[ErrorsOr[Model]] =
    for {
      supplier <- convertToSupplierUpdate(id, update)
      supplierLocations <- supplierLocationService.convertToSupplierLocationUpdates(id, update.locationOverrides)
      supplierProducts <- supplierProductService.convertToSupplierProductUpdates(id, update)
    } yield Multiple.combine(supplier, supplierLocations, supplierProducts)(SupplierUpsertion)

  private def convertToSupplierUpdate(
      id: UUID,
      update: Update,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[SupplierUpdateModel]] =
    validator.validateEmailFormat(update.email).mapNested { validEmail =>
      fromUpsertionToUpdate(id, update.copy(email = validEmail))
    }

  def findSupplierInfoByProductIds(productIds: Seq[UUID]): Future[Map[UUID, Seq[SupplierInfo]]] =
    dao.findAllSupplierInfoByProductIds(productIds).map { t =>
      t.groupBy {
        case (productId, _) => productId
      }.transform { (_, v) =>
        v.map {
          case (_, supplier) => supplier
        }
      }
    }

  def findByIds(ids: Seq[UUID])(implicit user: UserContext): Future[Seq[Entity]] =
    dao
      .findByIds(ids)
      .map(fromRecordsToEntities)
}
