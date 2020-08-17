package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.Availabilities

final case class CatalogCategoryCreation(
    name: String,
    description: ResettableString,
    catalogId: UUID,
    position: Option[Int],
    availabilities: Option[Availabilities],
    locationOverrides: Option[Map[UUID, Option[CategoryLocationUpdate]]],
  ) extends CreationEntity[Category, CatalogCategoryUpdate] {
  def asUpdate =
    CatalogCategoryUpdate(
      name = Some(name),
      description = description,
      catalogId = Some(catalogId),
      position = position,
      availabilities = availabilities,
      locationOverrides = locationOverrides,
    )
}

object CatalogCategoryCreation extends ToCategoryCreation[CatalogCategoryCreation] {
  def convert(t: CatalogCategoryCreation)(implicit user: UserContext) =
    CategoryCreation(
      name = t.name,
      description = t.description,
      avatarBgColor = None,
      imageUploadIds = Seq.empty,
      position = t.position,
      parentCategoryId = None,
      catalogId = Some(t.catalogId),
      subcategories = Seq.empty,
      locationOverrides = t.locationOverrides.getOrElse(Map.empty),
      availabilities = t.availabilities,
    )
}

final case class CatalogCategoryUpdate(
    name: Option[String],
    description: ResettableString,
    catalogId: Option[UUID],
    position: Option[Int],
    availabilities: Option[Availabilities],
    locationOverrides: Option[Map[UUID, Option[CategoryLocationUpdate]]],
  ) extends UpdateEntity[Category]

object CatalogCategoryUpdate extends ToCategoryUpdate[CatalogCategoryUpdate] {
  def convert(t: CatalogCategoryUpdate)(implicit user: UserContext) =
    CategoryUpdate(
      name = t.name,
      description = t.description,
      avatarBgColor = None,
      imageUploadIds = None,
      position = t.position,
      parentCategoryId = None,
      catalogId = t.catalogId,
      subcategories = Seq.empty,
      locationOverrides = t.locationOverrides.getOrElse(Map.empty),
      availabilities = t.availabilities,
    )
}
