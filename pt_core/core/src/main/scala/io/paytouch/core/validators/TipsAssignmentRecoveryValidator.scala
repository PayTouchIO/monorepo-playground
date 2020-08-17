package io.paytouch.core.validators

import java.util.UUID

import cats.implicits._

import io.paytouch.core.data.daos.{ Daos, TipsAssignmentDao }
import io.paytouch.core.data.model.{ TipsAssignmentRecord, TipsAssignmentUpdate => TipsAssignmentUpdateModel }
import io.paytouch.core.entities.{ TipsAssignmentUpsertion, UserContext }
import io.paytouch.core.errors.{ InvalidTipsAssignmentIds, NonAccessibleTipsAssignmentIds }
import io.paytouch.core.utils.PaytouchLogger
import io.paytouch.core.validators.features.DefaultRecoveryValidator

import scala.concurrent._

class TipsAssignmentRecoveryValidator(
    implicit
    val ec: ExecutionContext,
    val daos: Daos,
    val logger: PaytouchLogger,
  ) extends DefaultRecoveryValidator[TipsAssignmentRecord] {

  type Record = TipsAssignmentRecord
  type Dao = TipsAssignmentDao

  protected val dao = daos.tipsAssignmentDao
  val validationErrorF = InvalidTipsAssignmentIds(_)
  val accessErrorF = NonAccessibleTipsAssignmentIds(_)

  val locationValidator = new LocationValidatorIncludingDeleted
  val userValidator = new UserValidatorIncludingDeleted
  val orderValidator = new OrderRecoveryValidator
  val cashDrawerActivityValidator = new CashDrawerActivityRecoveryValidator(new CashDrawerRecoveryValidator)

  def recoverUpsertion(
      id: UUID,
      upsertion: TipsAssignmentUpsertion,
    )(implicit
      user: UserContext,
    ): Future[TipsAssignmentUpdateModel] =
    for {
      recoveredId <- recoverId(id, upsertion)
      recoveredLocationId <- recoverLocationId(id, upsertion.locationId, upsertion)
      recoveredUserId <- recoverUserId(id, upsertion.userId, upsertion)
      recoveredOrderId <- recoverOrderId(id, upsertion.orderId, upsertion)
      recoveredHandledViaCashDrawerActivityId <- recoverCashDrawerActivityId(
        id,
        upsertion.handledViaCashDrawerActivityId,
        upsertion,
      )
      recoveredCashDrawerActivityId <- recoverCashDrawerActivityId(
        id,
        upsertion.cashDrawerActivityId,
        upsertion,
      )
    } yield TipsAssignmentUpdateModel(
      id = Some(recoveredId),
      merchantId = Some(user.merchantId),
      locationId = recoveredLocationId,
      userId = recoveredUserId,
      orderId = recoveredOrderId,
      amount = Some(upsertion.amount),
      handledVia = Some(upsertion.handledVia),
      handledViaCashDrawerActivityId = recoveredHandledViaCashDrawerActivityId,
      cashDrawerActivityId = recoveredCashDrawerActivityId,
      paymentType = upsertion.paymentType,
      assignedAt = Some(upsertion.assignedAt),
      deletedAt = None, // is that right?
    )

  private def recoverId(id: UUID, upsertion: TipsAssignmentUpsertion)(implicit user: UserContext): Future[UUID] =
    validateOneById(id).mapNested(_ => id).map { validId =>
      val description = s"While syncing data for tips assignment $id"
      logger.loggedRecoverUUID(validId)(description, upsertion)
    }

  private def recoverLocationId(
      id: UUID,
      locationId: UUID,
      upsertion: TipsAssignmentUpsertion,
    )(implicit
      user: UserContext,
    ): Future[Option[UUID]] =
    locationValidator.accessOneById(locationId).map { validRecord =>
      val description = s"While syncing data for tips assignment $id"
      logger.loggedRecover(validRecord.map(l => Some(l.id)))(description, upsertion)
    }

  private def recoverUserId(
      id: UUID,
      userId: Option[UUID],
      upsertion: TipsAssignmentUpsertion,
    )(implicit
      user: UserContext,
    ): Future[Option[UUID]] =
    userValidator.accessOneByOptId(userId).map { validRecord =>
      val description = s"While syncing data for tips assignment $id"
      logger.loggedRecover(validRecord.map(_.map(_.id)))(description, upsertion)
    }

  private def recoverOrderId(
      id: UUID,
      orderId: Option[UUID],
      upsertion: TipsAssignmentUpsertion,
    )(implicit
      user: UserContext,
    ): Future[Option[UUID]] =
    orderValidator.accessOneByOptId(orderId).map { validRecord =>
      val description = s"While syncing data for tips assignment $id"
      logger.loggedRecover(validRecord.map(_.map(_.id)))(description, upsertion)
    }

  private def recoverCashDrawerActivityId(
      id: UUID,
      cashDrawerActivityId: Option[UUID],
      upsertion: TipsAssignmentUpsertion,
    )(implicit
      user: UserContext,
    ): Future[Option[UUID]] =
    cashDrawerActivityValidator.accessOneByOptId(cashDrawerActivityId).map { validRecord =>
      val description = s"While syncing data for tips assignment $id"
      logger.loggedRecover(validRecord.map(_.map(_.id)))(description, upsertion)
    }

}
