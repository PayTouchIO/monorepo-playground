package io.paytouch.core.reports.data.daos

import java.util.UUID

import com.typesafe.scalalogging.LazyLogging
import io.paytouch.core.data.daos.features.SlickMerchantDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.reports.data.model.enums.ExportStatus
import io.paytouch.core.reports.data.model.{ ExportRecord, ExportUpdate }
import io.paytouch.core.reports.data.tables.ExportsTable
import io.paytouch.core.utils.UtcTime
import slick.lifted.TableQuery

import scala.concurrent._

class ExportDao(implicit val ec: ExecutionContext, val db: Database) extends SlickMerchantDao with LazyLogging {

  type Record = ExportRecord
  type Update = ExportUpdate
  type Table = ExportsTable

  val table = TableQuery[Table]

  def exportProcessing(id: UUID) = updateStatusById(id, ExportStatus.Processing)

  def exportUploading(id: UUID) = updateStatusById(id, ExportStatus.Uploading)

  def exportCompleted(id: UUID, url: String) = updateStatusById(id, ExportStatus.Completed, Some(url))

  def exportFailed(id: UUID) = updateStatusById(id, ExportStatus.Failed)

  def updateStatusById(
      id: UUID,
      status: ExportStatus,
      url: Option[String] = None,
    ): Future[Boolean] = {
    logger.info(s"[Export $id] status set to $status")
    val field = for { o <- table if o.id === id } yield (o.status, o.baseUrl, o.updatedAt)
    run(field.update(status, url, UtcTime.now).map(_ > 0))
  }
}
