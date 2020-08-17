package io.paytouch.core.data.model.upsertions

import io.paytouch.core.data.model.{ ImageUploadUpdate, LocationEmailReceiptUpdate }

final case class LocationEmailReceiptUpsertion(
    emailReceiptUpdate: LocationEmailReceiptUpdate,
    imageUploadUpdates: Option[Seq[ImageUploadUpdate]],
  )
