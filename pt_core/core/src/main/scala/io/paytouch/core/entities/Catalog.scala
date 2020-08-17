package io.paytouch.core.entities

import java.util.UUID

import cats.implicits._

import io.paytouch.core.Availabilities
import io.paytouch.core.entities.enums.CatalogType
import io.paytouch.core.entities.enums.ExposedName

final case class Catalog(
    id: UUID,
    name: String,
    `type`: CatalogType,
    productsCount: Option[Int],
    categoriesCount: Option[Int],
    availabilities: Option[Availabilities],
    locationOverrides: Option[Map[UUID, CatalogLocation]],
  ) extends ExposedEntity {
  val classShortName = ExposedName.Catalog
}

final case class CatalogCreation(
    name: String,
    availabilities: Option[Availabilities],
  ) extends CreationEntity[Catalog, CatalogUpsertion] {
  def asUpdate = CatalogUpsertion(name = name.some, `type` = CatalogType.Menu.some, availabilities = availabilities)
}

final case class CatalogUpdate(name: Option[String], availabilities: Option[Availabilities]) {
  def asUpsertion = CatalogUpsertion(name = name, `type` = None, availabilities = availabilities)
}

final case class CatalogUpsertion(
    name: Option[String],
    `type`: Option[CatalogType],
    availabilities: Option[Availabilities],
  ) extends UpdateEntity[Catalog]
