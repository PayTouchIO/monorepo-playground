package io.paytouch.core.data.tables

import java.time.{ ZoneId, ZonedDateTime }
import java.util.{ Currency, UUID }

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.LocationRecord

class LocationsTable(tag: Tag) extends SlickSoftDeleteTable[LocationRecord](tag, "locations") {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def name = column[String]("name")
  def email = column[Option[String]]("email")
  def phoneNumber = column[Option[String]]("phone_number")
  def website = column[Option[String]]("website")
  def addressLine1 = column[Option[String]]("address_line_1")
  def addressLine2 = column[Option[String]]("address_line_2")
  def city = column[Option[String]]("city")
  def state = column[Option[String]]("state")
  def country = column[Option[String]]("country")
  def stateCode = column[Option[String]]("state_code")
  def countryCode = column[Option[String]]("country_code")
  def postalCode = column[Option[String]]("postal_code")
  def timezone = column[ZoneId]("timezone")
  def active = column[Boolean]("active")
  def dummyData = column[Boolean]("dummy_data")
  def latitude = column[Option[BigDecimal]]("latitude")
  def longitude = column[Option[BigDecimal]]("longitude")
  def deletedAt = column[Option[ZonedDateTime]]("deleted_at")
  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      name,
      email,
      phoneNumber,
      website,
      addressLine1,
      addressLine2,
      city,
      state,
      country,
      stateCode,
      countryCode,
      postalCode,
      timezone,
      active,
      dummyData,
      latitude,
      longitude,
      deletedAt,
      createdAt,
      updatedAt,
    ).<>(LocationRecord.tupled, LocationRecord.unapply)

}
