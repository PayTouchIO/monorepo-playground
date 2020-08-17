package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.entities.{ ImageUrls, Store, UserContext }

trait StoreConversions {

  def toEntity(locationId: UUID, logoImageUrls: Seq[ImageUrls]): Store =
    Store(locationId = locationId, logoImageUrls = logoImageUrls)
}
