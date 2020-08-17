package io.paytouch.ordering.data.tables

import java.time.ZonedDateTime
import java.util.{ Currency, UUID }

import io.paytouch.ordering.clients.paytouch.core.entities.ImageUrls
import io.paytouch.ordering.data.driver.PaytouchPostgresDriver.api._
import io.paytouch.ordering.data.model.StoreRecord
import io.paytouch.ordering.data.tables.features.{ ActiveColumn, LocationIdColumn, SlickTable }
import io.paytouch.ordering.entities.{ PaymentMethod, StoreContext }
import slick.lifted.Tag

class StoresTable(tag: Tag) extends SlickTable[StoreRecord](tag, "stores") with ActiveColumn with LocationIdColumn {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def locationId = column[UUID]("location_id")
  def currency = column[Currency]("currency")
  def urlSlug = column[String]("url_slug")
  def catalogId = column[UUID]("catalog_id")
  def active = column[Boolean]("active")
  def description = column[Option[String]]("description")
  def heroBgColor = column[Option[String]]("hero_bg_color")
  def heroImageUrls = column[Seq[ImageUrls]]("hero_image_urls")
  def logoImageUrls = column[Seq[ImageUrls]]("logo_image_urls")
  def takeOutEnabled = column[Boolean]("take_out_enabled")
  def takeOutStopMinsBeforeClosing = column[Option[Int]]("take_out_stop_mins_before_closing")
  def deliveryEnabled = column[Boolean]("delivery_enabled")
  def deliveryMinAmount = column[Option[BigDecimal]]("delivery_min_amount")
  def deliveryMaxAmount = column[Option[BigDecimal]]("delivery_max_amount")
  def deliveryMaxDistance = column[Option[BigDecimal]]("delivery_max_distance")
  def deliveryStopMinsBeforeClosing = column[Option[Int]]("delivery_stop_mins_before_closing")
  def deliveryFeeAmount = column[Option[BigDecimal]]("delivery_fee_amount")
  def paymentMethods = column[Seq[PaymentMethod]]("payment_methods")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def storeContext =
    (
      id,
      currency,
      merchantId,
      locationId,
      deliveryFeeAmount,
      deliveryMinAmount,
      deliveryMaxAmount,
      paymentMethods,
    ).<>((StoreContext.apply _).tupled, StoreContext.unapply)

  def * =
    (
      id,
      merchantId,
      locationId,
      currency,
      urlSlug,
      catalogId,
      active,
      description,
      heroBgColor,
      heroImageUrls,
      logoImageUrls,
      takeOutEnabled,
      takeOutStopMinsBeforeClosing,
      deliveryEnabled,
      deliveryMinAmount,
      deliveryMaxAmount,
      deliveryMaxDistance,
      deliveryStopMinsBeforeClosing,
      deliveryFeeAmount,
      paymentMethods,
      createdAt,
      updatedAt,
    ).<>(StoreRecord.tupled, StoreRecord.unapply)
}
