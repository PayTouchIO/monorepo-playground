package io.paytouch.core.validators

import java.util.UUID

import cats.implicits._

import io.paytouch.core.data.daos.{ CashDrawerDao, Daos }
import io.paytouch.core.data.model.{ CashDrawerRecord, CashDrawerUpdate => CashDrawerUpdateModel }
import io.paytouch.core.data.model.upsertions.{ CashDrawerUpsertion => CashDrawerUpsertionModel }
import io.paytouch.core.entities.{ CashDrawerUpsertion, UserContext }
import io.paytouch.core.errors.{ InvalidCashDrawerIds, NonAccessibleCashDrawerIds }
import io.paytouch.core.utils.PaytouchLogger
import io.paytouch.core.validators.features.DefaultRecoveryValidator

import scala.concurrent._

class CashDrawerRecoveryValidator(
    implicit
    val ec: ExecutionContext,
    val daos: Daos,
    val logger: PaytouchLogger,
  ) extends DefaultRecoveryValidator[CashDrawerRecord] {
  type Record = CashDrawerRecord
  type Dao = CashDrawerDao

  protected val dao = daos.cashDrawerDao
  val validationErrorF = InvalidCashDrawerIds(_)
  val accessErrorF = NonAccessibleCashDrawerIds(_)

  val locationValidator = new LocationValidatorIncludingDeleted
  val userValidator = new UserValidatorIncludingDeleted
  val cashDrawerActivityRecoveryValidator = new CashDrawerActivityRecoveryValidator(this)

  def recoverUpsertion(
      id: UUID,
      upsertion: CashDrawerUpsertion,
    )(implicit
      user: UserContext,
    ): Future[CashDrawerUpsertionModel] =
    for {
      recoveredId <- recoverId(id, upsertion)
      recoveredLocationId <- recoverLocationId(id, upsertion)
      recoveredUserId <- recoverUserId(id, upsertion, upsertion.userId)
      recoveredEmployeeId <-
        upsertion
          .employeeId
          .map(eId => recoverUserId(id, upsertion, eId))
          .getOrElse(Future.successful(None))
      recoveredActivities <- Future.sequence(
        upsertion
          .appendActivities
          .getOrElse(Seq.empty)
          .map(u => cashDrawerActivityRecoveryValidator.recoverUpsertion(u.id, u)),
      )
    } yield CashDrawerUpsertionModel(
      CashDrawerUpdateModel(
        id = Some(recoveredId),
        merchantId = Some(user.merchantId),
        locationId = recoveredLocationId,
        userId = recoveredUserId,
        name = upsertion.name,
        employeeId = recoveredEmployeeId,
        startingCashAmount = Some(upsertion.startingCashAmount.toOption.getOrElse(0)),
        endingCashAmount = upsertion.endingCashAmount,
        cashSalesAmount = upsertion.cashSalesAmount,
        cashRefundsAmount = upsertion.cashRefundsAmount,
        paidInAndOutAmount = upsertion.paidInAndOutAmount,
        paidInAmount = upsertion.paidInAmount,
        paidOutAmount = upsertion.paidOutAmount,
        manualPaidInAmount = upsertion.manualPaidInAmount,
        manualPaidOutAmount = upsertion.manualPaidOutAmount,
        tippedInAmount = upsertion.tippedInAmount,
        tippedOutAmount = upsertion.tippedOutAmount,
        expectedAmount = upsertion.expectedAmount,
        status = Some(upsertion.status),
        startedAt = Some(upsertion.startedAt),
        endedAt = upsertion.endedAt,
        exportFilename = None,
        printerMacAddress = upsertion.printerMacAddress,
      ),
      recoveredActivities.map(_.copy(cashDrawerId = Some(recoveredId))),
    )

  private def recoverId(id: UUID, upsertion: CashDrawerUpsertion)(implicit user: UserContext): Future[UUID] =
    validateOneById(id).mapNested(_ => id).map { validId =>
      val description = s"While syncing data for cash drawer $id"
      logger.loggedRecoverUUID(validId)(description, upsertion)
    }

  private def recoverLocationId(
      id: UUID,
      upsertion: CashDrawerUpsertion,
    )(implicit
      user: UserContext,
    ): Future[Option[UUID]] = {
    val locationId = upsertion.locationId
    locationValidator.accessOneById(locationId).map { validRecord =>
      val description = s"While syncing data for cash drawer $id"
      logger.loggedRecover(validRecord.map(location => Option(location.id)))(description, upsertion)
    }
  }

  private def recoverUserId(
      id: UUID,
      upsertion: CashDrawerUpsertion,
      userId: UUID,
    )(implicit
      user: UserContext,
    ): Future[Option[UUID]] =
    userValidator.accessOneById(userId).map { validRecord =>
      val description = s"While syncing data for cash drawer $id"
      logger.loggedRecover(validRecord.map(user => Option(user.id)))(description, upsertion)
    }
}
