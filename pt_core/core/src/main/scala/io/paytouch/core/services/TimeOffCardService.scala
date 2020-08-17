package io.paytouch.core.services

import java.util.UUID

import scala.concurrent._

import akka.actor.ActorRef

import cats.implicits._

import io.paytouch.core._
import io.paytouch.core.async.trackers.EventTracker
import io.paytouch.core.conversions.TimeOffCardConversions
import io.paytouch.core.data._
import io.paytouch.core.data.daos._
import io.paytouch.core.data.model._
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.ExposedName
import io.paytouch.core.expansions.NoExpansions
import io.paytouch.core.filters.TimeOffCardFilters
import io.paytouch.core.services.features._
import io.paytouch.core.utils.ResultType
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.TimeOffCardValidator

class TimeOffCardService(
    val eventTracker: ActorRef withTag EventTracker,
    val userService: UserService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends TimeOffCardConversions
       with FindByIdFeature
       with FindAllFeature
       with CreateAndUpdateFeature
       with DeleteFeature {
  type Creation = TimeOffCardCreation
  type Dao = TimeOffCardDao
  type Entity = TimeOffCard
  type Expansions = NoExpansions
  type Filters = TimeOffCardFilters
  type Model = model.TimeOffCardUpdate
  type Record = TimeOffCardRecord
  type Update = entities.TimeOffCardUpdate
  type Validator = TimeOffCardValidator

  protected val dao = daos.timeOffCardDao
  protected val validator = new TimeOffCardValidator
  val defaultFilters = TimeOffCardFilters()

  val classShortName = ExposedName.TimeOffCard

  def enrich(
      records: Seq[Record],
      filters: Filters,
    )(
      expansions: Expansions,
    )(implicit
      user: UserContext,
    ): Future[Seq[Entity]] =
    userService
      .getUserInfoByIds(records.map(_.userId))
      .map(users => fromRecordsAndOptionsToEntities(records, groupByUserByTimeOffCard(users, records)))

  implicit def toFutureResultTypeEntity(
      f: Future[(ResultType, Record)],
    )(implicit
      user: UserContext,
    ): Future[(ResultType, Entity)] =
    for {
      (resultType, record) <- f
      enrichedRecord <- enrich(record, defaultFilters)(NoExpansions())
    } yield (resultType, enrichedRecord)

  protected def convertToUpsertionModel(
      id: UUID,
      update: Update,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Model]] =
    validator
      .validateUpsertion(id, update)
      .mapNested(_ => fromUpsertionToUpdate(id, update))
}
