package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.{ ImportStatus, ImportType }
import io.paytouch.core.json.JsonSupport.JValue

final case class ImportRecord(
    id: UUID,
    `type`: ImportType,
    merchantId: UUID,
    locationIds: Seq[UUID],
    filename: String,
    validationStatus: ImportStatus,
    importStatus: ImportStatus,
    validationErrors: Option[JValue],
    importSummary: Option[JValue],
    deleteExisting: Boolean,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class ImportUpdate(
    id: Option[UUID],
    `type`: Option[ImportType] = None,
    merchantId: Option[UUID] = None,
    locationIds: Option[Seq[UUID]] = None,
    filename: Option[String] = None,
    validationStatus: Option[ImportStatus] = None,
    importStatus: Option[ImportStatus] = None,
    validationErrors: Option[JValue] = None,
    importSummary: Option[JValue] = None,
    deleteExisting: Option[Boolean] = None,
  ) extends SlickMerchantUpdate[ImportRecord] {

  def toRecord: ImportRecord = {
    require(merchantId.isDefined, s"Impossible to convert ImportUpdate without a merchant id. [$this]")
    require(locationIds.isDefined, s"Impossible to convert ImportUpdate without a sequence of location ids. [$this]")
    require(filename.isDefined, s"Impossible to convert ImportUpdate without a filename id. [$this]")
    ImportRecord(
      id = id.getOrElse(UUID.randomUUID),
      `type` = `type`.get,
      merchantId = merchantId.get,
      locationIds = locationIds.get,
      filename = filename.get,
      validationStatus = validationStatus.getOrElse(ImportStatus.NotStarted),
      importStatus = importStatus.getOrElse(ImportStatus.NotStarted),
      validationErrors = validationErrors,
      importSummary = importSummary,
      deleteExisting = deleteExisting.getOrElse(false),
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: ImportRecord): ImportRecord =
    ImportRecord(
      id = id.getOrElse(record.id),
      `type` = `type`.getOrElse(record.`type`),
      merchantId = merchantId.getOrElse(record.merchantId),
      locationIds = locationIds.getOrElse(record.locationIds),
      filename = filename.getOrElse(record.filename),
      validationStatus = validationStatus.getOrElse(record.validationStatus),
      importStatus = importStatus.getOrElse(record.importStatus),
      validationErrors = validationErrors.orElse(record.validationErrors),
      importSummary = importSummary.orElse(record.importSummary),
      deleteExisting = deleteExisting.getOrElse(record.deleteExisting),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
