package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.MerchantConfigurationRecord

class MerchantConfigurationsTable(tag: Tag)
    extends SlickMerchantTable[MerchantConfigurationRecord](tag, "merchant_configurations") {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (id, merchantId, createdAt, updatedAt).<>(MerchantConfigurationRecord.tupled, MerchantConfigurationRecord.unapply)
}
