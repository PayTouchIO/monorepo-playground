package io.paytouch.core.data.model

import java.time.{ LocalDate, ZonedDateTime }
import java.util.UUID

final case class GlobalCustomerRecord(
    id: UUID,
    firstName: Option[String],
    lastName: Option[String],
    dob: Option[LocalDate],
    anniversary: Option[LocalDate],
    email: Option[String],
    phoneNumber: Option[String],
    addressLine1: Option[String],
    addressLine2: Option[String],
    city: Option[String],
    state: Option[String],
    stateCode: Option[String],
    countryCode: Option[String],
    country: Option[String],
    postalCode: Option[String],
    mobileStorefrontLastLogin: Option[ZonedDateTime],
    webStorefrontLastLogin: Option[ZonedDateTime],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickRecord

case class GlobalCustomerUpdate(
    id: Option[UUID],
    firstName: Option[String],
    lastName: Option[String],
    dob: Option[LocalDate],
    anniversary: Option[LocalDate],
    email: Option[String],
    phoneNumber: Option[String],
    addressLine1: Option[String],
    addressLine2: Option[String],
    city: Option[String],
    state: Option[String],
    country: Option[String],
    stateCode: Option[String],
    countryCode: Option[String],
    postalCode: Option[String],
    mobileStorefrontLastLogin: Option[ZonedDateTime],
    webStorefrontLastLogin: Option[ZonedDateTime],
  ) extends SlickUpdate[GlobalCustomerRecord] {

  def toRecord: GlobalCustomerRecord =
    GlobalCustomerRecord(
      id = id.getOrElse(UUID.randomUUID),
      firstName = firstName,
      lastName = lastName,
      dob = dob,
      anniversary = anniversary,
      email = email,
      phoneNumber = phoneNumber,
      addressLine1 = addressLine1,
      addressLine2 = addressLine2,
      city = city,
      state = state,
      country = country,
      stateCode = stateCode,
      countryCode = countryCode,
      postalCode = postalCode,
      mobileStorefrontLastLogin = mobileStorefrontLastLogin,
      webStorefrontLastLogin = webStorefrontLastLogin,
      createdAt = now,
      updatedAt = now,
    )

  def updateRecord(record: GlobalCustomerRecord): GlobalCustomerRecord =
    GlobalCustomerRecord(
      id = record.id, // we never change the id of a global customer
      firstName = firstName.orElse(record.firstName),
      lastName = lastName.orElse(record.lastName),
      dob = dob.orElse(record.dob),
      anniversary = anniversary.orElse(record.anniversary),
      email = record.email.orElse(email), // if email is already populated we do not update it
      phoneNumber = phoneNumber.orElse(record.phoneNumber),
      addressLine1 = addressLine1.orElse(record.addressLine1),
      addressLine2 = addressLine2.orElse(record.addressLine2),
      city = city.orElse(record.city),
      state = state.orElse(record.state),
      country = country.orElse(record.country),
      stateCode = stateCode.orElse(record.stateCode),
      countryCode = countryCode.orElse(record.countryCode),
      postalCode = postalCode.orElse(record.postalCode),
      mobileStorefrontLastLogin = mobileStorefrontLastLogin.orElse(record.mobileStorefrontLastLogin),
      webStorefrontLastLogin = webStorefrontLastLogin.orElse(record.webStorefrontLastLogin),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
