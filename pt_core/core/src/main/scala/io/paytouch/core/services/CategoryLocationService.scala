package io.paytouch.core.services

import java.util.UUID

import io.paytouch.core.{ AvailabilitiesPerItemId, LocationOverridesPer }
import io.paytouch.core.clients.paytouch.ordering.PtOrderingClient
import io.paytouch.core.conversions.CategoryLocationConversions
import io.paytouch.core.data.daos.{ CategoryLocationDao, Daos }
import io.paytouch.core.data.model.{ CategoryLocationRecord, CategoryLocationUpdate => CategoryLocationUpdateModel }
import io.paytouch.core.entities.{ CategoryLocationUpdate => CategoryLocationUpdateEntity, _ }
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.CategoryValidator

import scala.concurrent._

class CategoryLocationService(
    categoryLocationAvailabilityService: => CategoryLocationAvailabilityService,
    val ptOrderingClient: PtOrderingClient,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends CategoryLocationConversions
       with ItemLocationService {

  type Dao = CategoryLocationDao
  type Entity = CategoryLocation
  type Record = CategoryLocationRecord

  protected val dao = daos.categoryLocationDao

  val categoryValidator = new CategoryValidator(ptOrderingClient)

  def accessItemById(id: UUID)(implicit user: UserContext) = categoryValidator.accessOneById(id)

  def convertToItemLocationUpdates(
      itemId: UUID,
      locationOverrides: Map[UUID, Option[CategoryLocationUpdateEntity]],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Map[UUID, Option[CategoryLocationUpdateModel]]]] = {
    val locationIds = locationOverrides.keys.toSeq
    for {
      locations <- locationValidator.validateByIds(locationIds)
      itemLocations <- dao.findByItemIdsAndLocationIds(Seq(itemId), locationIds).map(Multiple.success)
    } yield Multiple.combine(locations, itemLocations) {
      case (_, itemLocs) =>
        locationOverrides.map {
          case (locationId, itemLocationUpdate) =>
            val categoryLocationUpdate = itemLocationUpdate.map(itemLocUpd =>
              itemLocs
                .find(_.locationId == locationId)
                .map(toCategoryLocationUpdate)
                .getOrElse(toCategoryLocationUpdate(itemId, locationId))
                .copy(active = itemLocUpd.active),
            )
            (locationId, categoryLocationUpdate)
        }
    }
  }

  def findByCategoryIds(
      categoryIds: Seq[UUID],
      locationId: Option[UUID] = None,
    )(implicit
      user: UserContext,
    ): Future[Seq[Record]] =
    dao.findByItemIdsAndLocationIds(categoryIds, user.accessibleLocations(locationId))

  def findAllByItemIdsAsMap(
      itemIds: Seq[UUID],
      locationId: Option[UUID],
    )(
      withAvailabilities: Boolean,
    )(implicit
      user: UserContext,
    ): Future[LocationOverridesPer[UUID, CategoryLocation]] =
    for {
      itemLocations <- dao.findByItemIdsAndLocationIds(itemIds, user.accessibleLocations(locationId))
      availabilities <- getOptionalLocationAvailabilitiesPerCategory(itemIds, locationId)(withAvailabilities)
    } yield groupLocationsPerItem(itemLocations, availabilities)

  private def groupLocationsPerItem(
      itemLocations: Seq[Record],
      availabilities: Option[Map[UUID, AvailabilitiesPerItemId]],
    ): LocationOverridesPer[UUID, CategoryLocation] =
    itemLocations.groupBy(_.itemId).map {
      case (itemId, itemsLocs) =>
        itemId -> fromItemLocationsToCategoryLocations(
          itemsLocs,
          availabilities.flatMap(_.get(itemId)).getOrElse(Map.empty),
        ).toMap
    }

  private def getOptionalLocationAvailabilitiesPerCategory(
      categoryIds: Seq[UUID],
      locationId: Option[UUID],
    )(
      withAvailabilities: Boolean,
    )(implicit
      user: UserContext,
    ): Future[Option[Map[UUID, AvailabilitiesPerItemId]]] =
    if (withAvailabilities)
      categoryLocationAvailabilityService.findAllPerCategory(categoryIds, locationId).map(Some(_))
    else Future.successful(None)

}
