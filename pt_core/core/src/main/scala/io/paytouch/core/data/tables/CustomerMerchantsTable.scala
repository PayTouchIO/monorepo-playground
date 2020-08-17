package io.paytouch.core.data.tables

import java.time.{ LocalDate, ZonedDateTime }
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.CustomerMerchantRecord
import io.paytouch.core.entities.BillingDetails
import io.paytouch.core.entities.enums.CustomerSource

class CustomerMerchantsTable(tag: Tag) extends SlickMerchantTable[CustomerMerchantRecord](tag, "customer_merchants") {
  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def customerId = column[UUID]("customer_id")

  def firstName = column[Option[String]]("first_name")
  def lastName = column[Option[String]]("last_name")
  def dob = column[Option[LocalDate]]("dob")
  def anniversary = column[Option[LocalDate]]("anniversary")
  def email = column[Option[String]]("email")
  def phoneNumber = column[Option[String]]("phone_number")

  def addressLine1 = column[Option[String]]("address_line_1")
  def addressLine2 = column[Option[String]]("address_line_2")
  def city = column[Option[String]]("city")
  def state = column[Option[String]]("state")
  def country = column[Option[String]]("country")
  def stateCode = column[Option[String]]("state_code")
  def countryCode = column[Option[String]]("country_code")
  def postalCode = column[Option[String]]("postal_code")
  def billingDetails = column[Option[BillingDetails]]("billing_details")

  def source = column[CustomerSource]("source")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      customerId,
      firstName,
      lastName,
      dob,
      anniversary,
      email,
      phoneNumber,
      addressLine1,
      addressLine2,
      city,
      state,
      country,
      stateCode,
      countryCode,
      postalCode,
      billingDetails,
      source,
      createdAt,
      updatedAt,
    ).<>(CustomerMerchantRecord.tupled, CustomerMerchantRecord.unapply)
}
