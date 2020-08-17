package io.paytouch.core.reports.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.{ SlickMerchantRecord, SlickMerchantUpdate }
import io.paytouch.core.reports.data.model.enums.ExportStatus

final case class ExportRecord(
    id: UUID,
    `type`: String,
    merchantId: UUID,
    status: ExportStatus,
    baseUrl: Option[String],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

final case class ExportUpdate(
    id: Option[UUID],
    `type`: Option[String],
    merchantId: Option[UUID],
    status: Option[ExportStatus],
    baseUrl: Option[String],
  ) extends SlickMerchantUpdate[ExportRecord] {

  def toRecord: ExportRecord = {
    require(merchantId.isDefined, s"Impossible to convert ExportRecord without a merchant id. [$this]")
    ExportRecord(
      id = id.getOrElse(UUID.randomUUID),
      `type` = `type`.get,
      merchantId = merchantId.get,
      status = status.getOrElse(ExportStatus.NotStarted),
      baseUrl = baseUrl,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: ExportRecord): ExportRecord =
    ExportRecord(
      id = id.getOrElse(record.id),
      `type` = `type`.getOrElse(record.`type`),
      merchantId = merchantId.getOrElse(record.merchantId),
      status = status.getOrElse(record.status),
      baseUrl = baseUrl.orElse(record.baseUrl),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
