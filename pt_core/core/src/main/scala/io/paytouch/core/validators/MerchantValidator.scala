package io.paytouch.core.validators

import java.util.UUID
import scala.concurrent._

import cats.data._
import cats.implicits._

import io.paytouch.core.data._
import io.paytouch.core.data.daos.{ Daos, MerchantDao }
import io.paytouch.core.data.model.{ MerchantRecord, UserRecord }
import io.paytouch.core.data.model.enums.MerchantMode
import io.paytouch.core.data.model.enums.MerchantMode._
import io.paytouch.core.entities._
import io.paytouch.core.errors._
import io.paytouch.core.services.UtilService
import io.paytouch.core.utils._
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.features.DefaultValidator

class MerchantValidator(implicit val ec: ExecutionContext, val daos: Daos) extends DefaultValidator[MerchantRecord] {
  type Dao = MerchantDao
  type Record = MerchantRecord

  protected val dao = daos.merchantDao
  val userDao = daos.userDao
  val userValidator = new UserValidator

  val validationErrorF = InvalidMerchantIds(_)
  val accessErrorF = NonAccessibleMerchantIds(_)

  def isIdAvailable(id: UUID): Future[ErrorsOr[Unit]] =
    dao.findById(id).map {
      case Some(_) => Multiple.failure(MerchantIdAlreadyInUse(id))
      case None    => Multiple.success((): Unit)
    }

  def adminAccessById(id: UUID)(implicit admin: AdminContext): Future[ErrorsOr[MerchantRecord]] =
    dao.findById(id).map {
      case Some(merchant) => Multiple.success(merchant)
      case None           => Multiple.failure(InvalidAdminIds(Seq(id)))
    }

  def accessById(id: UUID)(implicit user: UserContext): Future[ErrorsOr[MerchantRecord]] =
    dao.findById(id).map {
      case Some(merchant) if merchant.id == user.merchantId => Multiple.success(merchant)
      case _                                                => Multiple.failure(InvalidAdminIds(Seq(id)))
    }

  def validateSwitch(
      merchantId: UUID,
      mode: MerchantMode,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[(MerchantRecord, UserRecord)]] =
    for {
      validSwitchMode <- validateSwitchMode(merchantId, mode)
      validOwner <- validateExistingOwner(merchantId)
    } yield Multiple.combine(validSwitchMode, validOwner) { case x => x }

  private def validateSwitchMode(
      merchantId: UUID,
      mode: MerchantMode,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[MerchantRecord]] =
    accessById(merchantId).map {
      case Validated.Valid(merchant) if mode == Production && merchant.mode != Production =>
        Multiple.success(merchant)

      case Validated.Valid(merchant) if merchant.mode == mode =>
        Multiple.failure(MerchantAlreadyInRequestedMode(merchant.id))

      case Validated.Valid(_) =>
        Multiple.failure(SwitchToDemoModeUnsupported())

      case i @ Validated.Invalid(_) =>
        i
    }

  private def validateExistingOwner(merchantId: UUID)(implicit user: UserContext): Future[ErrorsOr[UserRecord]] =
    userDao.findOwnersByMerchantId(merchantId).map {
      case users if users.isEmpty                 => Multiple.failure(FirstOwnerMissing(merchantId))
      case users if users.exists(_.id == user.id) => Multiple.success(users.find(_.id == user.id).get)
      case Seq(u, _)                              => Multiple.success(u)
    }
}
