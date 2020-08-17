package io.paytouch.ordering.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import slick.lifted.Tag

import io.paytouch.ordering.clients.paytouch.core.entities.enums.CartItemType
import io.paytouch.ordering.data.driver.PaytouchPostgresDriver.api._
import io.paytouch.ordering.data.model.CartItemRecord
import io.paytouch.ordering.data.tables.features.{ CartIdColumn, SlickStoreTable }
import io.paytouch.ordering.entities.CartItemBundleSet
import io.paytouch.ordering.entities.enums.UnitType
import io.paytouch.ordering.entities.GiftCardData

final class CartItemsTable(tag: Tag) extends SlickStoreTable[CartItemRecord](tag, "cart_items") with CartIdColumn {
  def id = column[UUID]("id", O.PrimaryKey)
  def storeId = column[UUID]("store_id")

  def cartId = column[UUID]("cart_id")
  def productId = column[UUID]("product_id")
  def productName = column[String]("product_name")
  def productDescription = column[Option[String]]("product_description")
  def quantity = column[BigDecimal]("quantity")
  def unit = column[UnitType]("unit")
  def priceAmount = column[BigDecimal]("price_amount")
  def costAmount = column[Option[BigDecimal]]("cost_amount")
  def taxAmount = column[BigDecimal]("tax_amount")
  def calculatedPriceAmount = column[BigDecimal]("calculated_price_amount")
  def totalPriceAmount = column[BigDecimal]("total_price_amount")
  def notes = column[Option[String]]("notes")
  def bundleSets = column[Option[Seq[CartItemBundleSet]]]("bundle_sets")
  def `type` = column[CartItemType]("type")
  def giftCardData = column[Option[GiftCardData]]("gift_card_data")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      storeId,
      cartId,
      productId,
      productName,
      productDescription,
      quantity,
      unit,
      priceAmount,
      costAmount,
      taxAmount,
      calculatedPriceAmount,
      totalPriceAmount,
      notes,
      bundleSets,
      `type`,
      giftCardData,
      createdAt,
      updatedAt,
    ).<>(CartItemRecord.tupled, CartItemRecord.unapply)
}
