package io.paytouch.ordering.clients.paytouch.core.entities

import java.util.UUID

import io.paytouch.ordering.clients.paytouch.core.entities.enums.Day

final case class Catalog(
    id: UUID,
    name: String,
    productsCount: Option[Int],
    locationOverrides: Option[Map[UUID, CatalogLocation]],
  )

final case class CatalogLocation(availabilities: CatalogLocation.Availabilities)
object CatalogLocation {
  private type Availabilities = Map[Day, Seq[Availability]]
}
