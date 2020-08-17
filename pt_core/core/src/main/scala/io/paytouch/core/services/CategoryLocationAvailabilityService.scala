package io.paytouch.core.services

import java.util.UUID

import io.paytouch.core.AvailabilitiesPerItemId
import io.paytouch.core.conversions.CategoryLocationAvailabilityConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.{ AvailabilityUpdate, CategoryLocationUpdate => CategoryLocationUpdateModel }
import io.paytouch.core.entities.{ CategoryLocationUpdate => CategoryLocationUpdateEntity, _ }
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr

import scala.concurrent._

class CategoryLocationAvailabilityService(
    val categoryLocationService: CategoryLocationService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends CategoryLocationAvailabilityConversions {

  protected val dao = daos.categoryLocationAvailabilityDao

  def findAllPerCategory(
      categoryIds: Seq[UUID],
      locationId: Option[UUID],
    )(implicit
      user: UserContext,
    ): Future[Map[UUID, AvailabilitiesPerItemId]] =
    for {
      categoryLocations <- categoryLocationService.findByCategoryIds(categoryIds, locationId)
      availabilities <- dao.findByItemIds(categoryLocations.map(_.id))
    } yield groupAvailabilitiesPerCategory(categoryLocations, availabilities)

  def toCategoryLocationAvailabilities(
      categoryLocations: Map[UUID, Option[CategoryLocationUpdateModel]],
      locationOverrides: Map[UUID, Option[CategoryLocationUpdateEntity]],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Seq[AvailabilityUpdate]]] =
    Future.successful {
      val categoryLocationUpdates: Seq[CategoryLocationUpdateModel] = categoryLocations.values.flatten.toSeq
      val availabilityUpdates = (for {
        categoryLocationUpdate <- categoryLocationUpdates
        locationId <- categoryLocationUpdate.locationId
        locOverride <- locationOverrides.get(locationId)
        locOver <- locOverride
      } yield toAvailabilities(categoryLocationUpdate.id, locOver.availabilities)).flatten
      Multiple.success(availabilityUpdates)
    }
}
