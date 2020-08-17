package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions._
import io.paytouch.core.data.model.enums.KitchenType
import io.paytouch.core.data.model.KitchenRecord

class KitchensTable(tag: Tag)
    extends SlickSoftDeleteTable[KitchenRecord](tag, "kitchens")
       with OneToOneLocationColumns {
  def id = column[UUID]("id", O.PrimaryKey)

  override def merchantId = column[UUID]("merchant_id")
  override def locationId = column[UUID]("location_id")
  def name = column[String]("name")
  def `type` = column[KitchenType]("type")
  def active = column[Boolean]("active")
  def kdsEnabled = column[Boolean]("kds_enabled")
  def deletedAt = column[Option[ZonedDateTime]]("deleted_at")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      locationId,
      name,
      `type`,
      active,
      kdsEnabled,
      deletedAt,
      createdAt,
      updatedAt,
    ).<>(KitchenRecord.tupled, KitchenRecord.unapply)
}
