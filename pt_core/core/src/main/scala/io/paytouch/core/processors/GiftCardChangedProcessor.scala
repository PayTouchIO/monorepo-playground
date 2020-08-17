package io.paytouch.core.processors

import scala.concurrent._

import cats.data._
import cats.implicits._

import io.paytouch.core.clients.urbanairship.entities._
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.GiftCardRecord
import io.paytouch.core.entities.enums.PassType
import io.paytouch.core.messages.entities.{ GiftCardChanged, GiftCardPayload, SQSMessage }
import io.paytouch.core.services.{ GiftCardService, UrbanAirshipService }

class GiftCardChangedProcessor(
    giftCardService: GiftCardService,
    urbanAirshipService: UrbanAirshipService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends Processor {
  def execute: PartialFunction[SQSMessage[_], Future[Unit]] = {
    case msg: GiftCardChanged => processGiftCardChanged(msg)
  }

  private def processGiftCardChanged(msg: GiftCardChanged): Future[Unit] =
    (for {
      giftCard <- OptionT(giftCardService.findRecordById(msg.payload.data.id))
      templateData <- OptionT(urbanAirshipService.prepareGiftCardTemplateData(msg.payload.merchantId, msg.payload.data))
      _ <- OptionT.liftF(upsertTemplatesAndPersist(giftCard, templateData))
    } yield ()).value.void

  private def upsertTemplatesAndPersist(
      giftCardRecord: GiftCardRecord,
      templateData: TemplateData.GiftCardTemplateData,
    ): Future[Unit] =
    for {
      _ <- upsertTemplateAndPersist(giftCardRecord, _.appleWalletTemplateId, PassType.Ios, templateData)
      _ <- upsertTemplateAndPersist(giftCardRecord, _.androidPayTemplateId, PassType.Android, templateData)
    } yield ()

  private def upsertTemplateAndPersist(
      record: GiftCardRecord,
      templateIdField: GiftCardRecord => Option[String],
      passType: PassType,
      templateData: TemplateData.GiftCardTemplateData,
    ) =
    urbanAirshipService.upsertTemplate(record.id, passType, templateData).map { templateUpserted =>
      if (templateIdField(record).isEmpty)
        giftCardService.updateTemplateId(record.id, passType, templateUpserted.templateId)
    }

}
