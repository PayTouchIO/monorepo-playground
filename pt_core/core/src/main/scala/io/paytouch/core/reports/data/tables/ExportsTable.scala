package io.paytouch.core.reports.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.tables.SlickMerchantTable
import io.paytouch.core.reports.data.model.ExportRecord
import io.paytouch.core.reports.data.model.enums.ExportStatus

class ExportsTable(tag: Tag) extends SlickMerchantTable[ExportRecord](tag, "exports") {

  def id = column[UUID]("id", O.PrimaryKey)

  def `type` = column[String]("type")
  def merchantId = column[UUID]("merchant_id")

  def status = column[ExportStatus]("status")
  def baseUrl = column[Option[String]]("base_url")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (id, `type`, merchantId, status, baseUrl, createdAt, updatedAt).<>(ExportRecord.tupled, ExportRecord.unapply)
}
