package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.entities.enums.ExposedName

final case class LocationPrintReceipt(
    locationId: UUID,
    headerColor: Option[String],
    locationName: Option[String],
    address: Address,
    includeItemDescription: Boolean,
    imageUrls: Option[ImageUrls],
  ) extends ExposedEntity {
  val classShortName = ExposedName.LocationPrintReceipt
}

final case class LocationPrintReceiptUpdate(
    headerColor: Option[String],
    locationName: Option[String],
    address: AddressUpsertion = AddressUpsertion.empty,
    includeItemDescription: Option[Boolean],
    imageUploadId: ResettableUUID,
  ) extends UpdateEntity[LocationPrintReceipt]
