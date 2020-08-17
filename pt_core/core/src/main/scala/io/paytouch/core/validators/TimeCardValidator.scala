package io.paytouch.core.validators

import java.time.ZonedDateTime
import java.util.UUID

import cats.implicits._

import io.paytouch.core.data.daos.{ Daos, TimeCardDao }
import io.paytouch.core.data.model.{ ShiftRecord, TimeCardRecord, UserRecord }
import io.paytouch.core.entities.{ TimeCardClock, TimeCardUpdate, UserContext }
import io.paytouch.core.errors.{ InvalidFutureTime, InvalidTimeCardIds, NonAccessibleTimeCardIds }
import io.paytouch.core.utils.Multiple.ErrorsOr

import io.paytouch.core.utils.{ Multiple, Sha1EncryptionSupport, UtcTime }
import io.paytouch.core.validators.features.DefaultValidator

import scala.concurrent._

class TimeCardValidator(implicit val ec: ExecutionContext, val daos: Daos) extends DefaultValidator[TimeCardRecord] {

  type Record = TimeCardRecord
  type Dao = TimeCardDao

  protected val dao = daos.timeCardDao
  val validationErrorF = InvalidTimeCardIds(_)
  val accessErrorF = NonAccessibleTimeCardIds(_)

  val userValidator = new UserValidator
  val userLocationValidator = new UserLocationValidator
  val shiftValidator = new ShiftValidator

  override def validityCheck(record: TimeCardRecord)(implicit user: UserContext): Boolean =
    record.merchantId == user.merchantId && user.locationIds.contains(record.locationId)

  def validateUpsertion(
      id: UUID,
      update: TimeCardUpdate,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[(Option[TimeCardRecord], Option[ShiftRecord])]] =
    dao.findById(id).flatMap { record =>
      val userId = update.userId.orElse(record.map(_.userId)).getOrElse(UUID.randomUUID)
      val locationId = update.locationId.orElse(record.map(_.locationId)).getOrElse(UUID.randomUUID)
      val maybeExistingShiftId = record.flatMap(_.shiftId)
      for {
        validUserLocation <- userLocationValidator.accessUserLocationAsLoggedUser(userId, locationId)
        validStartAt <- validateDatetime(update.startAt)
        validEndAt <- validateDatetime(update.endAt)
        validShift <- shiftValidator.accessOneByOptId(update.shiftId.orElse(maybeExistingShiftId))
      } yield Multiple.combine(validUserLocation, validStartAt, validEndAt, validShift) {
        case (_, _, _, shift) => (record, shift)
      }
    }

  def validateClock(
      clockData: TimeCardClock,
    )(implicit
      userContext: UserContext,
    ): Future[ErrorsOr[(UserRecord, Option[TimeCardRecord])]] =
    userValidator.validateByPinAndLocation(clockData.pin, clockData.locationId).flatMapTraverse { user =>
      dao.findOpenTimeCardByUserIdAndLocationId(user.id, clockData.locationId).map { maybeTimeCard =>
        (user, maybeTimeCard)
      }
    }

  private def validateDatetime(maybeDatetime: Option[ZonedDateTime]) =
    Future.successful {
      maybeDatetime match {
        case Some(datetime) =>
          if (datetime.isAfter(UtcTime.now.plusMinutes(5))) Multiple.failure(InvalidFutureTime(datetime))
          else Multiple.successOpt(datetime)
        case None => Multiple.empty
      }
    }
}
