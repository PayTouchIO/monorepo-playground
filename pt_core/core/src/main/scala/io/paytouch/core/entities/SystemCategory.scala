package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.entities.enums.ExposedName

final case class SystemCategoryCreation(
    name: String,
    description: ResettableString,
    avatarBgColor: ResettableString,
    imageUploadIds: Seq[UUID],
    position: Option[Int],
    parentCategoryId: Option[UUID],
    subcategories: Seq[SubcategoryUpsertion],
    locationOverrides: Map[UUID, Option[CategoryLocationUpdate]],
  ) extends CreationEntity[Category, SystemCategoryUpdate] {
  def asUpdate =
    SystemCategoryUpdate(
      name = Some(name),
      description = description,
      avatarBgColor = avatarBgColor,
      imageUploadIds = Some(imageUploadIds),
      position = position,
      parentCategoryId = parentCategoryId,
      subcategories = subcategories,
      locationOverrides = locationOverrides,
    )
}

object SystemCategoryCreation extends ToCategoryCreation[SystemCategoryCreation] {
  def convert(t: SystemCategoryCreation)(implicit user: UserContext) =
    CategoryCreation(
      name = t.name,
      description = t.description,
      avatarBgColor = t.avatarBgColor,
      imageUploadIds = t.imageUploadIds,
      position = t.position,
      parentCategoryId = t.parentCategoryId,
      catalogId = None,
      subcategories = t.subcategories,
      locationOverrides = t.locationOverrides,
      availabilities = None,
    )
}

final case class SystemCategoryUpdate(
    name: Option[String],
    description: ResettableString,
    avatarBgColor: ResettableString,
    imageUploadIds: Option[Seq[UUID]],
    position: Option[Int],
    parentCategoryId: Option[UUID],
    subcategories: Seq[SubcategoryUpsertion],
    locationOverrides: Map[UUID, Option[CategoryLocationUpdate]],
  ) extends UpdateEntity[Category]

object SystemCategoryUpdate extends ToCategoryUpdate[SystemCategoryUpdate] {
  def convert(t: SystemCategoryUpdate)(implicit user: UserContext) =
    CategoryUpdate(
      name = t.name,
      description = t.description,
      avatarBgColor = t.avatarBgColor,
      imageUploadIds = t.imageUploadIds,
      position = t.position,
      parentCategoryId = t.parentCategoryId,
      catalogId = None,
      subcategories = t.subcategories,
      locationOverrides = t.locationOverrides,
      availabilities = None,
    )
}
