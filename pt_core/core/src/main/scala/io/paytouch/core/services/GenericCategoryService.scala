package io.paytouch.core.services

import java.util.UUID

import akka.actor.ActorRef

import cats.implicits._
import cats.data.Validated.{ Invalid, Valid }

import io.paytouch.core.async.trackers.EventTracker
import io.paytouch.core.conversions.GenericCategoryConversions
import io.paytouch.core.data.daos._
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.data.model.upsertions.{ CategoryUpsertion => CategoryUpsertionModel }
import io.paytouch.core.data.model.{ CatalogRecord, CategoryUpdate => CategoryUpdateModel, _ }
import io.paytouch.core.entities.enums.ExposedName
import io.paytouch.core.entities.{ CategoryCreation, CategoryUpdate, Category => CategoryEntity, _ }
import io.paytouch.core.expansions.CategoryExpansions
import io.paytouch.core.filters.CategoryFilters
import io.paytouch.core.services.features._
import io.paytouch.core.utils.ResultType
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr

import io.paytouch.core.validators.GenericCategoryValidator
import io.paytouch.core.{ withTag, Availabilities, LocationOverridesPer }

import scala.concurrent._

trait GenericCategoryService
    extends GenericCategoryConversions
       with CreateAndUpdateFeatureWithStateProcessing
       with FindAllFeature
       with FindByIdFeature
       with UpdateActiveLocationsFeature
       with DeleteFeature {

  implicit def ec: ExecutionContext
  implicit def daos: Daos

  def availabilityService: CategoryAvailabilityService
  def locationAvailabilityService: CategoryLocationAvailabilityService
  def eventTracker: ActorRef withTag EventTracker
  def categoryLocationService: CategoryLocationService
  def imageUploadService: ImageUploadService
  def productCategoryService: ProductCategoryService

  type Creation = CategoryCreation
  type Dao <: GenericCategoryDao
  type Entity = CategoryEntity
  type Expansions = CategoryExpansions
  type Filters = CategoryFilters
  type Model = CategoryUpsertionModel
  type Record = CategoryRecord
  type Update = CategoryUpdate
  type Validator <: GenericCategoryValidator

  protected def dao: Dao
  protected def validator: Validator
  val defaultFilters = CategoryFilters()

  val classShortName = ExposedName.Category

  val itemLocationService = categoryLocationService

  def assignProducts(
      categoryId: UUID,
      assignment: ProductsAssignment,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Unit]] =
    validator.accessOneById(categoryId).flatMap {
      case Valid(_)       => productCategoryService.associateCategoryToProducts(categoryId, assignment.productIds)
      case i @ Invalid(_) => Future.successful(i)
    }

  def assignProducts(
      categoryId: UUID,
      assignments: Seq[CatalogCategoryProductAssignment],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Unit]] =
    validator.accessOneById(categoryId).flatMap {
      case Valid(_)       => productCategoryService.associateCatalogCategoryToProducts(categoryId, assignments)
      case i @ Invalid(_) => Future.successful(i)
    }

  def enrich(categories: Seq[Record], f: Filters)(e: Expansions)(implicit user: UserContext): Future[Seq[Entity]] = {
    val categoryIds = categories.map(_.id)
    val subcategoriesR = getOptionalSubcategoriesPerCategory(categoryIds)(e.withSubcategories)
    val productCountR = getOptionalProductsCountPerCategory(categoryIds, f.locationId)(e.withProductsCount)
    val locationOverridesR =
      getOptionalLocationOverridesPerCategory(categoryIds, f.locationId)(e.withLocations, e.withAvailabilities)
    val imageUrlsPerCategoryR = getImagesPerCategory(categories)
    val availabilitiesPerCategoryR = getOptionalAvailabilitiesPerCategory(categoryIds)(e.withAvailabilities)
    for {
      subcategories <- subcategoriesR
      locationOverrides <- locationOverridesR
      productsCounts <- productCountR
      enrichedSubcategories <- enrichSubcategories(subcategories, f)(e.withProductsCount)
      imageUrlsPerCategory <- imageUrlsPerCategoryR
      availabilitiesPerCategory <- availabilitiesPerCategoryR
    } yield fromRecordsAndOptionsToEntities(
      categories,
      imageUrlsPerCategory,
      enrichedSubcategories,
      locationOverrides,
      productsCounts,
      availabilitiesPerCategory,
    )
  }

  private def enrichSubcategories(
      subcategories: Option[Map[UUID, Seq[Record]]],
      f: Filters,
    )(
      withProductsCount: Boolean,
    )(implicit
      user: UserContext,
    ): Future[Option[Map[UUID, Seq[Entity]]]] =
    subcategories match {
      case Some(subcategoriesPerCategory) =>
        val subcategories: Seq[CategoryRecord] = subcategoriesPerCategory.flatMap { case (_, v) => v }.toSeq
        val expansions = CategoryExpansions(withProductsCount = withProductsCount)
        enrich(subcategories, f)(expansions).map { enrichedSubcategories =>
          val enrichedSubcategoriesPerCategory = subcategoriesPerCategory.transform {
            case (_, subcategories) =>
              subcategories.flatMap(subcategory => enrichedSubcategories.find(_.id == subcategory.id))
          }
          Some(enrichedSubcategoriesPerCategory)
        }
      case None => Future.successful(None)
    }

  def getCategoriesPerProducts(
      products: Seq[ArticleRecord],
    )(implicit
      user: UserContext,
    ): Future[Map[ArticleRecord, Seq[Entity]]] = {
    val mainProductIds = products.map(_.mainProductId)
    dao.findByProductIds(mainProductIds).map {
      _.flatMap {
        case (mainProductId, categories) =>
          val productsPerParent = products.filter(_.mainProductId == mainProductId)
          val categoryEntities = fromRecordsToEntities(categories)
          productsPerParent.map(product => product -> categoryEntities)
      }
    }
  }

  private def getOptionalSubcategoriesPerCategory(
      parentIds: Seq[UUID],
    )(
      withSubcategories: Boolean,
    ): Future[Option[Map[UUID, Seq[Record]]]] =
    if (withSubcategories)
      dao.findByParentIds(parentIds).map { categories =>
        val categoriesPerParent = categories.groupBy(_.parentCategoryId).toMap
        Some(categoriesPerParent.view.filterKeys(_.isDefined).map { case (k, v) => (k.get, v) }.toMap)
      }
    else
      Future.successful(None)

  private def getOptionalLocationOverridesPerCategory(
      categoryIds: Seq[UUID],
      locationId: Option[UUID],
    )(
      withLocations: Boolean,
      withAvailabilities: Boolean,
    )(implicit
      user: UserContext,
    ): Future[Option[LocationOverridesPer[UUID, CategoryLocation]]] =
    if (withLocations || withAvailabilities)
      itemLocationService.findAllByItemIdsAsMap(categoryIds, locationId)(withAvailabilities).map(Some(_))
    else Future.successful(None)

  private def getOptionalAvailabilitiesPerCategory(
      categoryIds: Seq[UUID],
    )(
      withAvailabilities: Boolean,
    )(implicit
      user: UserContext,
    ): Future[Option[Map[UUID, Availabilities]]] =
    if (withAvailabilities) availabilityService.findAllByCategoryIds(categoryIds).map(Some(_))
    else Future.successful(None)

  private def getOptionalProductsCountPerCategory(
      categoryIds: Seq[UUID],
      locationId: Option[UUID],
    )(
      withProductsCount: Boolean,
    )(implicit
      user: UserContext,
    ): Future[Option[Map[UUID, Int]]] =
    if (withProductsCount)
      dao.countProductsByCategoryIds(categoryIds, Some(user.accessibleLocations(locationId))).map(Some(_))
    else Future.successful(None)

  private def getImagesPerCategory(categories: Seq[Record]): Future[Map[Record, Seq[ImageUrls]]] = {
    val categoryIds = categories.map(_.id)
    imageUploadService.findByObjectIds(categoryIds, ImageUploadType.Category).map(_.mapKeysToRecords(categories))
  }

  override implicit def toFutureResultTypeEntity(
      f: Future[(ResultType, Record)],
    )(implicit
      user: UserContext,
    ): Future[(ResultType, Entity)] =
    for {
      (resultType, record) <- f
      enrichedRecord <- enrich(record, defaultFilters)(CategoryExpansions.withSubcategoriesOnly)
    } yield (resultType, enrichedRecord)

  protected def convertToUpsertionModel(
      id: UUID,
      update: Update,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Model]] =
    for {
      category <- convertToCategoryUpdate(id, update)
      subcategories <- convertToSubcategoryUpdates(id, update)
      categoryLocations <- itemLocationService.convertToItemLocationUpdates(id, update.locationOverrides)
      availabilities <- availabilityService.toCategoryAvailabilities(id, update.availabilities)
      locationAvailabilities <-
        locationAvailabilityService
          .toCategoryLocationAvailabilities(categoryLocations.getOrElse(Map.empty), update.locationOverrides)
      imageUploads <- convertToMultipleImageUploads(id, update)
    } yield Multiple.combine(
      category,
      subcategories,
      categoryLocations,
      availabilities,
      locationAvailabilities,
      imageUploads,
    )(CategoryUpsertionModel)

  private def convertToCategoryUpdate(
      id: UUID,
      upsertion: Update,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[CategoryUpdateModel]] =
    validator.validateUpsertion(id, upsertion).mapNested(_ => fromUpsertionToUpdate(id, upsertion))

  private def convertToMultipleImageUploads(
      id: UUID,
      update: Update,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[Seq[ImageUploadUpdate]]]] = {
    val relations = Map(id -> update.imageUploadIds) ++ update
      .subcategories
      .map(subcat => subcat.id -> subcat.imageUploadIds)
    imageUploadService.convertToMultipleImageUploadUpdates(relations, ImageUploadType.Category)
  }

  private def convertToSubcategoryUpdates(
      parentId: UUID,
      upsertion: Update,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Seq[CategoryUpdateModel]]] =
    Future.successful {
      Multiple.success {
        fromUpsertionsToUpdates(parentId, upsertion.catalogId, upsertion.subcategories)
      }
    }

  def updateOrdering(ordering: Seq[EntityOrdering])(implicit user: UserContext): Future[ErrorsOr[Unit]] =
    validator.validateByIds(ordering.map(_.id)).flatMapTraverse(_ => dao.updateOrdering(convertOrdering(ordering)))

  def findByCatalogs(catalogs: Seq[CatalogRecord]): Future[Map[CatalogRecord, Seq[Record]]] = {
    val catalogIds = catalogs.map(_.id)
    dao.findByCatalogIds(catalogIds).map(records => records.groupBy(_.catalogId).mapKeysToRecords(catalogs))
  }
}
