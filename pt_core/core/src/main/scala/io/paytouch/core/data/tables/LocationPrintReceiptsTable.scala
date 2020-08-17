package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.OneToOneLocationColumns
import io.paytouch.core.data.model.LocationPrintReceiptRecord

class LocationPrintReceiptsTable(tag: Tag)
    extends SlickMerchantTable[LocationPrintReceiptRecord](tag, "location_print_receipts")
       with OneToOneLocationColumns {
  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def locationId = column[UUID]("location_id")

  def headerColor = column[Option[String]]("header_color")
  def locationName = column[Option[String]]("location_name")
  def locationAddressLine1 = column[Option[String]]("location_address_line_1")
  def locationAddressLine2 = column[Option[String]]("location_address_line_2")

  def locationCity = column[Option[String]]("location_city")
  def locationState = column[Option[String]]("location_state")
  def locationCountry = column[Option[String]]("location_country")
  def locationStateCode = column[Option[String]]("location_state_code")
  def locationCountryCode = column[Option[String]]("location_country_code")
  def locationPostalCode = column[Option[String]]("location_postal_code")

  def includeItemDescription = column[Boolean]("include_item_description")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      locationId,
      headerColor,
      locationName,
      locationAddressLine1,
      locationAddressLine2,
      locationCity,
      locationState,
      locationCountry,
      locationStateCode,
      locationCountryCode,
      locationPostalCode,
      includeItemDescription,
      createdAt,
      updatedAt,
    ).<>(LocationPrintReceiptRecord.tupled, LocationPrintReceiptRecord.unapply)
}
