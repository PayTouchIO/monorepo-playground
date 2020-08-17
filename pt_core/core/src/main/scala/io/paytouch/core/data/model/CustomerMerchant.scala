package io.paytouch.core.data.model

import java.time.{ LocalDate, ZonedDateTime }
import java.util.UUID

import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.CustomerSource

final case class CustomerMerchantRecord(
    private val _id: UUID,
    merchantId: UUID,
    customerId: UUID,
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
    billingDetails: Option[BillingDetails],
    source: CustomerSource,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord {
  def id = customerId
}

case class CustomerMerchantUpdate(
    private val _id: Option[UUID] = None,
    merchantId: Option[UUID],
    customerId: Option[UUID],
    firstName: ResettableString,
    lastName: ResettableString,
    dob: ResettableLocalDate,
    anniversary: ResettableLocalDate,
    email: ResettableString,
    phoneNumber: ResettableString,
    addressLine1: ResettableString,
    addressLine2: ResettableString,
    city: ResettableString,
    state: ResettableString,
    country: ResettableString,
    stateCode: ResettableString,
    countryCode: ResettableString,
    postalCode: ResettableString,
    billingDetails: ResettableBillingDetails,
    source: Option[CustomerSource],
  ) extends SlickMerchantUpdate[CustomerMerchantRecord] {
  def id = customerId

  def toRecord: CustomerMerchantRecord = {
    require(merchantId.isDefined, s"Impossible to convert CustomerMerchantUpdate without a merchant id. [$this]")
    require(customerId.isDefined, s"Impossible to convert CustomerMerchantUpdate without a customer id. [$this]")
    require(source.isDefined, s"Impossible to convert CustomerMerchantUpdate without a source. [$this]")

    CustomerMerchantRecord(
      _id = _id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      customerId = customerId.get,
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
      billingDetails = billingDetails,
      source = source.get,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: CustomerMerchantRecord): CustomerMerchantRecord =
    record.copy(
      merchantId = merchantId.getOrElse(record.merchantId),
      customerId = customerId.getOrElse(record.customerId),
      firstName = firstName.getOrElse(record.firstName),
      lastName = lastName.getOrElse(record.lastName),
      dob = dob.getOrElse(record.dob),
      anniversary = anniversary.getOrElse(record.anniversary),
      email = email.getOrElse(record.email),
      phoneNumber = phoneNumber.getOrElse(record.phoneNumber),
      addressLine1 = addressLine1.getOrElse(record.addressLine1),
      addressLine2 = addressLine2.getOrElse(record.addressLine2),
      city = city.getOrElse(record.city),
      state = state.getOrElse(record.state),
      country = country.getOrElse(record.country),
      stateCode = stateCode.getOrElse(record.stateCode),
      countryCode = countryCode.getOrElse(record.countryCode),
      postalCode = postalCode.getOrElse(record.postalCode),
      billingDetails = billingDetails.getOrElse(record.billingDetails),
      source = record.source, // Set only on creation
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
