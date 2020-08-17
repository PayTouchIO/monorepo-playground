package io.paytouch.core.validators

import java.util.UUID

import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.UserLocationRecord
import io.paytouch.core.entities.UserContext
import io.paytouch.core.errors.{ NonAccessibleLocationIds, NonAccessibleUserLocationIds }
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple._

import scala.concurrent._

class UserLocationValidator(implicit val ec: ExecutionContext, val daos: Daos) {

  protected val dao = daos.userLocationDao

  def accessUserLocationAsLoggedUser(
      userId: UUID,
      locationId: UUID,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[UserLocationRecord]] =
    for {
      loggedUserCanAccess <- canLoggedUserAccessLocation(locationId)
      userLocation <- accessUserLocation(userId = userId, locationId = locationId)
    } yield Multiple.combine(loggedUserCanAccess, userLocation) { case (_, userLoc) => userLoc }

  private def canLoggedUserAccessLocation(locationId: UUID)(implicit user: UserContext): Future[ErrorsOr[Unit]] =
    Future.successful {
      if (user.locationIds.contains(locationId)) Multiple.success((): Unit)
      else Multiple.failure(NonAccessibleLocationIds(Seq(locationId)))
    }

  private def accessUserLocation(userId: UUID, locationId: UUID): Future[ErrorsOr[UserLocationRecord]] =
    dao.findOneByItemIdAndLocationId(userId, locationId).map {
      case Some(userLocation) => Multiple.success(userLocation)
      case None               => Multiple.failure(NonAccessibleUserLocationIds(userId = userId, locationId = locationId))
    }
}
