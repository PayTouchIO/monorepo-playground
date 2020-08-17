package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.CatalogRecord
import io.paytouch.core.entities.enums.CatalogType

class CatalogsTable(tag: Tag) extends SlickMerchantTable[CatalogRecord](tag, "catalogs") {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def name = column[String]("name")
  def `type` = column[CatalogType]("type")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * = (id, merchantId, name, `type`, createdAt, updatedAt).<>(CatalogRecord.tupled, CatalogRecord.unapply)
}
