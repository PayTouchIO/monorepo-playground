package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.Availabilities
import io.paytouch.core.entities.enums.ExposedName

final case class Category(
    id: UUID,
    name: String,
    merchantId: UUID,
    catalogId: UUID,
    description: Option[String],
    avatarBgColor: Option[String],
    avatarImageUrl: Option[String] = None, // TODO - remove once FE and iOS have migrated to new API
    avatarImageUrls: Seq[ImageUrls],
    position: Int,
    active: Option[Boolean],
    subcategories: Option[Seq[Category]],
    locationOverrides: Option[Map[UUID, CategoryLocation]],
    productsCount: Option[Int],
    availabilities: Option[Availabilities],
  ) extends ExposedEntity {
  val classShortName = ExposedName.Category
}

final case class CategoryCreation(
    name: String,
    description: ResettableString,
    avatarBgColor: ResettableString,
    imageUploadIds: Seq[UUID],
    position: Option[Int],
    catalogId: Option[UUID],
    parentCategoryId: Option[UUID],
    subcategories: Seq[SubcategoryUpsertion],
    locationOverrides: Map[UUID, Option[CategoryLocationUpdate]],
    availabilities: Option[Availabilities],
  ) extends CreationEntity[Category, CategoryUpdate] {
  def asUpdate =
    CategoryUpdate(
      name = Some(name),
      description = description,
      avatarBgColor = avatarBgColor,
      imageUploadIds = Some(imageUploadIds),
      position = position,
      parentCategoryId = parentCategoryId,
      catalogId = catalogId,
      subcategories = subcategories,
      locationOverrides = locationOverrides,
      availabilities = availabilities,
    )
}

final case class SubcategoryUpsertion(
    id: UUID,
    name: String,
    description: ResettableString,
    avatarBgColor: ResettableString,
    imageUploadIds: Option[Seq[UUID]],
    position: Option[Int],
    active: Option[Boolean],
  )

final case class CategoryUpdate(
    name: Option[String],
    description: ResettableString,
    avatarBgColor: ResettableString,
    imageUploadIds: Option[Seq[UUID]],
    position: Option[Int],
    parentCategoryId: Option[UUID],
    catalogId: Option[UUID],
    subcategories: Seq[SubcategoryUpsertion],
    locationOverrides: Map[UUID, Option[CategoryLocationUpdate]],
    availabilities: Option[Availabilities],
  ) extends UpdateEntity[Category]

trait ToCategoryCreation[T] {
  def convert(t: T)(implicit user: UserContext): CategoryCreation
}

trait ToCategoryUpdate[T] {
  def convert(t: T)(implicit user: UserContext): CategoryUpdate
}
