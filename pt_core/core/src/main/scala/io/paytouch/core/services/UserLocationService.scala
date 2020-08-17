package io.paytouch.core.services

import java.util.UUID

import cats.implicits._

import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.{ UserLocationUpdate => UserLocationUpdateModel }
import io.paytouch.core.entities.UserContext
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.LocationValidator

import scala.concurrent._

class UserLocationService(implicit val ec: ExecutionContext, val daos: Daos) {

  protected val dao = daos.userLocationDao

  val locationValidator = new LocationValidator
  val locationDao = daos.locationDao

  def convertToUserLocationUpdates(
      userId: UUID,
      locationIds: Option[Seq[UUID]],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Seq[UserLocationUpdateModel]]] =
    locationIds match {
      case Some(locIds) => convertToUserLocationUpdates(userId, locIds)
      case _            => Future.successful(Multiple.success(Seq.empty))
    }

  def convertToUserLocationUpdates(
      userId: UUID,
      locationIds: Seq[UUID],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Seq[UserLocationUpdateModel]]] =
    for {
      locations <- locationValidator.validateBelongsToMerchant(locationIds)
      userLocations <- dao.findByItemIdsAndLocationIds(Seq(userId), locationIds).map(Multiple.success)
    } yield Multiple.combine(locations, userLocations) {
      case _ => locationIds.map(locationId => toUserLocationUpdate(userId = userId, locationId = locationId))
    }

  def convertToUserLocationUpdates(
      itemId: UUID,
      locationOverrides: Map[UUID, Boolean],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Map[UUID, Option[UserLocationUpdateModel]]]] = {
    val locationIds = locationOverrides.keys.toSeq
    locationValidator.validateByIds(locationIds).mapNested { _ =>
      locationOverrides.map {
        case (locationId, true)  => locationId -> Some(toUserLocationUpdate(itemId, locationId))
        case (locationId, false) => locationId -> None
      }
    }
  }

  def toUserLocationUpdate(userId: UUID, locationId: UUID)(implicit user: UserContext): UserLocationUpdateModel =
    UserLocationUpdateModel(
      id = None,
      merchantId = Some(user.merchantId),
      locationId = Some(locationId),
      userId = Some(userId),
    )
}
