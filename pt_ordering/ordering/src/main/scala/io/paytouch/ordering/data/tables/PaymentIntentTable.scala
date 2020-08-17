package io.paytouch.ordering.data.tables

import java.time.{ LocalTime, ZonedDateTime }
import java.util.{ Currency, UUID }

import akka.http.scaladsl.model.Uri

import io.paytouch.ordering.data.driver.PaytouchPostgresDriver.api._
import io.paytouch.ordering.data.model.PaymentIntentRecord
import io.paytouch.ordering.data.tables.features.SlickTable
import io.paytouch.ordering.entities.PaymentIntentMetadata
import io.paytouch.ordering.entities.enums.{ PaymentIntentStatus, PaymentMethodType }
import shapeless.{ Generic, HNil }
import slick.lifted.Tag
import slickless._

class PaymentIntentsTable(tag: Tag) extends SlickTable[PaymentIntentRecord](tag, "payment_intents") {

  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")
  def orderId = column[UUID]("order_id")
  def orderItemIds = column[Seq[UUID]]("order_item_ids")

  def subtotalAmount = column[BigDecimal]("subtotal_amount")
  def taxAmount = column[BigDecimal]("tax_amount")
  def tipAmount = column[BigDecimal]("tip_amount")
  def totalAmount = column[BigDecimal]("total_amount")

  def paymentMethodType = column[PaymentMethodType]("payment_method_type")

  def successReturnUrl = column[Uri]("success_return_url")
  def failureReturnUrl = column[Uri]("failure_return_url")

  def status = column[PaymentIntentStatus]("status")
  def metadata = column[PaymentIntentMetadata]("metadata")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (id ::
      merchantId ::
      orderId ::
      orderItemIds ::
      subtotalAmount ::
      taxAmount ::
      tipAmount ::
      totalAmount ::
      paymentMethodType ::
      successReturnUrl ::
      failureReturnUrl ::
      status ::
      metadata ::
      createdAt ::
      updatedAt :: HNil).mappedWith(Generic[PaymentIntentRecord])

}
