package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.{ Availabilities, LocationOverridesPer }
import io.paytouch.core.data.model.{ CategoryUpdate => CategoryUpdateModel, _ }
import io.paytouch.core.entities.{ Category => CategoryEntity, CategoryUpdate => CategoryUpdateEntity, _ }

trait GenericCategoryConversions extends EntityConversion[CategoryRecord, CategoryEntity] with OrderingConversions {

  def fromRecordToEntity(record: CategoryRecord)(implicit user: UserContext): CategoryEntity =
    fromRecordAndOptionsToEntity(record, Seq.empty)

  def fromRecordsAndOptionsToEntities(
      categories: Seq[CategoryRecord],
      imageUrlsPerCategory: Map[CategoryRecord, Seq[ImageUrls]],
      subcategoriesPerCategory: Option[Map[UUID, Seq[CategoryEntity]]],
      locationOverridesPerCategory: Option[LocationOverridesPer[UUID, CategoryLocation]],
      productsCountPerCategory: Option[Map[UUID, Int]],
      availabilitiesPerCategory: Option[Map[UUID, Availabilities]],
    ): Seq[CategoryEntity] =
    categories.map { category =>
      val subcategories = subcategoriesPerCategory.map(_.getOrElse(category.id, Seq.empty))
      val locationOverrides = locationOverridesPerCategory.map(_.getOrElse(category.id, Map.empty))
      val productsCount = productsCountPerCategory.map(_.getOrElse(category.id, 0))
      val imageUrls = imageUrlsPerCategory.getOrElse(category, Seq.empty)
      val availabilities = availabilitiesPerCategory.map(_.getOrElse(category.id, Map.empty))
      fromRecordAndOptionsToEntity(category, imageUrls, subcategories, locationOverrides, productsCount, availabilities)
    }

  def fromRecordAndOptionsToEntity(
      category: CategoryRecord,
      imageUrls: Seq[ImageUrls],
      subcategories: Option[Seq[CategoryEntity]] = None,
      locationOverrides: Option[Map[UUID, CategoryLocation]] = None,
      productsCount: Option[Int] = None,
      availabilities: Option[Availabilities] = None,
    ): CategoryEntity =
    CategoryEntity(
      id = category.id,
      name = category.name,
      catalogId = category.catalogId,
      merchantId = category.merchantId,
      description = category.description,
      avatarBgColor = category.avatarBgColor,
      avatarImageUrls = imageUrls,
      position = category.position,
      active = category.active,
      subcategories = subcategories,
      locationOverrides = locationOverrides,
      productsCount = productsCount,
      availabilities = availabilities,
    )

  def fromUpsertionToUpdate(
      id: UUID,
      upsertion: CategoryUpdateEntity,
    )(implicit
      user: UserContext,
    ): CategoryUpdateModel =
    CategoryUpdateModel(
      id = Some(id),
      merchantId = Some(user.merchantId),
      parentCategoryId = upsertion.parentCategoryId,
      catalogId = upsertion.catalogId,
      name = upsertion.name,
      description = upsertion.description,
      avatarBgColor = upsertion.avatarBgColor,
      position = upsertion.position,
      active = None,
    )

  def fromUpsertionsToUpdates(
      parentId: UUID,
      catalogId: Option[UUID],
      upsertions: Seq[SubcategoryUpsertion],
    )(implicit
      user: UserContext,
    ): Seq[CategoryUpdateModel] =
    upsertions.map(fromUpsertionToUpdate(parentId, catalogId, _))

  def fromUpsertionToUpdate(
      parentId: UUID,
      catalogId: Option[UUID],
      upsertion: SubcategoryUpsertion,
    )(implicit
      user: UserContext,
    ): CategoryUpdateModel =
    CategoryUpdateModel(
      id = Some(upsertion.id),
      merchantId = Some(user.merchantId),
      parentCategoryId = Some(parentId),
      catalogId = catalogId,
      name = Some(upsertion.name),
      description = upsertion.description,
      avatarBgColor = upsertion.avatarBgColor,
      position = upsertion.position,
      active = upsertion.active,
    )

  def groupCategoriesPerProduct(
      productCategories: Seq[ProductCategoryRecord],
      categories: Seq[CategoryRecord],
    ): Map[UUID, Seq[CategoryRecord]] =
    productCategories.groupBy(_.productId).transform { (_, prodCats) =>
      prodCats.flatMap { productCategory =>
        categories.find(_.id == productCategory.categoryId)
      }
    }
}
