package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.entities.enums.ExposedName

final case class Store(locationId: UUID, logoImageUrls: Seq[ImageUrls]) extends ExposedEntity {
  val classShortName = ExposedName.Store
}
