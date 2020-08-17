package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.ImportRecord
import io.paytouch.core.data.model.enums.{ ImportStatus, ImportType }
import io.paytouch.core.json.JsonSupport.JValue

class ImportsTable(tag: Tag) extends SlickMerchantTable[ImportRecord](tag, "imports") {

  def id = column[UUID]("id", O.PrimaryKey)

  def `type` = column[ImportType]("type")
  def merchantId = column[UUID]("merchant_id")
  def locationIds = column[Seq[UUID]]("location_ids")

  def filename = column[String]("filename")
  def validationStatus = column[ImportStatus]("validation_status")
  def importStatus = column[ImportStatus]("import_status")
  def validationResult = column[Option[JValue]]("validation_errors")
  def importSummary = column[Option[JValue]]("import_summary")

  def deleteExisting = column[Boolean]("delete_existing")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      `type`,
      merchantId,
      locationIds,
      filename,
      validationStatus,
      importStatus,
      validationResult,
      importSummary,
      deleteExisting,
      createdAt,
      updatedAt,
    ).<>(ImportRecord.tupled, ImportRecord.unapply)
}
