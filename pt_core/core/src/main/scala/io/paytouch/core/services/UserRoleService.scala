package io.paytouch.core.services

import java.util.UUID

import scala.concurrent._

import akka.actor.ActorRef

import cats.implicits._

import io.paytouch.core._
import io.paytouch.core.async.trackers.EventTracker
import io.paytouch.core.conversions.UserRoleConversions
import io.paytouch.core.data._
import io.paytouch.core.data.daos.{ Daos, UserRoleDao }
import io.paytouch.core.entities.enums.ExposedName
import io.paytouch.core.expansions.UserRoleExpansions
import io.paytouch.core.filters.NoFilters
import io.paytouch.core.services.features._
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.UserRoleValidator

class UserRoleService(
    val eventTracker: ActorRef withTag EventTracker,
    userService: => UserService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends UserRoleConversions
       with FindAllFeature
       with FindByIdFeature
       with CreateAndUpdateFeature
       with DeleteFeature {
  type Creation = entities.UserRoleCreation
  type Dao = UserRoleDao
  type Entity = entities.UserRole
  type Expansions = UserRoleExpansions
  type Filters = NoFilters
  type Model = model.UserRoleUpdate
  type Record = model.UserRoleRecord
  type Update = entities.UserRoleUpdate
  type Validator = UserRoleValidator

  protected val dao = daos.userRoleDao
  protected val validator = new UserRoleValidator
  val defaultFilters = NoFilters()

  val classShortName = ExposedName.UserRole

  def findByIds(userRoleIds: Seq[UUID])(e: Expansions)(implicit user: entities.UserContext): Future[Seq[Entity]] =
    dao
      .findByIdsAndMerchantId(userRoleIds, user.merchantId)
      .flatMap(enrich(_, defaultFilters)(e))

  def convertToUpsertionModel(id: UUID, update: Update)(implicit user: entities.UserContext): Future[ErrorsOr[Model]] =
    fromUpsertionToUpdate(id, update)
      .validNel
      .pure[Future]

  def enrich(
      records: Seq[Record],
      f: Filters,
    )(
      e: Expansions,
    )(implicit
      user: entities.UserContext,
    ): Future[Seq[Entity]] =
    for {
      usersCountPerUserRoles <- getOptionalUsersCount(records)(e.withUsersCount)
    } yield fromRecordsAndOptionsToEntities(records, usersCountPerUserRoles, e.withPermissions)

  def getOptionalUsersCount(items: Seq[Record])(withUsersCount: Boolean): Future[DataByRecord[Int]] =
    if (withUsersCount)
      userService
        .countByUserRoles(items)
        .map(_.some)
    else
      None.pure[Future]

  def convertToDefaultUserRoleUpdates(
      merchantId: UUID,
      setupType: model.enums.SetupType,
    ): Future[ErrorsOr[Seq[Model]]] =
    model
      .UserRoleUpdate
      .defaults(merchantId, setupType)
      .validNel
      .pure[Future]
}
