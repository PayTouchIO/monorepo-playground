package io.paytouch.ordering.clients.paytouch.core.entities

import java.util.UUID

import io.paytouch.ordering.clients.paytouch.core.entities.enums.ImageType

final case class CoreIds(
    locationIds: Seq[UUID] = Seq.empty,
    catalogIds: Seq[UUID] = Seq.empty,
    imageUploadIds: Map[ImageType, Seq[UUID]] = Map.empty,
  )
