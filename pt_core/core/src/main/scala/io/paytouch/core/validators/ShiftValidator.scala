package io.paytouch.core.validators

import java.util.UUID

import scala.concurrent._

import cats.implicits._

import io.paytouch.core.data.daos.{ Daos, ShiftDao }
import io.paytouch.core.data.model.ShiftRecord
import io.paytouch.core.entities.{ ShiftUpdate, UserContext }
import io.paytouch.core.errors.{ InvalidShiftIds, NonAccessibleShiftIds }
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.validators.features.DefaultValidatorWithLocation

class ShiftValidator(implicit val ec: ExecutionContext, val daos: Daos)
    extends DefaultValidatorWithLocation[ShiftRecord] {
  type Record = ShiftRecord
  type Dao = ShiftDao

  protected val dao = daos.shiftDao
  val validationErrorF = InvalidShiftIds(_)
  val accessErrorF = NonAccessibleShiftIds(_)

  val userLocationValidator = new UserLocationValidator

  def validateUpsertion(id: UUID, update: ShiftUpdate)(implicit user: UserContext): Future[ErrorsOr[Unit]] =
    dao.findById(id).flatMap { record =>
      val userId = update.userId.orElse(record.map(_.userId)).getOrElse(UUID.randomUUID)
      val locationId = update.locationId.orElse(record.map(_.locationId)).getOrElse(UUID.randomUUID)
      userLocationValidator
        .accessUserLocationAsLoggedUser(userId, locationId)
        .asNested(Future.successful(()))
    }
}
