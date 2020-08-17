package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.data.model.enums.ImageUploadType

final case class IdsToValidate(
    locationIds: Seq[UUID] = Seq.empty,
    catalogIds: Seq[UUID] = Seq.empty,
    imageUploadIds: Map[ImageUploadType, Seq[UUID]] = Map.empty,
  )
