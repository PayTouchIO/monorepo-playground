package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.OptLocationIdColumn
import io.paytouch.core.data.model.OrderFeedbackRecord

class OrderFeedbacksTable(tag: Tag)
    extends SlickMerchantTable[OrderFeedbackRecord](tag, "order_feedbacks")
       with OptLocationIdColumn {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def orderId = column[UUID]("order_id")
  def locationId = column[Option[UUID]]("location_id")
  def customerId = column[UUID]("customer_id")

  def rating = column[Int]("rating")
  def body = column[String]("body")
  def read = column[Boolean]("read")

  def receivedAt = column[ZonedDateTime]("received_at")
  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      orderId,
      locationId,
      customerId,
      rating,
      body,
      read,
      receivedAt,
      createdAt,
      updatedAt,
    ).<>(OrderFeedbackRecord.tupled, OrderFeedbackRecord.unapply)
}
