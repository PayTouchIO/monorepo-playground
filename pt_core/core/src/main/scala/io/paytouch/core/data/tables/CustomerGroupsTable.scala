package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.CustomerGroupRecord

class CustomerGroupsTable(tag: Tag) extends SlickMerchantTable[CustomerGroupRecord](tag, "customer_groups") {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def groupId = column[UUID]("group_id")
  def customerId = column[UUID]("customer_id")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      customerId,
      groupId,
      createdAt,
      updatedAt,
    ).<>(CustomerGroupRecord.tupled, CustomerGroupRecord.unapply)
}
