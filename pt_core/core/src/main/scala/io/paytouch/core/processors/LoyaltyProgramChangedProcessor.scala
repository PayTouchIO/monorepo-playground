package io.paytouch.core.processors

import scala.concurrent._

import cats.data._
import cats.implicits._

import io.paytouch.core.clients.urbanairship.entities._
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.LoyaltyProgramRecord
import io.paytouch.core.entities.enums.PassType
import io.paytouch.core.messages.entities.{ LoyaltyProgramChanged, LoyaltyProgramPayload, SQSMessage }
import io.paytouch.core.services.{ LoyaltyProgramService, UrbanAirshipService }

class LoyaltyProgramChangedProcessor(
    loyaltyProgramService: LoyaltyProgramService,
    urbanAirshipService: UrbanAirshipService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends Processor {

  def execute: PartialFunction[SQSMessage[_], Future[Unit]] = {
    case msg: LoyaltyProgramChanged => processLoyaltyProgramChanged(msg)
  }

  private def processLoyaltyProgramChanged(msg: LoyaltyProgramChanged): Future[Unit] =
    upsertUrbanAirshipTemplates(msg.payload).void

  private def upsertUrbanAirshipTemplates(payload: LoyaltyProgramPayload): Future[Unit] =
    (for {
      loyaltyProgram <- OptionT(loyaltyProgramService.findRecordById(payload.data.id))
      templateData <- OptionT(urbanAirshipService.prepareLoyaltyTemplateData(payload.merchantId, payload.data))
      _ <- OptionT.liftF(upsertTemplatesAndPersist(loyaltyProgram, templateData))
    } yield ()).value.void

  private def upsertTemplatesAndPersist(
      loyaltyProgramRecord: LoyaltyProgramRecord,
      templateData: TemplateData.LoyaltyTemplateData,
    ): Future[Unit] =
    for {
      _ <- upsertTemplateAndPersist(loyaltyProgramRecord, _.appleWalletTemplateId, PassType.Ios, templateData)
      _ <- upsertTemplateAndPersist(loyaltyProgramRecord, _.androidPayTemplateId, PassType.Android, templateData)
    } yield ()

  private def upsertTemplateAndPersist(
      record: LoyaltyProgramRecord,
      templateIdField: LoyaltyProgramRecord => Option[String],
      passType: PassType,
      templateData: TemplateData.LoyaltyTemplateData,
    ) =
    urbanAirshipService.upsertTemplate(record.id, passType, templateData).map { templateUpserted =>
      if (templateIdField(record).isEmpty)
        loyaltyProgramService.updateTemplateId(record.id, passType, templateUpserted.templateId)
    }

}
