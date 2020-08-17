package io.paytouch.core.resources.catalogcategories

import java.util.UUID

import io.paytouch.core.data.daos._
import io.paytouch.core.data.model.{ CategoryLocationRecord, ImageUploadRecord }
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, _ }

abstract class CatalogCategoriesFSpec extends FSpec {

  abstract class CatalogCategoryResourceFSpecContext
      extends FSpecContext
         with MultipleLocationFixtures
         with AvailabilitiesSupport[CategoryAvailabilityDao]
         with ItemLocationSupport[CategoryLocationDao, CategoryLocationRecord, CategoryLocationUpdate] {

    lazy val categoryDao = daos.categoryDao
    lazy val catalogCategoryDao = daos.catalogCategoryDao
    lazy val itemLocationDao = daos.categoryLocationDao
    lazy val availabilityDao = daos.categoryAvailabilityDao
    lazy val productCategoryDao = daos.productCategoryDao
    lazy val productCategoryOptionDao = daos.productCategoryOptionDao
    lazy val imageUploadDao = daos.imageUploadDao
    lazy val articleDao = daos.articleDao

    lazy val catalog = Factory.catalog(merchant).create

    def assertCreation(creation: CatalogCategoryCreation, catalogCategoryId: UUID) =
      assertUpdate(creation.asUpdate, catalogCategoryId)

    def assertUpdate(update: CatalogCategoryUpdate, catalogCategoryId: UUID) = {
      val catalogCategory = catalogCategoryDao.findById(catalogCategoryId).await.get
      if (update.name.isDefined) update.name ==== Some(catalogCategory.name)
      if (update.description.isDefined) update.description ==== catalogCategory.description
      if (update.position.isDefined) update.position.getOrElse(0) ==== catalogCategory.position
      if (update.availabilities.isDefined) assertAvailabilityUpsertion(catalogCategory.id, update.availabilities.get)
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
        catalogCategoryId: UUID,
        imageUploads: Seq[ImageUploadRecord] = Seq.empty,
      ) = {
      val model = catalogCategoryDao.findById(catalogCategoryId).await.get
      val subCategories = catalogCategoryDao.findByParentId(catalogCategoryId).await
      entity.id ==== model.id
      entity.name ==== model.name
      entity.description ==== model.description
      entity.avatarImageUrls.map(_.imageUploadId) ==== imageUploads.map(_.id)
      entity.avatarImageUrls.map(_.urls) ==== imageUploads.map(_.urls.getOrElse(Map.empty))

      entity.position ==== model.position
    }

    private def assertUpdateImageUpload(imageUploadId: UUID, itemId: UUID) = {
      val imageUploadType = ImageUploadType.Category
      val imageUpload = imageUploadDao.findByObjectIds(Seq(itemId), imageUploadType).await.head
      imageUploadId ==== imageUpload.id
    }
  }
}
