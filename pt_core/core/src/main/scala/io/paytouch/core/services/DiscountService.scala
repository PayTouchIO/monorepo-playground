package io.paytouch.core.services

import java.util.UUID

import akka.actor.ActorRef
import io.paytouch.core.RichMap
import io.paytouch.core.async.trackers.EventTracker
import io.paytouch.core.conversions.DiscountConversions
import io.paytouch.core.data.daos.{ Daos, DiscountDao }
import io.paytouch.core.data.model.upsertions.{ DiscountUpsertion => DiscountUpsertionModel }
import io.paytouch.core.data.model.{ DiscountRecord, DiscountUpdate }
import io.paytouch.core.entities.enums.ExposedName
import io.paytouch.core.entities.{
  DiscountCreation,
  ItemLocation,
  UserContext,
  Discount => DiscountEntity,
  DiscountUpdate => DiscountUpdateEntity,
}
import io.paytouch.core.expansions.DiscountExpansions
import io.paytouch.core.filters.DiscountFilters
import io.paytouch.core.services.features._
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.DiscountValidator
import io.paytouch.core.{ withTag, Availabilities, LocationOverridesPer }

import scala.concurrent._

class DiscountService(
    val availabilityService: DiscountAvailabilityService,
    val eventTracker: ActorRef withTag EventTracker,
    val discountLocationService: DiscountLocationService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends DiscountConversions
       with CreateAndUpdateFeature
       with FindAllFeature
       with FindByIdFeature
       with UpdateActiveLocationsFeature
       with DeleteFeature {

  type Creation = DiscountCreation
  type Dao = DiscountDao
  type Entity = DiscountEntity
  type Expansions = DiscountExpansions
  type Filters = DiscountFilters
  type Model = DiscountUpsertionModel
  type Record = DiscountRecord
  type Update = DiscountUpdateEntity
  type Validator = DiscountValidator

  protected val dao = daos.discountDao
  protected val validator = new DiscountValidator

  val defaultFilters = DiscountFilters()

  val classShortName = ExposedName.Discount

  val itemLocationService = discountLocationService

  def enrich(discounts: Seq[Record], f: Filters)(e: Expansions)(implicit user: UserContext): Future[Seq[Entity]] = {
    val locationOverridesPerDiscountR =
      getOptionalLocationOverridesPerDiscount(discounts, f.locationIds)(e.withLocations)

    val availabilitiesPerDiscountR =
      getOptionalAvailabilitiesPerDiscount(discounts)(e.withAvailabilities)

    for {
      locationOverridesPerDiscount <- locationOverridesPerDiscountR
      availabilitiesPerDiscount <- availabilitiesPerDiscountR
    } yield fromRecordsAndOptionsToEntities(discounts, locationOverridesPerDiscount, availabilitiesPerDiscount)
  }

  private def getOptionalLocationOverridesPerDiscount(
      discounts: Seq[Record],
      locationIds: Option[Seq[UUID]],
    )(
      withLocations: Boolean,
    )(implicit
      user: UserContext,
    ): Future[Option[LocationOverridesPer[Record, ItemLocation]]] =
    if (withLocations) {
      val discountIds = discounts.map(_.id)
      itemLocationService.findAllByItemIdsAsMap(discountIds, locationIds = locationIds).map { recordsByItemIds =>
        Some(recordsByItemIds.mapKeysToRecords(discounts))
      }
    }
    else Future.successful(None)

  private def getOptionalAvailabilitiesPerDiscount(
      discounts: Seq[Record],
    )(
      withAvailabilities: Boolean,
    )(implicit
      user: UserContext,
    ): Future[DataByRecord[Availabilities]] =
    if (withAvailabilities) {
      val discountIds = discounts.map(_.id)
      availabilityService.findAllPerDiscount(discountIds).map { recordsByItemIds =>
        Some(recordsByItemIds.mapKeysToRecords(discounts))
      }
    }
    else Future.successful(None)

  protected def convertToUpsertionModel(
      id: UUID,
      update: Update,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Model]] =
    for {
      discount <- convertToDiscountUpdate(id, update)
      discountLocations <- itemLocationService.convertToDiscountLocationUpdates(id, update.locationOverrides)
      availabilities <- availabilityService.toDiscountAvailabilities(id, update.availabilityHours)
    } yield Multiple.combine(discount, discountLocations, availabilities)(DiscountUpsertionModel)

  private def convertToDiscountUpdate(
      discountId: UUID,
      upsertion: Update,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[DiscountUpdate]] =
    Future.successful(Multiple.success(fromUpsertionToUpdate(discountId, upsertion)))

}
