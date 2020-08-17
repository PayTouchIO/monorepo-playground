package io.paytouch.core.services

import java.util.UUID

import akka.actor.ActorRef

import cats.implicits._

import cats.data.Validated.{ Invalid, Valid }
import io.paytouch.core.async.trackers.EventTracker
import io.paytouch.core.conversions.TimeCardConversions
import io.paytouch.core.data.daos.{ Daos, TimeCardDao }
import io.paytouch.core.data.model.{ ShiftRecord, TimeCardRecord, TimeCardUpdate => TimeCardUpdateModel }
import io.paytouch.core.entities.enums.ExposedName
import io.paytouch.core.entities.{
  Shift,
  TimeCardClock,
  TimeCardCreation,
  UserContext,
  TimeCard => TimeCardEntity,
  TimeCardUpdate => TimeCardUpdateEntity,
}
import io.paytouch.core.expansions.TimeCardExpansions
import io.paytouch.core.filters.TimeCardFilters
import io.paytouch.core.services.features._
import io.paytouch.core.utils.ResultType
import io.paytouch.core.utils._
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.validators.TimeCardValidator
import io.paytouch.core.withTag

import scala.concurrent._

class TimeCardService(
    val eventTracker: ActorRef withTag EventTracker,
    val locationService: LocationService,
    val shiftService: ShiftService,
    val userService: UserService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends TimeCardConversions
       with FindAllFeature
       with FindByIdFeature
       with CreateAndUpdateFeature
       with DeleteWithAccessibleLocationFeature {

  type Creation = TimeCardCreation
  type Dao = TimeCardDao
  type Entity = TimeCardEntity
  type Expansions = TimeCardExpansions
  type Filters = TimeCardFilters
  type Model = TimeCardUpdateModel
  type Record = TimeCardRecord
  type Update = TimeCardUpdateEntity
  type Validator = TimeCardValidator

  protected val dao = daos.timeCardDao
  val defaultFilters = TimeCardFilters()
  protected val validator = new TimeCardValidator
  val classShortName = ExposedName.TimeCard

  def enrich(records: Seq[Record], f: Filters)(e: Expansions)(implicit user: UserContext): Future[Seq[Entity]] = {
    val usersR = userService.getUserInfoByIds(records.map(_.userId))
    val locationsR = locationService.findByIds(records.map(_.locationId))
    val shiftsR = getOptionalShifts(records)(e.withShift)
    for {
      users <- usersR
      locations <- locationsR
      shifts <- shiftsR
    } yield fromRecordsToEntities(records, users, locations, shifts)
  }

  def getOptionalShifts(
      items: Seq[Record],
    )(
      withShifts: Boolean,
    )(implicit
      user: UserContext,
    ): Future[Option[Seq[Shift]]] =
    if (withShifts) shiftService.findByIds(items.flatMap(_.shiftId)).map(Some(_))
    else Future.successful(None)

  implicit def toFutureResultTypeEntity(
      f: Future[(ResultType, Record)],
    )(implicit
      user: UserContext,
    ): Future[(ResultType, Entity)] =
    f.flatMap {
      case (result, record) =>
        val expansions = TimeCardExpansions(false)
        enrich(Seq(record), defaultFilters)(expansions).map(entities => (result, entities.head))
    }

  def convertToUpsertionModel(id: UUID, update: Update)(implicit user: UserContext): Future[ErrorsOr[Model]] =
    validator.validateUpsertion(id, update).flatMapTraverse {
      case (existingRecord, associatedShift) =>
        detectShiftForTimeCard(update, existingRecord, associatedShift).map { detectedShift =>
          fromUpsertionToUpdate(id, update, detectedShift)
        }
    }

  private def detectShiftForTimeCard(
      update: Update,
      existingRecord: Option[Record],
      associatedShift: Option[ShiftRecord],
    )(implicit
      user: UserContext,
    ): Future[Option[ShiftRecord]] =
    if (associatedShift.isDefined) Future.successful(associatedShift)
    else {
      val userId = update.userId.orElse(existingRecord.map(_.userId)).getOrElse(UUID.randomUUID)
      val locationId = update.locationId.orElse(existingRecord.map(_.locationId)).getOrElse(UUID.randomUUID)
      val startTime = update.startAt.orElse(existingRecord.flatMap(_.startAt))
      val endTime = update.endAt.orElse(existingRecord.flatMap(_.endAt))
      shiftService.detectShiftToAssociate(
        userId = userId,
        locationId = locationId,
        startDateTime = startTime,
        endDateTime = endTime,
      )
    }

  def clock(
      clockData: TimeCardClock,
    )(implicit
      userContext: UserContext,
    ): Future[ErrorsOr[Result[TimeCardEntity]]] =
    validator.validateClock(clockData).flatMap {
      case Valid((user, maybeTimeCard)) =>
        val timeCardId = maybeTimeCard.fold(UUID.randomUUID)(_.id)
        val updateEntity =
          maybeTimeCard.fold(toOpenTimeCardUpdate(user.id, clockData.locationId))(toCloseTimeCardUpdate)
        convertAndUpsert(timeCardId, updateEntity)
      case i @ Invalid(_) => Future.successful(i)
    }
}
