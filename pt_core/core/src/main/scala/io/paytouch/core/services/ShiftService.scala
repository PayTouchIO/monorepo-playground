package io.paytouch.core.services

import java.time.{ DayOfWeek, ZoneId, ZonedDateTime }
import java.util.UUID

import akka.actor.ActorRef

import cats.implicits._

import io.paytouch.core.{ RichLocalTime, RichZoneDateTime }
import io.paytouch.core.async.trackers.EventTracker
import io.paytouch.core.conversions.ShiftConversions
import io.paytouch.core.data.daos.{ Daos, ShiftDao }
import io.paytouch.core.data.model.{ ShiftRecord, ShiftUpdate => ShiftUpdateModel }
import io.paytouch.core.entities.enums.{ ExposedName, MerchantSetupSteps }
import io.paytouch.core.entities.{
  Location,
  ShiftCreation,
  UserContext,
  UserInfo,
  Shift => ShiftEntity,
  ShiftUpdate => ShiftUpdateEntity,
}
import io.paytouch.core.expansions.ShiftExpansions
import io.paytouch.core.filters.ShiftFilters
import io.paytouch.core.services.features._
import io.paytouch.core.utils._
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.ShiftValidator
import io.paytouch.core.withTag

import scala.concurrent._

class ShiftService(
    val eventTracker: ActorRef withTag EventTracker,
    val locationService: LocationService,
    val setupStepService: SetupStepService,
    val userService: UserService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends ShiftConversions
       with FindByIdFeature
       with FindAllFeature
       with CreateAndUpdateFeatureWithStateProcessing
       with InsertOrUpdateFeature
       with DeleteWithAccessibleLocationFeature {

  type Creation = ShiftCreation
  type Dao = ShiftDao
  type Entity = ShiftEntity
  type Expansions = ShiftExpansions
  type Filters = ShiftFilters
  type Model = ShiftUpdateModel
  type Record = ShiftRecord
  type State = Unit
  type Update = ShiftUpdateEntity
  type Validator = ShiftValidator

  protected val dao = daos.shiftDao
  protected val validator = new ShiftValidator
  val defaultFilters = ShiftFilters()
  val classShortName = ExposedName.Shift

  def findByIds(ids: Seq[UUID])(implicit user: UserContext): Future[Seq[Entity]] =
    dao
      .findByIdsAndMerchantId(ids, user.merchantId)
      .flatMap(records => enrich(records, defaultFilters)(ShiftExpansions()))

  def enrich(
      records: Seq[ShiftRecord],
      filters: ShiftFilters,
    )(
      expansions: ShiftExpansions,
    )(implicit
      user: UserContext,
    ): Future[Seq[ShiftEntity]] = {
    val userPerShiftR =
      getRelatedField[UserInfo](
        userService.getUserInfoByIds,
        _.id,
        _.userId,
        records,
      )

    val locationPerShiftR =
      getExpandedField[Location](
        locationService.findByIds,
        _.id,
        _.locationId,
        records,
        expansions.withLocations,
      )

    for {
      userPerShift <- userPerShiftR
      locationPerShift <- locationPerShiftR
    } yield fromRecordsAndOptionsToEntities(records, userPerShift, locationPerShift)
  }

  implicit def toFutureResultTypeEntity(
      f: Future[(ResultType, Record)],
    )(implicit
      user: UserContext,
    ): Future[(ResultType, Entity)] =
    for {
      (resultType, record) <- f
      enrichedRecord <- enrich(record, defaultFilters)(ShiftExpansions())
    } yield (resultType, enrichedRecord)

  protected def convertToUpsertionModel(
      id: UUID,
      update: Update,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Model]] =
    validator.validateUpsertion(id, update).mapNested(_ => fromUpsertionToUpdate(id, update))

  def detectShiftToAssociate(
      userId: UUID,
      locationId: UUID,
      startDateTime: Option[ZonedDateTime],
      endDateTime: Option[ZonedDateTime],
    )(implicit
      user: UserContext,
    ): Future[Option[ShiftRecord]] =
    startDateTime match {
      case None => Future.successful(None)
      case Some(startDate) =>
        implicit val merchantCtx = user.toMerchantContext
        for {
          locationTimezone <- locationService.findTimezoneForLocationWithFallback(Some(locationId))
          startDateInLocationTimezone = startDate.toLocationTimezone(locationTimezone).toLocalDate
          shifts <-
            dao
              .findByUserIdLocationIdAndStartDate(
                userId = userId,
                locationId = locationId,
                startDate = startDateInLocationTimezone,
              )
        } yield findBestMatch(startDate, endDateTime, shifts, locationTimezone)
    }

  private def findBestMatch(
      startDateTime: ZonedDateTime,
      maybeEndDateTime: Option[ZonedDateTime],
      records: Seq[ShiftRecord],
      locationTimezone: ZoneId,
    ): Option[ShiftRecord] = {
    val startTime = startDateTime.withZoneSameInstant(locationTimezone).toLocalTime
    val maybeEndTime = maybeEndDateTime.map(_.withZoneSameInstant(locationTimezone).toLocalTime)

    val weekDays =
      Set(startDateTime.getDayOfWeek) ++ maybeEndDateTime.fold(Set.empty[DayOfWeek])(t => Set(t.getDayOfWeek))

    def timeMatches(shift: ShiftRecord): Boolean =
      startTime.isBetween(shift.startTime, shift.endTime) ||
        maybeEndTime.fold(false)(_.isBetween(shift.startTime, shift.endTime)) ||
        startTime.isAbout(shift.startTime) ||
        maybeEndTime.fold(false)(_.isAbout(shift.endTime)) ||
        maybeEndTime.fold(false)(endTime => shift.isContainedBy(startTime, endTime))

    def dayMatches(shift: ShiftRecord): Boolean = !shift.repeat || shift.covers(weekDays)

    records.find(r => dayMatches(r) && timeMatches(r))
  }

  protected def saveCreationState(id: UUID, creation: Creation)(implicit user: UserContext): Future[Option[State]] =
    Future.successful(None)

  protected def saveCurrentState(record: Record)(implicit user: UserContext): Future[State] =
    Future.unit

  protected def processChangeOfState(
      state: Option[State],
      update: Update,
      resultType: ResultType,
      entity: Entity,
    )(implicit
      user: UserContext,
    ) =
    setupStepService.simpleCheckStepCompletion(user.merchantId, MerchantSetupSteps.ScheduleEmployees)
}
