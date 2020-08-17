package io.paytouch.core.resources.productcategories

import java.util.UUID

import io.paytouch.core.data.daos._
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.data.model.{ CategoryLocationRecord, ImageUploadRecord }
import io.paytouch.core.entities._
import io.paytouch.core.utils._

abstract class ProductCategoriesFSpec extends FSpec {

  abstract class ProductCategoryResourceFSpecContext
      extends FSpecContext
         with MultipleLocationFixtures
         with AvailabilitiesSupport[CategoryLocationAvailabilityDao]
         with ItemLocationSupport[CategoryLocationDao, CategoryLocationRecord, CategoryLocationUpdate] {

    lazy val categoryDao = daos.categoryDao
    lazy val itemLocationDao = daos.categoryLocationDao
    lazy val availabilityDao = daos.categoryLocationAvailabilityDao
    lazy val productCategoryDao = daos.productCategoryDao
    lazy val imageUploadDao = daos.imageUploadDao
    lazy val articleDao = daos.articleDao

    def assertCreation(creation: SystemCategoryCreation, categoryId: UUID) =
      assertUpdate(creation.asUpdate, categoryId)

    def assertUpdate(update: SystemCategoryUpdate, categoryId: UUID) = {
      val category = categoryDao.findById(categoryId).await.get
      if (update.name.isDefined) update.name ==== Some(category.name)
      if (update.description.isDefined) update.description ==== category.description
      if (update.avatarBgColor.isDefined) update.avatarBgColor ==== category.avatarBgColor
      if (update.position.isDefined) update.position.getOrElse(0) ==== category.position
      if (update.parentCategoryId.isDefined) update.parentCategoryId ==== category.parentCategoryId
      update.subcategories.foreach(assertSubcategoryUpsertion(categoryId, _))

      assertLocationOverridesUpdate(update.locationOverrides, categoryId)

      if (update.imageUploadIds.isDefined)
        update.imageUploadIds.get.map(imageUploadId => assertUpdateImageUpload(imageUploadId, categoryId))
    }

    def assertItemLocationUpdate(
        itemId: UUID,
        locationId: UUID,
        update: CategoryLocationUpdate,
      ) = {
      val record = assertItemLocationExists(itemId, locationId)
      if (update.active.isDefined) update.active ==== Some(record.active)
      if (update.availabilities.isDefined) assertAvailabilityUpsertion(record.id, update.availabilities.get)
    }

    def assertResponse(
        entity: Category,
        categoryId: UUID,
        imageUploads: Seq[ImageUploadRecord] = Seq.empty,
      ) = {
      val model = categoryDao.findById(categoryId).await.get
      val subcategories = categoryDao.findByParentId(categoryId).await
      model.active.isEmpty ==== model.parentCategoryId.isEmpty
      entity.id ==== model.id
      entity.name ==== model.name
      entity.description ==== model.description
      entity.avatarBgColor ==== model.avatarBgColor
      entity.avatarImageUrls.map(_.imageUploadId) ==== imageUploads.map(_.id)
      entity.avatarImageUrls.map(_.urls) ==== imageUploads.map(_.urls.getOrElse(Map.empty))

      entity.position ==== model.position
      entity.subcategories.map(_.map(_.id)).filterNot(_.isEmpty) ==== Some(subcategories.map(_.id))
        .filterNot(_.isEmpty)
    }

    def assertSubcategoryUpsertion(parentId: UUID, upsertion: SubcategoryUpsertion) = {
      val model = categoryDao.findById(upsertion.id).await.get
      model.active.isEmpty ==== model.parentCategoryId.isEmpty
      model.id ==== upsertion.id
      model.merchantId ==== merchant.id
      model.parentCategoryId ==== Some(parentId)
      model.name ==== upsertion.name
      if (upsertion.description.isDefined) upsertion.description ==== model.description
      if (upsertion.avatarBgColor.isDefined) upsertion.avatarBgColor ==== model.avatarBgColor
      if (upsertion.position.isDefined) upsertion.position ==== Some(model.position)
      if (upsertion.active.isDefined) upsertion.active ==== model.active
      if (upsertion.imageUploadIds.isDefined)
        upsertion.imageUploadIds.get.map(imageUploadId => assertUpdateImageUpload(imageUploadId, upsertion.id))
    }

    private def assertUpdateImageUpload(imageUploadId: UUID, itemId: UUID) = {
      val imageUploadType = ImageUploadType.Category
      val imageUpload = imageUploadDao.findByObjectIds(Seq(itemId), imageUploadType).await.head
      imageUploadId ==== imageUpload.id
    }
  }
}
