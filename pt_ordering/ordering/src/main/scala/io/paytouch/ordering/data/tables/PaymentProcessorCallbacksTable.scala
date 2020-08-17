package io.paytouch.ordering.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import slick.lifted.Tag

import io.paytouch.ordering.data.driver.PaytouchPostgresDriver.api._
import io.paytouch.ordering.data.model.PaymentProcessorCallbackRecord
import io.paytouch.ordering.data.tables.features.SlickTable
import io.paytouch.ordering.entities.enums._

class PaymentProcessorCallbacksTable(tag: Tag)
    extends SlickTable[PaymentProcessorCallbackRecord](tag, "payment_processor_callbacks") {
  def id = column[UUID]("id", O.PrimaryKey)

  def paymentProcessor = column[PaymentProcessor]("payment_processor")
  def status = column[PaymentProcessorCallbackStatus]("status")
  def reference = column[Option[String]]("reference")
  def payload = column[JValue]("payload")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      paymentProcessor,
      status,
      reference,
      payload,
      createdAt,
      updatedAt,
    ).<>(PaymentProcessorCallbackRecord.tupled, PaymentProcessorCallbackRecord.unapply)
}
