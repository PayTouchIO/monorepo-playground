package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.GroupRecord

class GroupsTable(tag: Tag) extends SlickMerchantTable[GroupRecord](tag, "groups") {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def name = column[String]("name")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * = (id, merchantId, name, createdAt, updatedAt).<>(GroupRecord.tupled, GroupRecord.unapply)
}
