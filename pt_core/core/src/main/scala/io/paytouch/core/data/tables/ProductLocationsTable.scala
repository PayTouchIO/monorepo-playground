package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.{ Currency, UUID }

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.{ ActiveColumn, ManyItemsToManyLocationsColumns, ProductIdColumn }
import io.paytouch.core.data.model.ProductLocationRecord
import io.paytouch.core.data.model.enums.UnitType

class ProductLocationsTable(tag: Tag)
    extends SlickMerchantTable[ProductLocationRecord](tag, "product_locations")
       with ProductIdColumn
       with ManyItemsToManyLocationsColumns
       with ActiveColumn {

  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")

  def productId = column[UUID]("product_id")
  def itemId = productId // interface for ManyItemsToManyLocationsColumns
  def locationId = column[UUID]("location_id")

  def priceAmount = column[BigDecimal]("price_amount")
  def costAmount = column[Option[BigDecimal]]("cost_amount")
  def averageCostAmount = column[Option[BigDecimal]]("avg_cost_amount")
  def unit = column[UnitType]("unit")
  def margin = column[Option[BigDecimal]]("margin")
  def active = column[Boolean]("active")
  def routeToKitchenId = column[Option[UUID]]("route_to_kitchen_id")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      productId,
      locationId,
      priceAmount,
      costAmount,
      averageCostAmount,
      unit,
      margin,
      active,
      routeToKitchenId,
      createdAt,
      updatedAt,
    ).<>(ProductLocationRecord.tupled, ProductLocationRecord.unapply)
}
