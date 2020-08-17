package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.entities._

case class OrderDeliveryAddressRecord(
    id: UUID,
    merchantId: UUID,
    firstName: Option[String],
    lastName: Option[String],
    addressLine1: Option[String],
    addressLine2: Option[String],
    city: Option[String],
    state: Option[String],
    country: Option[String],
    stateCode: Option[String],
    countryCode: Option[String],
    postalCode: Option[String],
    drivingDistanceInMeters: Option[BigDecimal],
    estimatedDrivingTimeInMins: Option[Int],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class OrderDeliveryAddressUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    firstName: ResettableString,
    lastName: ResettableString,
    addressLine1: ResettableString,
    addressLine2: ResettableString,
    city: ResettableString,
    state: ResettableString,
    country: ResettableString,
    stateCode: ResettableString,
    countryCode: ResettableString,
    postalCode: ResettableString,
    drivingDistanceInMeters: ResettableBigDecimal,
    estimatedDrivingTimeInMins: ResettableInt,
  ) extends SlickMerchantUpdate[OrderDeliveryAddressRecord] {
  def toRecord: OrderDeliveryAddressRecord = {
    require(merchantId.isDefined, s"Impossible to convert OrderDeliveryAddressUpdate without a merchant id. [$this]")

    OrderDeliveryAddressRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      firstName = firstName,
      lastName = lastName,
      addressLine1 = addressLine1,
      addressLine2 = addressLine2,
      city = city,
      state = state,
      country = country,
      stateCode = stateCode,
      countryCode = countryCode,
      postalCode = postalCode,
      drivingDistanceInMeters = drivingDistanceInMeters,
      estimatedDrivingTimeInMins = estimatedDrivingTimeInMins,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: OrderDeliveryAddressRecord): OrderDeliveryAddressRecord =
    OrderDeliveryAddressRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      firstName = firstName.getOrElse(record.firstName),
      lastName = lastName.getOrElse(record.lastName),
      addressLine1 = addressLine1.getOrElse(record.addressLine1),
      addressLine2 = addressLine2.getOrElse(record.addressLine2),
      city = city.getOrElse(record.city),
      state = state.getOrElse(record.state),
      country = country.getOrElse(record.country),
      stateCode = stateCode.getOrElse(record.stateCode),
      countryCode = countryCode.getOrElse(record.countryCode),
      postalCode = postalCode.getOrElse(record.postalCode),
      drivingDistanceInMeters = drivingDistanceInMeters.getOrElse(record.drivingDistanceInMeters),
      estimatedDrivingTimeInMins = estimatedDrivingTimeInMins.getOrElse(record.estimatedDrivingTimeInMins),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
