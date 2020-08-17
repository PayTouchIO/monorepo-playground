package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.OrderDeliveryAddressRecord

class OrderDeliveryAddressesTable(tag: Tag)
    extends SlickMerchantTable[OrderDeliveryAddressRecord](tag, "order_delivery_addresses") {

  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")
  def firstName = column[Option[String]]("first_name")
  def lastName = column[Option[String]]("last_name")
  def addressLine1 = column[Option[String]]("address_line_1")
  def addressLine2 = column[Option[String]]("address_line_2")
  def city = column[Option[String]]("city")
  def state = column[Option[String]]("state")
  def country = column[Option[String]]("country")
  def stateCode = column[Option[String]]("state_code")
  def countryCode = column[Option[String]]("country_code")
  def postalCode = column[Option[String]]("postal_code")
  def drivingDistanceInMeters = column[Option[BigDecimal]]("driving_distance_in_meters")
  def estimatedDrivingTimeInMins = column[Option[Int]]("estimated_driving_time_in_mins")
  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      firstName,
      lastName,
      addressLine1,
      addressLine2,
      city,
      state,
      country,
      stateCode,
      countryCode,
      postalCode,
      drivingDistanceInMeters,
      estimatedDrivingTimeInMins,
      createdAt,
      updatedAt,
    ).<>(OrderDeliveryAddressRecord.tupled, OrderDeliveryAddressRecord.unapply)
}
