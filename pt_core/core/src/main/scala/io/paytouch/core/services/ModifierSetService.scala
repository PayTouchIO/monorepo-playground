package io.paytouch.core.services

import java.util.UUID

import scala.concurrent._

import akka.actor.ActorRef

import cats.data._
import cats.implicits._

import io.paytouch._

import io.paytouch.core.{ withTag, LocationOverridesPer }
import io.paytouch.core.async.trackers.EventTracker
import io.paytouch.core.conversions.ModifierSetConversions
import io.paytouch.core.data.daos._
import io.paytouch.core.data.model.{ ModifierSetRecord, ModifierSetUpdate }
import io.paytouch.core.data.model.upsertions.{ ModifierSetUpsertion => ModifierSetUpsertionModel }
import io.paytouch.core.entities.{ ModifierSet => ModifierSetEntity, ModifierSetUpdate => ModifierSetUpdateEntity, _ }
import io.paytouch.core.entities.enums.ExposedName
import io.paytouch.core.errors._
import io.paytouch.core.expansions.ModifierSetExpansions
import io.paytouch.core.filters.ModifierSetFilters
import io.paytouch.core.RichMap
import io.paytouch.core.services.features._
import io.paytouch.core.utils.ResultType
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.ModifierSetValidator

class ModifierSetService(
    val eventTracker: ActorRef withTag EventTracker,
    val locationService: LocationService,
    val modifierSetLocationService: ModifierSetLocationService,
    val modifierOptionService: ModifierOptionService,
    val modifierSetProductService: ModifierSetProductService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends ModifierSetConversions
       with UpdateActiveLocationsFeature
       with DeleteFeature
       with CreateAndUpdateFeature
       with FindAllFeature
       with FindByIdFeature {
  type Creation = ModifierSetCreation
  type Dao = ModifierSetDao
  type Entity = ModifierSetEntity
  type Expansions = ModifierSetExpansions
  type Filters = ModifierSetFilters
  type Model = ModifierSetUpsertionModel
  type Record = ModifierSetRecord
  type Update = ModifierSetUpdateEntity
  type Validator = ModifierSetValidator

  protected val dao = daos.modifierSetDao
  protected val validator = new ModifierSetValidator
  val defaultFilters = ModifierSetFilters()

  val classShortName = ExposedName.ModifierSet

  val itemLocationService = modifierSetLocationService

  final override protected def convertToUpsertionModel(
      id: UUID,
      upsertion: Update,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Model]] =
    for {
      modifierSet <- convertToModifierSetUpdate(id, upsertion).toValidated.pure[Future]
      modifierSetLocations <- itemLocationService.convertToModifierSetLocationUpdates(id, upsertion.locationOverrides)
      modifierOptions <-
        modifierOptionService.convertToModifierOptionUpdates(id, upsertion.options.map(_.map(_.toModifierOption)))
    } yield Multiple.combine(modifierSet, modifierSetLocations, modifierOptions)(ModifierSetUpsertionModel)

  def findByProductIds(productIds: Seq[UUID])(implicit user: UserContext): Future[Map[UUID, Seq[Entity]]] =
    for {
      modifierSetProducts <- modifierSetProductService.findByProductIds(productIds)
      modifierSets <- dao.findByIds(modifierSetProducts.map(_.modifierSetId))
      enrichedModifierSets <- enrich(modifierSets, defaultFilters)(
        ModifierSetExpansions(withProductsCount = false, withLocations = true),
      )
    } yield groupModifierSetsPerProduct(modifierSetProducts, enrichedModifierSets)

  def assignProducts(
      modifierSetId: UUID,
      assignment: ModifierSetProductsAssignment,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Unit]] =
    validator.accessOneById(modifierSetId).flatMap {
      case Validated.Valid(_) =>
        modifierSetProductService
          .associateProductsToModifierSet(modifierSetId, assignment.productIds)

      case i @ Validated.Invalid(_) =>
        i.pure[Future]
    }

  override def enrich(
      modifierSets: Seq[Record],
      filters: Filters,
    )(
      e: Expansions,
    )(implicit
      user: UserContext,
    ): Future[Seq[Entity]] =
    for {
      modifierOptions <- getModifierOptionsPerModifierSet(modifierSets)
      countsPerModifierSet <- getOptionalProductsCountPerModifierSet(modifierSets)(e.withProductsCount)
      locationOverridesPerModifierSet <- getOptionalLocationOverridesPerModifierSet(modifierSets, filters.locationIds)(
        e.withLocations,
      )
    } yield fromRecordsAndOptionsToEntities(
      modifierSets,
      modifierOptions,
      countsPerModifierSet,
      locationOverridesPerModifierSet,
    )

  override implicit def toFutureResultTypeEntity(
      f: Future[(ResultType, Record)],
    )(implicit
      user: UserContext,
    ): Future[(ResultType, Entity)] =
    for {
      (resultType, record) <- f
      enrichedRecord <- enrich(record, defaultFilters)(ModifierSetExpansions(true, true))
    } yield (resultType, enrichedRecord)

  private def getModifierOptionsPerModifierSet(
      modifierSets: Seq[Record],
    )(implicit
      user: UserContext,
    ): Future[DataSeqByRecord[ModifierOption]] =
    modifierOptionService
      .findByModifierSets(modifierSets)
      .map(_.mapKeysToRecords(modifierSets).some)

  private def getOptionalProductsCountPerModifierSet(
      modifierSets: Seq[Record],
    )(
      withProductsCount: Boolean,
    ): Future[DataByRecord[Int]] =
    if (withProductsCount)
      modifierSetProductService
        .countByModifierSetIds(modifierSets.map(_.id))
        .map(_.mapKeysToRecords(modifierSets).some)
    else
      Future.successful(None)

  private def getOptionalLocationOverridesPerModifierSet(
      modifierSets: Seq[Record],
      locationIds: Option[Seq[UUID]],
    )(
      withLocations: Boolean,
    )(implicit
      user: UserContext,
    ): Future[Option[LocationOverridesPer[Record, ItemLocation]]] =
    if (withLocations)
      itemLocationService
        .findAllByItemIdsAsMap(modifierSets.map(_.id), locationIds = locationIds)
        .map(_.mapKeysToRecords(modifierSets).some)
    else
      Future.successful(None)

  private def convertToModifierSetUpdate(
      id: UUID,
      update: Update,
    )(implicit
      user: UserContext,
    ): EitherNel[Error, ModifierSetUpdate] =
    for {
      optionCount <- validator.validateModifierOptionLimits(update)
      maxSingleOptionCount <- validator.validateMaximumSingleOptionCount(optionCount, update.maximumSingleOptionCount)
    } yield fromUpsertionToUpdateWithProperTypes(
      id,
      update.copy(maximumSingleOptionCount = maxSingleOptionCount.some),
      optionCount,
    )
}
