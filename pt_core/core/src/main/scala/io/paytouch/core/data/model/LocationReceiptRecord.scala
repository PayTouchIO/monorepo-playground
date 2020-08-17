package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.entities.ResettableString

final case class AddressCodes(stateCode: Option[String], countryCode: Option[String]) {
  def toResettable: ResettableCodes =
    ResettableCodes(
      stateCode = stateCode,
      countryCode = countryCode,
    )
}
final case class ResettableCodes(stateCode: ResettableString, countryCode: ResettableString) {
  def toCodes: AddressCodes =
    AddressCodes(
      stateCode = stateCode,
      countryCode = countryCode,
    )
}

final case class LocationReceiptRecord(
    id: UUID,
    merchantId: UUID,
    locationId: UUID,
    locationName: Option[String],
    headerColor: Option[String],
    addressLine1: Option[String],
    addressLine2: Option[String],
    city: Option[String],
    state: Option[String],
    country: Option[String],
    codes: AddressCodes,
    postalCode: Option[String],
    phoneNumber: Option[String],
    websiteUrl: Option[String],
    facebookUrl: Option[String],
    twitterUrl: Option[String],
    showCustomText: Boolean,
    customText: Option[String],
    showReturnPolicy: Boolean,
    returnPolicyText: Option[String],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord
       with SlickOneToOneWithLocationRecord {
  def stateCode: Option[String] = codes.stateCode
  def countryCode: Option[String] = codes.countryCode
}

case class LocationReceiptUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    locationId: Option[UUID],
    locationName: ResettableString,
    headerColor: ResettableString,
    addressLine1: ResettableString,
    addressLine2: ResettableString,
    city: ResettableString,
    state: ResettableString,
    country: ResettableString,
    codes: ResettableCodes,
    postalCode: ResettableString,
    phoneNumber: ResettableString,
    websiteUrl: ResettableString,
    facebookUrl: ResettableString,
    twitterUrl: ResettableString,
    showCustomText: Option[Boolean],
    customText: ResettableString,
    showReturnPolicy: Option[Boolean],
    returnPolicyText: ResettableString,
  ) extends SlickMerchantUpdate[LocationReceiptRecord] {
  def countryCode: ResettableString = codes.countryCode
  def stateCode: ResettableString = codes.stateCode

  def toRecord: LocationReceiptRecord = {
    require(merchantId.isDefined, s"Impossible to convert LocationReceiptUpdate without a merchant id. [$this]")
    require(locationId.isDefined, s"Impossible to convert LocationReceiptUpdate without a location id. [$this]")

    LocationReceiptRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      locationId = locationId.get,
      locationName = locationName,
      headerColor = headerColor,
      addressLine1 = addressLine1,
      addressLine2 = addressLine2,
      city = city,
      state = state,
      country = country,
      codes = codes.toCodes,
      postalCode = postalCode,
      phoneNumber = phoneNumber,
      websiteUrl = websiteUrl,
      facebookUrl = facebookUrl,
      twitterUrl = twitterUrl,
      showCustomText = showCustomText.getOrElse(false),
      customText = customText,
      showReturnPolicy = showReturnPolicy.getOrElse(false),
      returnPolicyText = returnPolicyText,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: LocationReceiptRecord): LocationReceiptRecord =
    LocationReceiptRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      locationId = locationId.getOrElse(record.locationId),
      locationName = locationName.getOrElse(record.locationName),
      headerColor = headerColor.getOrElse(record.headerColor),
      addressLine1 = addressLine1.getOrElse(record.addressLine1),
      addressLine2 = addressLine2.getOrElse(record.addressLine2),
      city = city.getOrElse(record.city),
      state = state.getOrElse(record.state),
      country = country.getOrElse(record.country),
      codes = AddressCodes(
        stateCode = stateCode.getOrElse(record.stateCode),
        countryCode = countryCode.getOrElse(record.countryCode),
      ),
      postalCode = postalCode.getOrElse(record.postalCode),
      phoneNumber = phoneNumber.getOrElse(record.phoneNumber),
      websiteUrl = websiteUrl.getOrElse(record.websiteUrl),
      facebookUrl = facebookUrl.getOrElse(record.facebookUrl),
      twitterUrl = twitterUrl.getOrElse(record.twitterUrl),
      showCustomText = showCustomText.getOrElse(record.showCustomText),
      customText = customText.getOrElse(record.customText),
      showReturnPolicy = showReturnPolicy.getOrElse(record.showReturnPolicy),
      returnPolicyText = returnPolicyText.getOrElse(record.returnPolicyText),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}

object LocationReceiptUpdate {
  def defaultUpdate(merchantId: UUID, locationId: UUID) =
    LocationReceiptUpdate(
      id = None,
      merchantId = Some(merchantId),
      locationId = Some(locationId),
      locationName = None,
      headerColor = None,
      addressLine1 = None,
      addressLine2 = None,
      city = None,
      state = None,
      country = None,
      codes = ResettableCodes(None, None),
      postalCode = None,
      phoneNumber = None,
      websiteUrl = None,
      facebookUrl = None,
      twitterUrl = None,
      showCustomText = None,
      customText = None,
      showReturnPolicy = None,
      returnPolicyText = None,
    )
}
