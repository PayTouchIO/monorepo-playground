package io.paytouch.core.services

import java.util.UUID

import akka.actor.ActorRef
import cats.data.OptionT
import cats.data.Validated._
import cats.instances.future._
import io.paytouch.core.BcryptRounds
import io.paytouch.core.async.monitors.{ UserChange, UserMonitor }
import io.paytouch.core.async.trackers.EventTracker
import io.paytouch.core.conversions.UserConversions
import io.paytouch.core.data.daos.{ Daos, UserDao }
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.data.model.upsertions.UserUpsertion
import io.paytouch.core.data.model.{
  ImageUploadRecord,
  Permission,
  UserRecord,
  UserRoleRecord,
  UserUpdate => UserUpdateModel,
}
import io.paytouch.core.entities.enums.{ ContextSource, ExposedName, MerchantSetupSteps }
import io.paytouch.core.entities.{ User => UserEntity, UserUpdate => UserUpdateEntity, _ }
import io.paytouch.core.expansions.{ LocationExpansions, MerchantExpansions, UserExpansions }
import io.paytouch.core.filters.UserFilters
import io.paytouch.core.services.features._
import io.paytouch.core.utils.ResultType
import io.paytouch.utils.Tagging.withTag
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.validators.UserValidator
import io.paytouch.core.RichMap
import io.paytouch.core.utils.Multiple

import scala.concurrent._

class UserService(
    val bcryptRounds: Int withTag BcryptRounds,
    val eventTracker: ActorRef withTag EventTracker,
    val monitor: ActorRef withTag UserMonitor,
    val imageUploadService: ImageUploadService,
    locationService: => LocationService,
    merchantService: => MerchantService,
    val setupStepService: SetupStepService,
    val userRoleService: UserRoleService,
    val userLocationService: UserLocationService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends UserConversions
       with CreateAndUpdateFeatureWithStateProcessing
       with FindAllFeature
       with FindByIdFeature
       with UpdateActiveItemFeature
       with SoftDeleteFeature {

  type Creation = UserCreation
  type Dao = UserDao
  type Entity = UserEntity
  type Expansions = UserExpansions
  type Filters = UserFilters
  type Model = UserUpsertion
  type Record = UserRecord
  type Update = UserUpdateEntity
  type Validator = UserValidator

  type State = (Record, Seq[ImageUploadRecord])

  protected val dao = daos.userDao
  protected val validator = new UserValidator
  val defaultFilters = UserFilters()

  val classShortName = ExposedName.User

  val imageUploadDao = daos.imageUploadDao
  val locationDao = daos.locationDao
  val userLocationDao = daos.userLocationDao
  val itemLocationService = userLocationService

  def findUserLoginByEmail(email: String): Future[Option[UserLogin]] = dao.findUserLoginByEmail(email)
  def findUserLoginById(userId: UUID): Future[Option[UserLogin]] = dao.findUserLoginById(userId)
  def findActiveUserLoginById(userId: UUID): Future[Option[UserLogin]] = dao.findActiveUserLoginById(userId)

  def getUserContext(userId: UUID, source: ContextSource): Future[Option[UserContext]] =
    (for {
      user <- OptionT(dao.findActiveUserLoginById(userId))
      locationIds <- OptionT.liftF(getLocationIds(user))
      merchant <- OptionT(merchantService.findById(user.merchantId)(MerchantExpansions.none))
    } yield UserContext(
      id = user.id,
      merchantId = merchant.id,
      currency = merchant.currency,
      businessType = merchant.businessType,
      locationIds = locationIds,
      adminId = None,
      merchantSetupCompleted = merchant.setupCompleted,
      source = source,
      paymentProcessor = merchant.paymentProcessor,
    )).value

  private def getLocationIds(user: UserLogin): Future[Seq[UUID]] =
    userLocationDao.findByItemId(user.id).map(_.map(_.locationId))

  def enrich(users: Seq[Record], f: Filters)(e: Expansions)(implicit user: UserContext): Future[Seq[Entity]] = {
    val userRolesR = getOptionalUserRoles(users)(e)
    val locationsPerUserR = getOptionalLocations(users)(e.withLocations)
    val merchantR = getOptionalMerchant(e.withMerchant, e.withMerchantSetupSteps, e.withMerchantLegalDetails)
    val imageUrlsPerUserR = getImageUrls(users)
    for {
      userRoles <- userRolesR
      locationsPerUser <- locationsPerUserR
      merchant <- merchantR
      imageUrlsPerUser <- imageUrlsPerUserR
    } yield {
      val userRolesPerUser = toUserRolePerUser(users, userRoles)(e.withPermissions)
      val accessPerUser = toAccessPerUser(users, userRoles)(e.withAccess)
      fromRecordsAndOptionsToEntities(
        users,
        userRolesPerUser,
        locationsPerUser,
        merchant,
        imageUrlsPerUser,
        accessPerUser,
      )
    }
  }

  private def getOptionalUserRoles(
      users: Seq[Record],
    )(
      e: Expansions,
    )(implicit
      user: UserContext,
    ): Future[Seq[UserRole]] = {
    val userRoleIds = users.flatMap(_.userRoleId)
    userRoleService.findByIds(userRoleIds)(e.asUserRoleExpansions)
  }

  private def getOptionalLocations(
      users: Seq[Record],
    )(
      withLocations: Boolean,
    )(implicit
      user: UserContext,
    ): Future[DataSeqByRecord[Location]] =
    if (withLocations) {
      val userIds = users.map(_.id)
      locationService.findAllByUserIds(userIds)(LocationExpansions.all).map { locationsPerUserId =>
        val locationsPerUser = locationsPerUserId.mapKeysToRecords(users)
        Some(locationsPerUser)
      }
    }
    else Future.successful(None)

  private def getOptionalMerchant(
      withMerchant: Boolean,
      withMerchantSetupSteps: Boolean,
      withMerchantLegalDetails: Boolean,
    )(implicit
      user: UserContext,
    ): Future[Option[Merchant]] =
    if (withMerchant)
      merchantService
        .findById(user.merchantId)(
          MerchantExpansions
            .none
            .copy(
              withSetupSteps = withMerchantSetupSteps,
              withLegalDetails = withMerchantLegalDetails,
            ),
        )
    else
      Future.successful(None)

  private def getImageUrls(users: Seq[Record])(implicit user: UserContext): Future[Map[Record, Seq[ImageUrls]]] =
    imageUploadService.findByObjectIds(users.map(_.id), ImageUploadType.User).map(_.mapKeysToRecords(users))

  def getUserInfoByIds(userIds: Seq[UUID]): Future[Seq[UserInfo]] = dao.findUserInfoByIds(userIds)

  protected def convertToUpsertionModel(
      id: UUID,
      update: Update,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Model]] =
    for {
      aUser <- convertToUserUpdate(id, update)
      locationOverrides <- handleLocationOverridesForOwner(id, update)
      userLocations <- itemLocationService.convertToUserLocationUpdates(id, locationOverrides)
      imageUpload <- imageUploadService.convertToImageUploadUpdates(id, ImageUploadType.User, update.avatarImageId)
    } yield Multiple.combine(aUser, userLocations, imageUpload)(UserUpsertion)

  private def handleLocationOverridesForOwner(
      id: UUID,
      update: Update,
    )(implicit
      user: UserContext,
    ): Future[Map[UUID, Boolean]] =
    update match {
      case u if u.locationIds.isDefined =>
        legacyHandleLocationOverridesForOwner(id, update, u.locationIds.getOrElse(Seq.empty))
      case _ =>
        for {
          existingUser <- dao.findById(id)
          hasOwnership = update.isOwner.isEmpty && existingUser.exists(_.isOwner)
          receivesOwnership = update.isOwner.contains(true)
          hasOwnerShip = receivesOwnership || hasOwnership
          locationOverrides <- {
            if (hasOwnerShip) locationDao.findAllByMerchantId(user.merchantId).map(ls => ls.map(_.id -> true).toMap)
            else Future.successful(update.locationOverrides)
          }
        } yield locationOverrides
    }

  private def legacyHandleLocationOverridesForOwner(
      id: UUID,
      update: Update,
      updateLocationIds: Seq[UUID],
    )(implicit
      user: UserContext,
    ): Future[Map[UUID, Boolean]] =
    for {
      existingUser <- dao.findById(id)
      hasOwnerShip = update.isOwner.contains(true) || existingUser.exists(_.isOwner)
      locationIds <- {
        if (hasOwnerShip) locationDao.findAllByMerchantId(user.merchantId).map(ls => ls.map(_.id))
        else Future.successful(updateLocationIds)
      }
    } yield locationIds.map(_ -> true).toMap

  def countByUserRoles(userRoles: Seq[UserRoleRecord]): Future[Map[UserRoleRecord, Int]] =
    dao.countByUserRoleIds(userRoles.map(_.id)).map(_.mapKeysToRecords(userRoles))

  protected def saveCurrentState(record: UserRecord)(implicit user: UserContext): Future[State] =
    for {
      imageUploads <- imageUploadDao.findByObjectIds(Seq(record.id))
    } yield (record, imageUploads)

  protected def processChangeOfState(
      state: Option[State],
      update: Update,
      resultType: ResultType,
      entity: Entity,
    )(implicit
      user: UserContext,
    ): Future[Unit] = {
    state.foreach(s => monitor ! UserChange(s, update, user))
    setupStepService.simpleCheckStepCompletion(user.merchantId, MerchantSetupSteps.SetupEmployees)
  }

  protected def saveCreationState(id: UUID, creation: Creation)(implicit user: UserContext): Future[Option[State]] =
    Future.successful(None)

  private def convertToUserUpdate(
      id: UUID,
      update: UserUpdateEntity,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[UserUpdateModel]] =
    validator.validateUpsertion(id, update).mapNested(fromUpsertionToUpdate(id, _))

  def convertToUserUpdate(id: UUID, creation: MerchantCreation): Future[ErrorsOr[UserUpdateModel]] =
    validator.validateEmail(id, creation.email).flatMap {
      case Valid(_) =>
        validator.validateAuth0UserId(id, creation.auth0UserId).map {
          case Valid(_) =>
            Valid(fromMerchantCreationToOwnerUserUpdate(id, creation))

          case i @ Invalid(_) => i
        }
      case i @ Invalid(_) => Future.successful(i)
    }

  def findOwners(merchantId: UUID): Future[Seq[UserRecord]] =
    findOwnersByMerchantIds(Seq(merchantId)).map(_.getOrElse(merchantId, Seq.empty))

  def findOwner(merchantId: UUID): Future[Option[UserRecord]] =
    findOwners(merchantId).map(_.headOption)

  def findOwnersByMerchantIds(merchantIds: Seq[UUID]): Future[Map[UUID, Seq[UserRecord]]] =
    dao.findOwnersByMerchantIds(merchantIds)

  def findOwnerByMerchantIds(merchantIds: Seq[UUID]): Future[Map[UUID, UserRecord]] =
    findOwnersByMerchantIds(merchantIds).map { usersMap =>
      usersMap.flatMap {
        case (key, users) =>
          users.headOption.map(user => (key, user))
      }
    }

  def renameEmailsByMerchantId(merchantId: UUID, prefix: String): Future[Boolean] =
    dao.renameEmailsByMerchantId(merchantId, prefix).map(_ > 0)

  def copyOwnerDataFromMerchant(fromMerchantId: UUID, toMerchantId: UUID): Future[Int] = {
    val optT = for {
      fromOwner <- OptionT(findOwner(fromMerchantId))
      toOwner <- OptionT(findOwner(toMerchantId))
      changes <- OptionT.liftF(dao.setEncryptedPasswordAndPin(toOwner.id, fromOwner.encryptedPassword, fromOwner.pin))
    } yield changes
    optT.value.map(_.getOrElse(0))
  }

  def findUsersWithPermission(
      target: String,
      permissionName: String,
      permissionToFilterFor: Permission,
    )(implicit
      user: UserContext,
    ): Future[Seq[Entity]] =
    dao
      .findUsersWithPermission(user.merchantId, target, permissionName, permissionToFilterFor)
      .flatMap(enrich(_, defaultFilters)(UserExpansions.empty))
}
