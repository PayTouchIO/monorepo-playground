package io.paytouch.core.validators

import java.util.UUID

import cats.implicits._

import io.paytouch.core.data.daos.{ CashDrawerActivityDao, Daos }
import io.paytouch.core.data.model.{
  CashDrawerActivityRecord,
  CashDrawerActivityUpdate => CashDrawerActivityUpdateModel,
}
import io.paytouch.core.entities.{ CashDrawerActivityUpsertion, UserContext }
import io.paytouch.core.errors.{ InvalidCashDrawerIds, NonAccessibleCashDrawerIds }
import io.paytouch.core.utils.PaytouchLogger
import io.paytouch.core.validators.features.DefaultRecoveryValidator

import scala.concurrent._

class CashDrawerActivityRecoveryValidator(
    cashDrawerValidator: CashDrawerRecoveryValidator,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
    val logger: PaytouchLogger,
  ) extends DefaultRecoveryValidator[CashDrawerActivityRecord] {

  type Record = CashDrawerActivityRecord
  type Dao = CashDrawerActivityDao

  protected val dao = daos.cashDrawerActivityDao
  val validationErrorF = InvalidCashDrawerIds(_)
  val accessErrorF = NonAccessibleCashDrawerIds(_)

  val userValidator = new UserValidatorIncludingDeleted

  def recoverUpsertion(
      id: UUID,
      upsertion: CashDrawerActivityUpsertion,
    )(implicit
      user: UserContext,
    ): Future[CashDrawerActivityUpdateModel] =
    for {
      recoveredId <- recoverId(id, upsertion)
      recoveredUserId <- recoverUserId(id, Some(upsertion.userId), upsertion)
      recoveredTipForUserId <- recoverUserId(id, upsertion.tipForUserId, upsertion)
    } yield CashDrawerActivityUpdateModel(
      id = Some(recoveredId),
      merchantId = Some(user.merchantId),
      userId = recoveredUserId,
      orderId = upsertion.orderId,
      cashDrawerId = upsertion.cashDrawerId,
      `type` = Some(upsertion.`type`),
      startingCashAmount = upsertion.startingCashAmount,
      endingCashAmount = upsertion.endingCashAmount,
      payInAmount = upsertion.payInAmount,
      payOutAmount = upsertion.payOutAmount,
      tipInAmount = upsertion.tipInAmount,
      tipOutAmount = upsertion.tipOutAmount,
      currentBalanceAmount = Some(upsertion.currentBalanceAmount),
      tipForUserId = recoveredTipForUserId,
      timestamp = Some(upsertion.timestamp),
      notes = upsertion.notes,
    )

  private def recoverId(id: UUID, upsertion: CashDrawerActivityUpsertion)(implicit user: UserContext): Future[UUID] =
    validateOneById(id).mapNested(_ => id).map { validId =>
      val description = s"While syncing data for cash drawer activity $id"
      logger.loggedRecoverUUID(validId)(description, upsertion)
    }

  private def recoverUserId(
      id: UUID,
      userId: Option[UUID],
      upsertion: CashDrawerActivityUpsertion,
    )(implicit
      user: UserContext,
    ): Future[Option[UUID]] =
    userValidator.accessOneByOptId(userId).map { validRecord =>
      val description = s"While syncing data for cash drawer activity $id"
      logger.loggedRecover(validRecord.map(_.map(_.id)))(description, upsertion)
    }
}
