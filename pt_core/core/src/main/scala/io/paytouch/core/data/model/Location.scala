package io.paytouch.core.data.model

import java.time.{ ZoneId, ZonedDateTime }
import java.util.{ Currency, UUID }

import cats.implicits._

import io.paytouch.core.entities._
import io.paytouch.core.services.UtilService

final case class LocationRecord(
    id: UUID,
    merchantId: UUID,
    name: String,
    email: Option[String],
    phoneNumber: Option[String],
    website: Option[String],
    addressLine1: Option[String],
    addressLine2: Option[String],
    city: Option[String],
    state: Option[String],
    country: Option[String],
    stateCode: Option[String],
    countryCode: Option[String],
    postalCode: Option[String],
    timezone: ZoneId,
    active: Boolean,
    dummyData: Boolean,
    latitude: Option[BigDecimal],
    longitude: Option[BigDecimal],
    deletedAt: Option[ZonedDateTime],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickSoftDeleteRecord

case class LocationUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    name: Option[String],
    email: Option[String],
    phoneNumber: ResettableString,
    website: ResettableString,
    addressLine1: ResettableString,
    addressLine2: ResettableString,
    city: ResettableString,
    state: ResettableString,
    country: Option[String],
    stateCode: ResettableString,
    countryCode: Option[String],
    postalCode: ResettableString,
    timezone: Option[ZoneId],
    active: Option[Boolean],
    dummyData: Option[Boolean],
    latitude: ResettableBigDecimal,
    longitude: ResettableBigDecimal,
    deletedAt: Option[ZonedDateTime],
  ) extends SlickSoftDeleteUpdate[LocationRecord] {
  def toRecord: LocationRecord = {
    require(merchantId.isDefined, s"Impossible to convert LocationUpdate without a merchant id. [$this]")
    require(name.isDefined, s"Impossible to convert LocationUpdate without a name. [$this]")
    require(timezone.isDefined, s"Impossible to convert LocationUpdate without a timezone. [$this]")

    LocationRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      name = name.get,
      email = email,
      phoneNumber = phoneNumber,
      website = website,
      addressLine1 = addressLine1,
      addressLine2 = addressLine2,
      city = city,
      state = state,
      country = country.orElse("US".some),
      stateCode = stateCode,
      countryCode = countryCode.orElse(UtilService.Geo.UnitedStates.code.some),
      postalCode = postalCode,
      timezone = timezone.get,
      active = active.getOrElse(true),
      dummyData = dummyData.getOrElse(false),
      latitude = latitude,
      longitude = longitude,
      deletedAt = deletedAt,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: LocationRecord): LocationRecord =
    LocationRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      name = name.getOrElse(record.name),
      email = email.orElse(record.email),
      phoneNumber = phoneNumber.getOrElse(record.phoneNumber),
      website = website.getOrElse(record.website),
      addressLine1 = addressLine1.getOrElse(record.addressLine1),
      addressLine2 = addressLine2.getOrElse(record.addressLine2),
      city = city.getOrElse(record.city),
      state = state.getOrElse(record.state),
      country = country.orElse(record.country),
      stateCode = stateCode.getOrElse(record.stateCode),
      countryCode = countryCode.orElse(record.countryCode),
      postalCode = postalCode.getOrElse(record.postalCode),
      timezone = timezone.getOrElse(record.timezone),
      active = active.getOrElse(record.active),
      dummyData = dummyData.getOrElse(record.dummyData),
      latitude = latitude.getOrElse(record.latitude),
      longitude = longitude.getOrElse(record.longitude),
      deletedAt = deletedAt.orElse(record.deletedAt),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
