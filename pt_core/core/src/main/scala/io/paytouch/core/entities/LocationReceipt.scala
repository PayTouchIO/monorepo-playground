package io.paytouch.core.entities

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.entities.enums.ExposedName

final case class LocationReceipt(
    locationId: UUID,
    locationName: String,
    headerColor: Option[String],
    address: Address,
    phoneNumber: Option[String],
    websiteUrl: Option[String],
    facebookUrl: Option[String],
    twitterUrl: Option[String],
    showCustomText: Boolean,
    customText: Option[String],
    showReturnPolicy: Boolean,
    returnPolicyText: Option[String],
    emailImageUrls: Seq[ImageUrls],
    printImageUrls: Seq[ImageUrls],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends ExposedEntity {
  val classShortName = ExposedName.LocationReceipt
}

final case class LocationReceiptUpdate(
    locationName: ResettableString,
    headerColor: ResettableString,
    address: AddressUpsertion = AddressUpsertion.empty,
    phoneNumber: ResettableString,
    websiteUrl: ResettableString,
    facebookUrl: ResettableString,
    twitterUrl: ResettableString,
    showCustomText: Option[Boolean],
    customText: ResettableString,
    showReturnPolicy: Option[Boolean],
    returnPolicyText: ResettableString,
    emailImageUploadIds: Option[Seq[UUID]],
    printImageUploadIds: Option[Seq[UUID]],
  ) extends UpdateEntity[LocationReceipt]
