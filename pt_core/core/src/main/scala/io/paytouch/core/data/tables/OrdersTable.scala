package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.{ Currency, UUID }

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.OptLocationIdColumn
import io.paytouch.core.data.model.enums._
import io.paytouch.core.data.model.{ CustomerNote, MerchantNote, OrderRecord, StatusTransition }
import io.paytouch.core.entities.Seating
import shapeless.{ Generic, HNil }
import slickless._

class OrdersTable(tag: Tag) extends SlickMerchantTable[OrderRecord](tag, "orders") with OptLocationIdColumn {

  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")

  def locationId = column[Option[UUID]]("location_id")
  def deviceId = column[Option[UUID]]("device_id")
  def userId = column[Option[UUID]]("user_id")
  def customerId = column[Option[UUID]]("customer_id")
  def deliveryAddressId = column[Option[UUID]]("delivery_address_id")
  def onlineOrderAttributeId = column[Option[UUID]]("online_order_attribute_id")

  def number = column[Option[String]]("number")
  def orderTag = column[Option[String]]("tag")
  def source = column[Option[Source]]("source")
  def `type` = column[Option[OrderType]]("type")
  def paymentType = column[Option[OrderPaymentType]]("payment_type")

  def totalAmount = column[Option[BigDecimal]]("total_amount")
  def subtotalAmount = column[Option[BigDecimal]]("subtotal_amount")
  def discountAmount = column[Option[BigDecimal]]("discount_amount")

  def taxAmount = column[Option[BigDecimal]]("tax_amount")
  def tipAmount = column[Option[BigDecimal]]("tip_amount")
  def ticketDiscountAmount = column[Option[BigDecimal]]("ticket_discount_amount")
  def deliveryFeeAmount = column[Option[BigDecimal]]("delivery_fee_amount")

  def customerNotes = column[Seq[CustomerNote]]("customer_notes")
  def merchantNotes = column[Seq[MerchantNote]]("merchant_notes")

  def paymentStatus = column[Option[PaymentStatus]]("payment_status")
  def status = column[Option[OrderStatus]]("status")
  def fulfillmentStatus = column[Option[FulfillmentStatus]]("fulfillment_status")
  def statusTransitions = column[Seq[StatusTransition]]("status_transitions")
  def isInvoice = column[Boolean]("is_invoice")
  def isFiscal = column[Boolean]("is_fiscal")

  def version = column[Int]("version")
  def seating = column[Option[Seating]]("seating")
  def seatingJson = column[Option[JValue]]("seating")

  def deliveryProvider = column[Option[DeliveryProvider]]("delivery_provider")
  def deliveryProviderId = column[Option[String]]("delivery_provider_id")
  def deliveryProviderNumber = column[Option[String]]("delivery_provider_number")

  def receivedAt = column[ZonedDateTime]("received_at")
  def completedAt = column[Option[ZonedDateTime]]("completed_at")
  def receivedAtTz = column[ZonedDateTime]("received_at_tz")
  def completedAtTz = column[Option[ZonedDateTime]]("completed_at_tz")
  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * = {
    val orderGeneric = Generic[OrderRecord]
    (id :: merchantId ::
      locationId :: deviceId :: userId :: customerId :: deliveryAddressId :: onlineOrderAttributeId ::
      number :: orderTag :: source :: `type` :: paymentType ::
      totalAmount :: subtotalAmount :: discountAmount ::
      taxAmount :: tipAmount :: ticketDiscountAmount :: deliveryFeeAmount :: customerNotes :: merchantNotes ::
      paymentStatus :: status :: fulfillmentStatus :: statusTransitions :: isInvoice :: isFiscal :: version :: seating ::
      deliveryProvider :: deliveryProviderId :: deliveryProviderNumber ::
      receivedAt :: receivedAtTz :: completedAt :: completedAtTz :: createdAt :: updatedAt :: HNil).<>(
      (dbRow: orderGeneric.Repr) => orderGeneric.from(dbRow),
      (caseClass: OrderRecord) => Some(orderGeneric.to(caseClass)),
    )
  }
}
