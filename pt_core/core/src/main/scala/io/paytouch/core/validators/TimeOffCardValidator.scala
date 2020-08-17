package io.paytouch.core.validators

import java.util.UUID

import scala.concurrent._

import cats.implicits._

import io.paytouch.core.data.daos.{ Daos, TimeOffCardDao }
import io.paytouch.core.data.model.{ TimeOffCardRecord, UserLocationRecord }
import io.paytouch.core.entities.{ TimeOffCardUpdate, UserContext }
import io.paytouch.core.errors.{ InvalidTimeOffCardIds, NonAccessibleTimeOffCardIds }
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.validators.features.{ DeletionValidator, ValidatorWithExtraFields }

class TimeOffCardValidator(implicit val ec: ExecutionContext, val daos: Daos)
    extends ValidatorWithExtraFields[TimeOffCardRecord]
       with DeletionValidator[TimeOffCardRecord] {
  type Extra = UserLocationRecord
  type Dao = TimeOffCardDao

  protected val dao = daos.timeOffCardDao
  val validationErrorF = InvalidTimeOffCardIds(_)
  val accessErrorF = NonAccessibleTimeOffCardIds(_)

  val userValidator = new UserValidator

  protected def recordsFinder(ids: Seq[UUID])(implicit user: UserContext): Future[Seq[TimeOffCardRecord]] =
    dao.findByIds(ids)

  protected def extraRecordsFinder(ids: Seq[UUID])(implicit user: UserContext): Future[Seq[UserLocationRecord]] =
    daos.userLocationDao.findByTimeOffCardIds(ids)

  protected def validityCheckWithExtraRecords(
      record: TimeOffCardRecord,
      extraRecords: Seq[UserLocationRecord],
    )(implicit
      user: UserContext,
    ): Boolean = {
    val locationIdsForCardUser = extraRecords.filter(r => r.userId == record.userId).map(_.locationId)
    val commonLocations = user.locationIds intersect locationIdsForCardUser

    record.merchantId == user.merchantId && commonLocations.nonEmpty
  }

  def validateUpsertion(
      id: UUID,
      update: TimeOffCardUpdate,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Unit]] =
    dao.findById(id).flatMap { record =>
      val userId = update.userId.orElse(record.map(_.userId)).getOrElse(UUID.randomUUID)

      userValidator.accessOneById(userId).asNested(Future.successful(()))
    }
}
