package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.ModifierOptionRecord

class ModifierOptionsTable(tag: Tag) extends SlickMerchantTable[ModifierOptionRecord](tag, "modifier_options") {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def modifierSetId = column[UUID]("modifier_set_id")
  def name = column[String]("name")
  def priceAmount = column[BigDecimal]("price_amount")
  def position = column[Int]("position")
  def active = column[Boolean]("active")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      modifierSetId,
      name,
      priceAmount,
      position,
      active,
      createdAt,
      updatedAt,
    ).<>(ModifierOptionRecord.tupled, ModifierOptionRecord.unapply)
}
