package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.ManyItemsToManyLocationsColumns
import io.paytouch.core.data.model.CustomerLocationRecord

class CustomerLocationsTable(tag: Tag)
    extends SlickMerchantTable[CustomerLocationRecord](tag, "customer_locations")
       with ManyItemsToManyLocationsColumns {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def customerId = column[UUID]("customer_id")
  def itemId = customerId // interface for ManyItemsToManyLocationsColumns
  def locationId = column[UUID]("location_id")

  def totalVisits = column[Int]("total_visits")
  def totalSpendAmount = column[BigDecimal]("total_spend_amount")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      customerId,
      locationId,
      totalVisits,
      totalSpendAmount,
      createdAt,
      updatedAt,
    ).<>(CustomerLocationRecord.tupled, CustomerLocationRecord.unapply)
}
