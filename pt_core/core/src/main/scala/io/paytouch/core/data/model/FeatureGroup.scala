package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.entities.MerchantFeatures

final case class FeatureGroupRecord(
    id: UUID,
    name: String,
    description: String,
    features: MerchantFeatures,
    allEnabled: Boolean,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickRecord
