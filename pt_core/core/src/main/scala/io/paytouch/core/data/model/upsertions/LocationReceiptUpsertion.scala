package io.paytouch.core.data.model.upsertions

import io.paytouch.core.data.model.{ ImageUploadUpdate, LocationReceiptUpdate }

final case class LocationReceiptUpsertion(
    receiptUpdate: LocationReceiptUpdate,
    emailImageUploadUpdates: Option[Seq[ImageUploadUpdate]],
    printImageUploadUpdates: Option[Seq[ImageUploadUpdate]],
  )
