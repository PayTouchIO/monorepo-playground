package io.paytouch.core.data.model.upsertions

import io.paytouch.core.data.model._

final case class LocationSettingsUpsertion(
    locationSettings: LocationSettingsUpdate,
    emailReceiptUpdate: Option[LocationEmailReceiptUpsertion],
    printReceiptUpdate: Option[LocationPrintReceiptUpsertion],
    receiptUpdate: Option[LocationReceiptUpsertion],
    splashImageUploads: Option[Seq[ImageUploadUpdate]],
  ) extends UpsertionModel[LocationSettingsRecord]
