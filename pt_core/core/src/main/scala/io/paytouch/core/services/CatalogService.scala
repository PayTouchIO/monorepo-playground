package io.paytouch.core.services

import java.util.UUID

import akka.actor.ActorRef
import cats.implicits._

import scala.concurrent._

import io.paytouch.core.{ Availabilities, LocationOverridesPer }
import io.paytouch.core.async.trackers.EventTracker
import io.paytouch.core.clients.paytouch.ordering.PtOrderingClient
import io.paytouch.core.conversions.CatalogConversions
import io.paytouch.core.data.daos.{ CatalogDao, Daos }
import io.paytouch.core.data.model
import io.paytouch.core.data.model.upsertions.CatalogUpsertion
import io.paytouch.core.entities
import io.paytouch.core.entities.enums.{ CatalogType, ExposedName }
import io.paytouch.core.entities.UserContext
import io.paytouch.core.errors.UnexpectedMissingDefaultProductCatalog
import io.paytouch.core.expansions._
import io.paytouch.core.filters._
import io.paytouch.core.services.features._
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.CatalogValidator
import io.paytouch.core.withTag

class CatalogService(
    val availabilityService: CatalogAvailabilityService,
    val categoryService: CategoryService,
    val eventTracker: ActorRef withTag EventTracker,
    val locationService: LocationService,
    val productCategoryService: ProductCategoryService,
    val ptOrderingClient: PtOrderingClient,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends CatalogConversions
       with FindByIdFeature
       with FindAllFeature
       with CreateAndUpdateFeature
       with DeleteFeature {

  type Creation = entities.CatalogCreation
  type Dao = CatalogDao
  type Entity = entities.Catalog
  type Expansions = CatalogExpansions
  type Filters = CatalogFilters
  type Model = CatalogUpsertion
  type Record = model.CatalogRecord
  type Update = entities.CatalogUpsertion
  type Validator = CatalogValidator

  protected val dao = daos.catalogDao
  protected val validator = new CatalogValidator(ptOrderingClient)

  val classShortName = ExposedName.Catalog
  val defaultFilters = CatalogFilters()

  def enrich(
      records: Seq[Record],
      filters: Filters,
    )(
      expansions: Expansions,
    )(implicit
      user: UserContext,
    ): Future[Seq[Entity]] = {
    val categoriesCountPerCatalogR =
      getOptionalCategoriesCountPerCatalog(records)(expansions.withCategoriesCount)
    val productsCountPerCatalogR =
      getOptionalProductsCountPerCatalog(records)(expansions.withProductsCount)
    val availabilitiesDataPerCatalogR =
      getOptionalAvailabilitiesPerCatalog(records)(expansions.withAvailabilities || expansions.withLocationOverrides)

    for {
      categoriesCountPerCatalog <- categoriesCountPerCatalogR
      productsCountPerCatalog <- productsCountPerCatalogR
      availabilitiesDataPerCatalog <- availabilitiesDataPerCatalogR
      availabilitiesPerCatalog = if (expansions.withAvailabilities) availabilitiesDataPerCatalog else None
      locationOverridesPerCatalog <-
        getOptionalLocationOverridesPerCatalog(records, availabilitiesDataPerCatalog)(expansions.withLocationOverrides)
    } yield fromRecordsAndOptionsToEntities(
      records,
      productsCountPerCatalog,
      categoriesCountPerCatalog,
      availabilitiesPerCatalog,
      locationOverridesPerCatalog,
    )
  }

  private def getOptionalCategoriesCountPerCatalog(
      catalogs: Seq[Record],
    )(
      withCategoriesCount: Boolean,
    )(implicit
      user: UserContext,
    ): Future[Option[Map[Record, Int]]] =
    if (withCategoriesCount)
      categoryService
        .findByCatalogs(catalogs)
        .map(records => Some(records.transform((_, v) => v.size)))
    else
      Future.successful(None)

  private def getOptionalProductsCountPerCatalog(
      catalogs: Seq[Record],
    )(
      withProductsCount: Boolean,
    )(implicit
      user: UserContext,
    ): Future[Option[Map[Record, Int]]] =
    if (withProductsCount)
      dao
        .productsCountByCatalogs(catalogs.map(_.id))
        .map(Some(_))
    else
      Future.successful(None)

  private def getOptionalAvailabilitiesPerCatalog(
      catalogs: Seq[Record],
    )(
      withAvailabilities: Boolean,
    )(implicit
      user: UserContext,
    ): Future[Option[Map[UUID, Availabilities]]] =
    if (withAvailabilities)
      availabilityService
        .findAllByCatalogIds(catalogs.map(_.id))
        .map(Some(_))
    else
      Future.successful(None)

  private def getOptionalLocationOverridesPerCatalog(
      catalogs: Seq[Record],
      maybeAvailabilitiesPerCatalog: Option[Map[UUID, Availabilities]],
    )(
      withLocationOverrides: Boolean,
    )(implicit
      user: UserContext,
    ): Future[Option[LocationOverridesPer[UUID, entities.CatalogLocation]]] =
    if (withLocationOverrides) {
      implicit val pagination = entities.Pagination(page = 1, perPage = 100)
      val locationExpansions = LocationExpansions.empty.copy(withOpeningHours = true)
      val locationFilters = LocationFilters(user.locationIds, query = None, showAll = None)
      locationService.findAll(locationFilters)(locationExpansions).map {
        case (locations, _) =>
          val locationsAvailabilities: Map[UUID, entities.CatalogLocation] = locations.map { location =>
            location.id -> entities.CatalogLocation(availabilities = location.openingHours.getOrElse(Map.empty))
          }.toMap
          catalogs
            .map { catalog =>
              val catalogAvailiabities: Map[UUID, entities.CatalogLocation] = catalog.`type` match {
                case CatalogType.DefaultMenu => locationsAvailabilities
                case _ =>
                  val availabilities =
                    maybeAvailabilitiesPerCatalog.getOrElse(Map.empty).getOrElse(catalog.id, Map.empty)
                  locations.map(_.id -> entities.CatalogLocation(availabilities = availabilities)).toMap
              }
              catalog.id -> catalogAvailiabities
            }
            .toMap
            .some
      }
    }
    else
      Future.successful(None)

  def convertToUpsertionModel(id: UUID, update: Update)(implicit user: UserContext): Future[ErrorsOr[Model]] =
    for {
      catalog <- Future.successful(Multiple.success(fromUpsertionToUpdate(id, update)))
      availabilities <- availabilityService.toCatalogAvailabilities(id, update.availabilities)
    } yield Multiple.combine(catalog, availabilities)(CatalogUpsertion)

  def findDefaultMenuCatalog()(implicit user: UserContext): Future[ErrorsOr[Entity]] =
    dao.findByMerchantIdAndType(user.merchantId, CatalogType.DefaultMenu).flatMap {
      case Some(c) => enrich(c, defaultFilters)(CatalogExpansions.empty).map(Multiple.success)
      case _       => Future.successful(Multiple.failure(UnexpectedMissingDefaultProductCatalog(user.merchantId)))
    }

  def createDefaultMenu(implicit user: UserContext) =
    dao.upsert(
      model.CatalogUpdate(
        id = UUID.randomUUID.some,
        merchantId = user.merchantId.some,
        name = "Default Menu".some,
        `type` = CatalogType.DefaultMenu.some,
      ),
    )
}
