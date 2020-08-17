package io.paytouch.core.validators

import java.util.UUID

import scala.concurrent._

import cats.implicits._

import io.paytouch._
import io.paytouch.core.data.daos.{ Daos, UserDao }
import io.paytouch.core.data.model.{ TimeCardRecord, UserLocationRecord, UserRecord }
import io.paytouch.core.entities.{ UserContext, UserLogin, UserUpdate => UserUpdateEntity }
import io.paytouch.core.errors._
import io.paytouch.core.utils.{ Multiple, Sha1EncryptionSupport }
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.features._

class UserValidator(implicit val ec: ExecutionContext, val daos: Daos)
    extends ValidatorWithExtraFields[UserRecord]
       with PasswordValidator
       with UniqueEmailValidator[UserLogin]
       with Sha1EncryptionSupport
       with DeletionValidator[UserRecord] {
  type Extra = UserLocationRecord
  type Dao = UserDao

  protected val dao = daos.userDao
  protected val userLocationDao = daos.userLocationDao

  val validationErrorF = InvalidUserIds(_)
  val accessErrorF = NonAccessibleUserIds(_)

  val userRoleValidator = new UserRoleValidator

  def validateUpsertion(
      id: UUID,
      update: UserUpdateEntity,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[UserUpdateEntity]] =
    for {
      validPassword <- validatePassword(update.password).pure[Future]
      validEmail <- validateEmail(id, update.email)
      validPin <- validatePin(id, update.pin)
      validUserRole <- userRoleValidator.accessOneByOptId(update.userRoleId)
    } yield Multiple.combine(validPassword, validEmail, validPin, validUserRole) { case _ => update }

  private def validatePin(
      userId: UUID,
      maybePin: Option[String],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[String]]] =
    maybePin match {
      case Some(pin) =>
        dao.findByMerchantIdAndEncryptedPin(user.merchantId, sha1Encrypt(pin)).map { records =>
          if (records.forall(_.id == userId)) Multiple.successOpt(pin)
          else Multiple.failure(PinAlreadyInUse())
        }
      case None => Future.successful(Multiple.empty)
    }

  protected def findUserById(email: String) = dao.findUserLoginByEmail(email)

  protected def recordsFinder(ids: Seq[UUID])(implicit user: UserContext): Future[Seq[UserRecord]] =
    dao.findByIds(ids)

  protected def extraRecordsFinder(ids: Seq[UUID])(implicit user: UserContext): Future[Seq[UserLocationRecord]] =
    userLocationDao.findByItemIds(ids)

  protected def validityCheckWithExtraRecords(
      record: UserRecord,
      extraRecords: Seq[UserLocationRecord],
    )(implicit
      user: UserContext,
    ): Boolean = {
    def canAccessOtherUser = {
      val locationIdsForUser = extraRecords.filter(r => r.userId == record.id).map(_.locationId)
      val commonLocations = user.locationIds intersect locationIdsForUser
      record.merchantId == user.merchantId && commonLocations.nonEmpty
    }
    val isAccessingItself = user.id == record.id
    isAccessingItself || canAccessOtherUser
  }

  def validateByPinAndLocation(
      pin: String,
      locationId: UUID,
    )(implicit
      userContext: UserContext,
    ): Future[ErrorsOr[UserRecord]] =
    for {
      maybeUser <- dao.findOneByMerchantIdAndEncryptedPin(userContext.merchantId, sha1Encrypt(pin))
      locations <- userLocationDao.findByItemIdsAndLocationIds(maybeUser.toSeq.map(_.id), Seq(locationId))
    } yield (maybeUser, locations) match {
      case (Some(user), l) if l.nonEmpty => Multiple.success(user)
      case (Some(user), l) if l.isEmpty  => Multiple.failure(UserNotEnabledInLocation(user.id, locationId))
      case (_, _)                        => Multiple.failure(NoUserMatchingPin())
    }

  def validateAuth0UserId(
      id: UUID,
      maybeUserId: Option[Auth0UserId],
      idToIgnore: Option[UUID] = None,
    ): Future[ErrorsOr[Option[Auth0UserId]]] =
    maybeUserId
      .map { userId =>
        dao.findUserLoginByAuth0UserId(userId).map {
          case Some(userLogin) if idToIgnore.contains(userLogin.id) => Multiple.success(maybeUserId)
          case Some(userLogin) if userLogin.id != id                => Multiple.failure(Auth0UserAlreadyInUse())
          case _                                                    => Multiple.success(maybeUserId)
        }
      }
      .getOrElse(Future.successful(Multiple.success(None)))
}
