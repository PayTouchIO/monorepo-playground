package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.{ Currency, UUID }

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.OrderItemRecord
import io.paytouch.core.data.model.enums.{ ArticleType, PaymentStatus, UnitType }

class OrderItemsTable(tag: Tag) extends SlickMerchantTable[OrderItemRecord](tag, "order_items") {
  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")

  def orderId = column[UUID]("order_id")
  def productId = column[Option[UUID]]("product_id")

  def productName = column[Option[String]]("product_name")
  def productDescription = column[Option[String]]("product_description")
  def productType = column[Option[ArticleType]]("product_type")
  def quantity = column[Option[BigDecimal]]("quantity")
  def unit = column[Option[UnitType]]("unit")
  def paymentStatus = column[Option[PaymentStatus]]("payment_status")

  def priceAmount = column[Option[BigDecimal]]("price_amount")
  def costAmount = column[Option[BigDecimal]]("cost_amount")
  def discountAmount = column[Option[BigDecimal]]("discount_amount")
  def taxAmount = column[Option[BigDecimal]]("tax_amount")
  def basePriceAmount = column[Option[BigDecimal]]("base_price_amount")
  def calculatedPriceAmount = column[Option[BigDecimal]]("calculated_price_amount")
  def totalPriceAmount = column[Option[BigDecimal]]("total_price_amount")

  def notes = column[Option[String]]("notes")
  def giftCardPassRecipientEmail = column[Option[String]]("gift_card_pass_recipient_email")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      orderId,
      productId,
      productName,
      productDescription,
      productType,
      quantity,
      unit,
      paymentStatus,
      priceAmount,
      costAmount,
      discountAmount,
      taxAmount,
      basePriceAmount,
      calculatedPriceAmount,
      totalPriceAmount,
      notes,
      giftCardPassRecipientEmail,
      createdAt,
      updatedAt,
    ).<>(OrderItemRecord.tupled, OrderItemRecord.unapply)
}
