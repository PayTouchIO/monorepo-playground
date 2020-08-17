package io.paytouch.ordering.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.ordering.data.driver.PaytouchPostgresDriver.api._
import io.paytouch.ordering.data.model.{ MerchantRecord, PaymentProcessorConfig }
import io.paytouch.ordering.data.tables.features.SlickTable
import io.paytouch.ordering.entities.enums.PaymentProcessor
import slick.lifted.Tag

class MerchantsTable(tag: Tag) extends SlickTable[MerchantRecord](tag, "merchants") {

  def id = column[UUID]("id", O.PrimaryKey)

  def urlSlug = column[String]("url_slug")

  def paymentProcessor = column[PaymentProcessor]("payment_processor")
  def paymentProcessorConfig = column[PaymentProcessorConfig]("payment_processor_config")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      urlSlug,
      paymentProcessor,
      paymentProcessorConfig,
      createdAt,
      updatedAt,
    ).<>(MerchantRecord.tupled, MerchantRecord.unapply)
}
