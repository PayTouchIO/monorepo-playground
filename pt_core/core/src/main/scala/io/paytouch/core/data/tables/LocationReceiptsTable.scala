package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.OneToOneLocationColumns
import io.paytouch.core.data.model._

class LocationReceiptsTable(tag: Tag)
    extends SlickMerchantTable[LocationReceiptRecord](tag, "location_receipts")
       with OneToOneLocationColumns {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def locationId = column[UUID]("location_id")

  def locationName = column[Option[String]]("location_name")
  def headerColor = column[Option[String]]("header_color")
  def addressLine1 = column[Option[String]]("address_line_1")
  def addressLine2 = column[Option[String]]("address_line_2")
  def city = column[Option[String]]("city")
  def state = column[Option[String]]("state")
  def country = column[Option[String]]("country")
  def stateCode = column[Option[String]]("state_code")
  def countryCode = column[Option[String]]("country_code")
  def postalCode = column[Option[String]]("postal_code")
  def phoneNumber = column[Option[String]]("phone_number")

  def websiteUrl = column[Option[String]]("website_url")
  def facebookUrl = column[Option[String]]("facebook_url")
  def twitterUrl = column[Option[String]]("twitter_url")

  def showCustomText = column[Boolean]("show_custom_text")
  def customText = column[Option[String]]("custom_text")
  def showReturnPolicy = column[Boolean]("show_return_policy")
  def returnPolicyText = column[Option[String]]("return_policy_text")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      locationId,
      locationName,
      headerColor,
      addressLine1,
      addressLine2,
      city,
      state,
      country,
      codes,
      postalCode,
      phoneNumber,
      websiteUrl,
      facebookUrl,
      twitterUrl,
      showCustomText,
      customText,
      showReturnPolicy,
      returnPolicyText,
      createdAt,
      updatedAt,
    ).<>(LocationReceiptRecord.tupled, LocationReceiptRecord.unapply)

  def codes = (stateCode, countryCode).<>(AddressCodes.tupled, AddressCodes.unapply)
}
