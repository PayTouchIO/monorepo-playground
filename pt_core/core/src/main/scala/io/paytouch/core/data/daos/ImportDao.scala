package io.paytouch.core.data.daos

import java.util.UUID

import com.typesafe.scalalogging.LazyLogging
import io.paytouch.core.data.daos.features.SlickMerchantDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.enums.ImportStatus
import io.paytouch.core.data.model.{ ImportRecord, ImportUpdate }
import io.paytouch.core.data.tables.ImportsTable
import io.paytouch.core.utils.UtcTime
import slick.lifted.TableQuery

import scala.concurrent._

class ImportDao(implicit val ec: ExecutionContext, val db: Database) extends SlickMerchantDao with LazyLogging {

  type Record = ImportRecord
  type Update = ImportUpdate
  type Table = ImportsTable

  val table = TableQuery[Table]

  def validationSuccessful(id: UUID, importSummary: JValue) = {
    logger.info(s"[Import $id] validation successful: saving import summary.")
    val importUpdate =
      ImportUpdate(id = Some(id), importSummary = Some(importSummary), validationStatus = Some(ImportStatus.Successful))
    upsert(importUpdate)
  }

  def validationFailed(id: UUID, validationResult: JValue) = {
    logger.info(s"[Import $id] validation failed: saving validation result")
    logger.debug(s"[Import {}] validation failed: saving validation result. ValidationResult: {}", id, validationResult)
    val importUpdate = ImportUpdate(
      id = Some(id),
      validationErrors = Some(validationResult),
      validationStatus = Some(ImportStatus.Failed),
    )
    upsert(importUpdate)
  }

  def validationInProgress(id: UUID) = updateValidationStatusById(id, ImportStatus.InProgress)

  def importInProgress(id: UUID) = updateImportStatusById(id, ImportStatus.InProgress)

  def importSuccessful(id: UUID) = updateImportStatusById(id, ImportStatus.Successful)

  def importFailed(id: UUID) = updateImportStatusById(id, ImportStatus.Failed)

  def updateValidationStatusById(id: UUID, validationStatus: ImportStatus): Future[Boolean] = {
    logger.info(s"[Import $id] validation status set to $validationStatus")
    val field = for { o <- table if o.id === id } yield (o.validationStatus, o.updatedAt)
    run(field.update(validationStatus, UtcTime.now).map(_ > 0))
  }

  def updateImportStatusById(id: UUID, importStatus: ImportStatus): Future[Boolean] = {
    logger.info(s"[Import $id] import status set to $importStatus")
    val field = for { o <- table if o.id === id } yield (o.importStatus, o.updatedAt)
    run(field.update(importStatus, UtcTime.now).map(_ > 0))
  }
}
