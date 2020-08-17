package io.paytouch.core.data.model.upsertions

import io.paytouch.core.data.model.{ ImageUploadUpdate, LocationPrintReceiptUpdate }

final case class LocationPrintReceiptUpsertion(
    printReceiptUpdate: LocationPrintReceiptUpdate,
    imageUploadUpdates: Option[Seq[ImageUploadUpdate]],
  )
