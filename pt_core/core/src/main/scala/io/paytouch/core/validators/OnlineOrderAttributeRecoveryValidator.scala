package io.paytouch.core.validators

import java.util.UUID

import cats.implicits._

import io.paytouch.core.data.daos.{ Daos, OnlineOrderAttributeDao }
import io.paytouch.core.data.model._
import io.paytouch.core.errors.{ InvalidAcceptanceStatus, MissingOnlineOrderAttributes }
import io.paytouch.core.data.model.enums.{ AcceptanceStatus, CancellationStatus, Source }
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ Multiple, PaytouchLogger }
import io.paytouch.core.validators.features.DefaultRecoveryValidator

import io.paytouch.core.utils.Multiple.ErrorsOr

import scala.concurrent._

class OnlineOrderAttributeRecoveryValidator(
    implicit
    val ec: ExecutionContext,
    val daos: Daos,
    val logger: PaytouchLogger,
  ) extends DefaultRecoveryValidator[OnlineOrderAttributeRecord] {

  val onlineOrderAttributeValidator = new OnlineOrderAttributeValidator
  type Record = OnlineOrderAttributeRecord
  type Dao = OnlineOrderAttributeDao

  protected val dao = daos.onlineOrderAttributeDao
  val validationErrorF = onlineOrderAttributeValidator.validationErrorF
  val accessErrorF = onlineOrderAttributeValidator.accessErrorF

  def validateUpsertion(
      orderId: UUID,
      upsertion: OrderUpsertion,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[OrderUpsertion]] =
    (upsertion.onlineOrderAttribute, upsertion.source) match {
      case (Some(ooa), _) =>
        for {
          validated <- onlineOrderAttributeValidator.validateOneById(ooa.id)
          validStatus = validateAcceptanceStatus(ooa)
        } yield Multiple.combine(validated, validStatus) { case _ => upsertion }
      case (None, Some(Source.Storefront)) =>
        Future.successful(Multiple.failure(MissingOnlineOrderAttributes()))
      case _ =>
        Future.successful(Multiple.success(upsertion))
    }

  private def validateAcceptanceStatus(
      ooa: OnlineOrderAttributeUpsertion,
    ): ErrorsOr[OnlineOrderAttributeUpsertion] =
    ooa.acceptanceStatus match {
      case None | Some(AcceptanceStatus.Open) | Some(AcceptanceStatus.Pending) =>
        Multiple.success(ooa)
      case Some(invalid) =>
        Multiple.failure(InvalidAcceptanceStatus(invalid))
    }

  def recoverUpsertion(
      orderId: UUID,
      upsertion: OrderUpsertion,
    )(implicit
      user: UserContext,
    ): Future[Option[RecoveredOnlineOrderAttributeUpsertion]] =
    (upsertion.onlineOrderAttribute, upsertion.source) match {
      case (Some(ooa), _) => recoverUpsertion(ooa).map(Some(_))
      case (None, Some(Source.Storefront)) =>
        logger.recoverLog(s"Missing online order attributes for a storefront order $orderId", upsertion)
        Future.successful(None)
      case _ => Future.successful(None)
    }

  private def recoverUpsertion(
      upsertion: OnlineOrderAttributeUpsertion,
    )(implicit
      user: UserContext,
    ): Future[RecoveredOnlineOrderAttributeUpsertion] =
    onlineOrderAttributeValidator.validateOneById(upsertion.id).asNested(upsertion.id).map { attributeId =>
      val context = "While recovering online order attribute id not accessible"
      val recoveredAttributeId = logger.loggedSoftRecoverUUID(attributeId)(context)

      val recoveredAcceptanceStatus = upsertion.acceptanceStatus match {
        case None | Some(AcceptanceStatus.Open) | Some(AcceptanceStatus.Pending) =>
          upsertion.acceptanceStatus
        case Some(_) => None
      }
      toRecoveredOnlineOrderAttributeUpsertion(recoveredAttributeId, recoveredAcceptanceStatus, upsertion)
    }

  private def toRecoveredOnlineOrderAttributeUpsertion(
      recoveredAttributeId: UUID,
      recoveredAcceptanceStatus: Option[AcceptanceStatus],
      upsertion: OnlineOrderAttributeUpsertion,
    ): RecoveredOnlineOrderAttributeUpsertion =
    RecoveredOnlineOrderAttributeUpsertion(
      id = recoveredAttributeId,
      prepareByTime = upsertion.prepareByTime,
      prepareByDateTime = upsertion.prepareByDateTime,
      estimatedPrepTimeInMins = upsertion.estimatedPrepTimeInMins,
      acceptanceStatus = recoveredAcceptanceStatus,
      cancellationStatus = upsertion.cancellationStatus,
      cancellationReason = upsertion.cancellationReason,
    )
}

final case class RecoveredOnlineOrderAttributeUpsertion(
    id: UUID,
    prepareByTime: ResettableLocalTime,
    prepareByDateTime: ResettableZonedDateTime,
    estimatedPrepTimeInMins: ResettableInt,
    acceptanceStatus: Option[AcceptanceStatus],
    cancellationStatus: Option[CancellationStatus],
    cancellationReason: Option[String],
  )
