package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.entities.enums.ExposedName

final case class LocationEmailReceipt(
    locationId: UUID,
    headerColor: Option[String],
    locationName: Option[String],
    address: Address,
    includeItemDescription: Boolean,
    websiteUrl: Option[String],
    facebookUrl: Option[String],
    twitterUrl: Option[String],
    imageUrls: Option[ImageUrls],
  ) extends ExposedEntity {
  val classShortName = ExposedName.LocationEmailReceipt
}

final case class LocationEmailReceiptUpdate(
    headerColor: Option[String],
    locationName: Option[String],
    address: AddressUpsertion = AddressUpsertion.empty,
    includeItemDescription: Option[Boolean],
    websiteUrl: Option[String],
    facebookUrl: Option[String],
    twitterUrl: Option[String],
    imageUploadId: ResettableUUID,
  ) extends UpdateEntity[LocationEmailReceipt]
