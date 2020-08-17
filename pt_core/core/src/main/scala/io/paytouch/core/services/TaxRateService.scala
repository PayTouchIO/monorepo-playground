package io.paytouch.core.services

import java.util.UUID

import scala.concurrent._
import akka.actor.ActorRef
import cats.implicits._
import io.paytouch.core.{ withTag, LocationOverridesPer, TaxRatesPerLocation }
import io.paytouch.core.async.trackers.EventTracker
import io.paytouch.core.conversions.TaxRateConversions
import io.paytouch.core.data.daos._
import io.paytouch.core.data.model.{ LocationRecord, TaxRateRecord, TaxRateUpdate => TaxRateUpdateModel }
import io.paytouch.core.data.model.upsertions.{ TaxRateUpsertion => TaxRateUpsertionModel }
import io.paytouch.core.entities.{ TaxRate => TaxRateEntity, TaxRateUpdate => TaxRateUpdateEntity, _ }
import io.paytouch.core.entities.enums.{ ExposedName, MerchantSetupSteps }
import io.paytouch.core.expansions.TaxRateExpansions
import io.paytouch.core.filters.TaxRateFilters
import io.paytouch.core.services.features._
import io.paytouch.core.utils._
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.validators.TaxRateValidator

class TaxRateService(
    val eventTracker: ActorRef withTag EventTracker,
    val setupStepService: SetupStepService,
    val taxRateLocationService: TaxRateLocationService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends TaxRateConversions
       with CreateAndUpdateFeatureWithStateProcessing
       with DeleteFeature
       with FindAllFeature
       with UpdateActiveLocationsFeature {
  type Creation = TaxRateCreation
  type Dao = TaxRateDao
  type Entity = TaxRateEntity
  type Expansions = TaxRateExpansions
  type Filters = TaxRateFilters
  type Model = TaxRateUpsertionModel
  type Record = TaxRateRecord
  type State = Unit
  type Update = TaxRateUpdateEntity
  type Validator = TaxRateValidator

  protected val dao = daos.taxRateDao
  protected val validator = new TaxRateValidator

  val productLocationDao = daos.productLocationDao
  val productLocationTaxRateDao = daos.productLocationTaxRateDao

  val classShortName = ExposedName.TaxRate

  val itemLocationService = taxRateLocationService

  def findByIds(taxRateIds: Seq[UUID])(e: Expansions)(implicit user: UserContext): Future[Seq[Entity]] =
    dao.findByIds(taxRateIds).flatMap(taxRates => enrich(taxRates, TaxRateFilters())(e))

  def findByProductIds(
      productIds: Seq[UUID],
    )(
      e: Expansions,
    )(implicit
      user: UserContext,
    ): Future[Map[UUID, TaxRatesPerLocation]] =
    for {
      productLocations <- productLocationDao.findByItemIdsAndLocationIds(productIds, user.locationIds)
      productLocationTaxRates <- productLocationTaxRateDao.findByProductLocationIds(productLocations.map(_.id))
      taxRates <- findByIds(productLocationTaxRates.map(_.taxRateId))(e)
    } yield groupTaxRatesPerProduct(productLocations, productLocationTaxRates, taxRates)

  def findByLocationIds(
      locations: Seq[LocationRecord],
    )(implicit
      user: UserContext,
    ): Future[Map[LocationRecord, Seq[Entity]]] =
    for {
      taxRateLocations <- itemLocationService.findAllByLocationIds(locations.map(_.id))
      taxRates <- findByIds(taxRateLocations.map(_.taxRateId))(TaxRateExpansions(withLocations = true))
    } yield groupTaxRatesPerLocation(taxRates, taxRateLocations, locations)

  def enrich(
      taxRates: Seq[Record],
      filters: Filters,
    )(
      e: Expansions,
    )(implicit
      user: UserContext,
    ): Future[Seq[Entity]] =
    getOptionalLocationOverridesPerTaxRate(taxRates, filters.locationIds)(e.withLocations)
      .map(fromRecordsAndOptionsToEntities(taxRates, _))

  private def getOptionalLocationOverridesPerTaxRate(
      taxRates: Seq[Record],
      locationIds: Option[Seq[UUID]],
    )(
      withLocations: Boolean,
    )(implicit
      user: UserContext,
    ): Future[Option[LocationOverridesPer[Record, ItemLocation]]] =
    if (withLocations)
      itemLocationService
        .findAllByItemIdsAsMap(taxRates.map(_.id), locationIds = locationIds)
        .map(_.mapKeysToRecords(taxRates).some)
    else
      Future.successful(None)

  protected def convertToUpsertionModel(
      id: UUID,
      update: Update,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Model]] =
    for {
      taxRate <- convertToTaxRateUpdate(id, update)
      taxRateLocations <- itemLocationService.convertToItemLocationUpdates(id, update.locationOverrides)
    } yield Multiple.combine(taxRate, taxRateLocations)(TaxRateUpsertionModel)

  private def convertToTaxRateUpdate(
      taxRateId: UUID,
      update: Update,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[TaxRateUpdateModel]] =
    Future.successful {
      Multiple.success(fromUpsertionToUpdate(taxRateId, update))
    }

  protected def saveCreationState(id: UUID, creation: Creation)(implicit user: UserContext): Future[Option[State]] =
    Future.successful(None)

  protected def saveCurrentState(record: Record)(implicit user: UserContext): Future[State] =
    Future.unit

  protected def processChangeOfState(
      state: Option[State],
      update: Update,
      resultType: ResultType,
      entity: Entity,
    )(implicit
      user: UserContext,
    ) =
    setupStepService.checkStepCompletion(entity, MerchantSetupSteps.SetupTaxes)
}
