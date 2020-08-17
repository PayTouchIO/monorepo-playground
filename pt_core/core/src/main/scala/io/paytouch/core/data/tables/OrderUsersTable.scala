package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.OrderUserRecord

class OrderUsersTable(tag: Tag) extends SlickMerchantTable[OrderUserRecord](tag, "order_users") {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def orderId = column[UUID]("order_id")
  def userId = column[UUID]("user_id")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (id, merchantId, orderId, userId, createdAt, updatedAt).<>(OrderUserRecord.tupled, OrderUserRecord.unapply)
}
