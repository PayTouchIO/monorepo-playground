package io.paytouch.ordering.data.tables

import java.time.{ LocalTime, ZonedDateTime }
import java.util.{ Currency, UUID }

import slick.lifted.Tag
import slickless._

import shapeless._

import io.paytouch.ordering.data.driver.PaytouchPostgresDriver.api._
import io.paytouch.ordering.data.model.CartRecord
import io.paytouch.ordering.data.tables.features.SlickStoreTable
import io.paytouch.ordering.entities.Address
import io.paytouch.ordering.entities.enums._
import io.paytouch.ordering.entities.GiftCardPassApplied

class CartsTable(tag: Tag) extends SlickStoreTable[CartRecord](tag, "carts") {
  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")
  def storeId = column[UUID]("store_id")

  def orderId = column[Option[UUID]]("order_id")
  def orderNumber = column[Option[String]]("order_number")
  def paymentProcessor = column[Option[PaymentProcessor]]("payment_processor")
  def paymentMethodType = column[Option[PaymentMethodType]]("payment_method_type")
  def currency = column[Currency]("currency")

  def totalAmount = column[BigDecimal]("total_amount")
  def totalAmountWithoutGiftCards = column[BigDecimal]("total_amount_without_gift_cards")
  def subtotalAmount = column[BigDecimal]("subtotal_amount")
  def taxAmount = column[BigDecimal]("tax_amount")
  def tipAmount = column[BigDecimal]("tip_amount")
  def deliveryFeeAmount = column[Option[BigDecimal]]("delivery_fee_amount")
  def phoneNumber = column[Option[String]]("phone_number")
  def email = column[Option[String]]("email")
  def firstName = column[Option[String]]("first_name")
  def lastName = column[Option[String]]("last_name")
  def deliveryAddressLine1 = column[Option[String]]("delivery_address_line_1")
  def deliveryAddressLine2 = column[Option[String]]("delivery_address_line_2")
  def deliveryCity = column[Option[String]]("delivery_city")
  def deliveryState = column[Option[String]]("delivery_state")
  def deliveryCountry = column[Option[String]]("delivery_country")
  def deliveryPostalCode = column[Option[String]]("delivery_postal_code")
  def orderType = column[OrderType]("order_type")
  def prepareBy = column[Option[LocalTime]]("prepare_by")
  def drivingDistanceInMeters = column[Option[BigDecimal]]("driving_distance_in_meters")
  def estimatedDrivingTimeInMins = column[Option[Int]]("estimated_driving_time_in_mins")
  def storeAddress = column[Option[Address]]("store_address")
  def status = column[CartStatus]("status")
  def appliedGiftCardPasses = column[Seq[GiftCardPassApplied]]("applied_gift_card_passes")
  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (id ::
      merchantId ::
      storeId ::
      orderId ::
      orderNumber ::
      paymentProcessor ::
      paymentMethodType ::
      currency ::
      totalAmount ::
      totalAmountWithoutGiftCards ::
      subtotalAmount ::
      taxAmount ::
      tipAmount ::
      deliveryFeeAmount ::
      phoneNumber ::
      email ::
      firstName ::
      lastName ::
      deliveryAddressLine1 ::
      deliveryAddressLine2 ::
      deliveryCity ::
      deliveryState ::
      deliveryCountry ::
      deliveryPostalCode ::
      orderType ::
      prepareBy ::
      drivingDistanceInMeters ::
      estimatedDrivingTimeInMins ::
      storeAddress ::
      status ::
      appliedGiftCardPasses ::
      createdAt ::
      updatedAt :: HNil).mappedWith(Generic[CartRecord])
}
