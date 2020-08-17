package io.paytouch.core.services

import java.util.UUID

import scala.concurrent._

import cats.data._
import cats.implicits._

import io.paytouch.core.data.daos.Daos
import io.paytouch.core.entities.{ ExposedEntity, SetupStepCondition, UserContext }
import io.paytouch.core.entities.enums.{ MerchantSetupStatus, MerchantSetupSteps }
import io.paytouch.core.entities.enums.MerchantSetupStatus._
import io.paytouch.core.expansions.MerchantExpansions

class SetupStepService(merchantService: => MerchantService)(implicit val ec: ExecutionContext, daos: Daos) {
  def checkStepCompletion[E <: ExposedEntity](
      entity: E,
      step: MerchantSetupSteps,
    )(implicit
      setupStepCondition: SetupStepCondition[E],
      user: UserContext,
    ): Future[Unit] =
    if (user.merchantSetupCompleted)
      Future.unit
    else
      loadData(user.merchantId, step).flatMap {
        case Some(Pending) if setupStepCondition(entity) =>
          markAsCompleted(user.merchantId, step)

        case _ =>
          Future.unit
      }

  def simpleCheckStepCompletion(merchantId: UUID, step: MerchantSetupSteps): Future[Unit] =
    loadData(merchantId, step).flatMap {
      case Some(Pending) => markAsCompleted(merchantId, step)
      case _             => Future.unit
    }

  private def loadData(merchantId: UUID, step: MerchantSetupSteps): Future[Option[MerchantSetupStatus]] =
    (for {
      merchant <- OptionT(merchantService.findById(merchantId)(MerchantExpansions.none.copy(withSetupSteps = true)))
      if !merchant.setupCompleted
      setupStep <- OptionT.fromOption[Future](merchant.setupSteps.flatMap(_.get(step)))
    } yield setupStep).value

  private def markAsCompleted(merchantId: UUID, step: MerchantSetupSteps): Future[Unit] =
    merchantService.unvalidatedCompleteSetupStep(merchantId, step).void
}
