package io.paytouch.core.data.daos

import java.time.ZonedDateTime
import java.util.UUID

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.core.data.daos.features.{ SlickFindAllDao, SlickMerchantDao, SlickUpsertDao }
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.data.model.upsertions.CategoryUpsertion
import io.paytouch.core.data.tables.CategoriesTable
import io.paytouch.core.entities.enums.CatalogType
import io.paytouch.core.filters.CategoryFilters
import io.paytouch.core.utils.ResultType
import io.paytouch.core.utils.UtcTime

trait GenericCategoryDao extends SlickMerchantDao with SlickFindAllDao with SlickUpsertDao {
  implicit def ec: ExecutionContext
  def db: Database

  type Record = CategoryRecord
  type Update = CategoryUpdate
  type Upsertion = CategoryUpsertion
  type Table = CategoriesTable
  type Filters = CategoryFilters

  def catalogDao: CatalogDao
  def categoryAvailabilityDao: CategoryAvailabilityDao
  def categoryLocationAvailabilityDao: CategoryLocationAvailabilityDao
  def categoryLocationDao: CategoryLocationDao
  def imageUploadDao: ImageUploadDao
  def productCategoryDao: ProductCategoryDao

  val table: TableQuery[Table] = TableQuery[Table]

  final override lazy val baseQuery: Query[Table, Record, Seq] =
    withCatalogId match {
      case Some(true) =>
        super
          .baseQuery
          .join(catalogDao.queryFindByType(CatalogType.Menu))
          .on(_.catalogId === _.id)
          .map { case (categoriesT, _) => categoriesT }
      case Some(false) =>
        super
          .baseQuery
          .join(catalogDao.queryFindByType(CatalogType.DefaultMenu))
          .on(_.catalogId === _.id)
          .map { case (categoriesT, _) => categoriesT }
      case _ => super.baseQuery
    }

  def withCatalogId: Option[Boolean]

  def findAllWithFilters(merchantId: UUID, f: Filters)(offset: Int, limit: Int) =
    findAllByMerchantId(merchantId, f.locationId, f.query, f.updatedSince, f.catalogId)(offset, limit)

  def countAllWithFilters(merchantId: UUID, f: Filters) =
    countAllByMerchantId(merchantId, f.locationId, f.query, f.updatedSince, f.catalogId)

  def findAllByMerchantId(
      merchantId: UUID,
      locationId: Option[UUID],
      query: Option[String],
      updatedSince: Option[ZonedDateTime],
      catalogId: Option[UUID],
    )(
      offset: Int,
      limit: Int,
    ): Future[Seq[Record]] = {
    val q = queryFindMainByMerchantId(merchantId, locationId, query, updatedSince, catalogId)
      .sortBy(m => (m.position.asc, m.name.asc))
      .drop(offset)
      .take(limit)
    run(q.result)
  }

  def countAllByMerchantId(
      merchantId: UUID,
      locationId: Option[UUID],
      query: Option[String],
      updatedSince: Option[ZonedDateTime],
      catalogId: Option[UUID],
    ): Future[Int] = {
    val queryFind = queryFindMainByMerchantId(merchantId, locationId, query, updatedSince, catalogId)
    run(queryFind.length.result)
  }

  private def queryFindMainByMerchantId(
      merchantId: UUID,
      locationId: Option[UUID],
      query: Option[String],
      updatedSince: Option[ZonedDateTime],
      catalogId: Option[UUID],
    ) =
    baseQuery.filter(t =>
      all(
        Some(t.merchantId === merchantId),
        Some(t.parentCategoryId.isEmpty),
        locationId.map(lId => t.id in categoryLocationDao.queryFindByLocationId(lId).map(_.categoryId)),
        query.map(q => t.name.toLowerCase like s"%${q.toLowerCase}%"),
        catalogId.map(cId => t.catalogId === cId),
        updatedSince.map { date =>
          any(
            t.id in queryUpdatedSince(date).map(_.id),
            t.id in categoryLocationDao.queryUpdatedSince(date).map(_.categoryId),
          )
        },
      ),
    )

  def countProductsByCategoryIds(
      categoryIds: Seq[UUID],
      locationIds: Option[Seq[UUID]] = None,
    ): Future[Map[UUID, Int]] =
    if (categoryIds.isEmpty || locationIds.exists(_.isEmpty))
      Future.successful(Map.empty)
    else
      baseQuery
        .filter(_.id inSet categoryIds)
        .join(productCategoryDao.nonDeletedAccessibleProductsJoin(locationIds))
        .on(_.id === _.categoryId)
        .distinctOn(_._2.productId)
        .groupBy {
          case (categoriesTable, _) => categoriesTable.id
        }
        .map {
          case (categoryId, rows) => categoryId -> rows.size
        }
        .result
        .pipe(run)
        .map(_.toMap)

  def upsert(upsertion: Upsertion): Future[(ResultType, Record)] =
    (for {
      (resultType, category) <- queryUpsert(upsertion.category)
      subcategories <- queryBulkUpsertAndDeleteTheRestByParentCategoryId(upsertion.subcategories, category.id)
      categoryLocations <-
        categoryLocationDao
          .queryBulkUpsertAndDeleteTheRest(upsertion.categoryLocations, category.id)
      _ <-
        categoryAvailabilityDao
          .queryBulkUpsertAndDeleteTheRestByItemIds(upsertion.availabilities, Seq(category.id))
      _ <-
        categoryLocationAvailabilityDao
          .queryBulkUpsertAndDeleteTheRestByItemIds(upsertion.locationAvailabilities, categoryLocations.map(_.id))
      _ <- asOption(upsertion.imageUploads.map { img =>
        val imgType = ImageUploadType.Category
        val allCategoryIds = (subcategories :+ category).map(_.id)
        imageUploadDao.queryBulkUpsertAndDeleteTheRestByObjectIdsAndImageType(img, allCategoryIds, imgType)
      })
    } yield (resultType, category)).pipe(runWithTransaction)

  def findByParentId(parentId: UUID) =
    findByParentIds(Seq(parentId))

  def findByParentIds(parentIds: Seq[UUID]): Future[Seq[Record]] =
    if (parentIds.isEmpty)
      Future.successful(Seq.empty)
    else
      baseQuery
        .filter(_.parentCategoryId inSet parentIds)
        .sortBy(m => (m.position.asc, m.name.asc))
        .result
        .pipe(run)

  def updateOrdering(ordering: Seq[ModelOrdering]) =
    (for {
      _ <- asSeq {
        ordering.map { mo =>
          table
            .filter(_.id === mo.id)
            .map(t => (t.position, t.updatedAt))
            .update(mo.position, UtcTime.now)
        }
      }
    } yield ()).pipe(runWithTransaction)

  def queryBulkUpsertAndDeleteTheRestByParentCategoryId(categories: Seq[Update], parentCategoryId: UUID) =
    for {
      us <- queryBulkUpsert(categories)
      records = us.map { case (_, record) => record }
      _ <- queryDeleteTheRestByDeleteFilter(records, _.parentCategoryId === parentCategoryId)
    } yield records

  def findByNamesAndMerchantId(names: Seq[String], merchantId: UUID): Future[Seq[Record]] =
    run(baseQuery.filter(t => t.name.inSet(names) && t.merchantId === merchantId).result)

  override def queryDeleteByIdsAndMerchantId(ids: Seq[UUID], merchantId: UUID) =
    for {
      _ <-
        categoryAvailabilityDao
          .queryDeleteByCategoryIdsAndMerchantId(ids, merchantId)
      _ <-
        categoryLocationAvailabilityDao
          .queryDeleteByCategoryIdsAndMerchantId(ids, merchantId)
      categories <- table.filter(_.merchantId === merchantId).filter(_.id inSet ids).delete.map(_ => ids)
    } yield categories

  def findByProductIds(productIds: Seq[UUID]): Future[Map[UUID, Seq[Record]]] =
    queryFindByProductIds(productIds)
      .pipe(run)
      .map(_.groupBy { case (productId, _) => productId }
        .transform { (_, v) =>
          v.map {
            case (_, category) => category
          }
        })

  def queryFindByProductIds(productIds: Seq[UUID]) =
    baseQuery
      .join(productCategoryDao.table.filter(_.productId inSet productIds))
      .on(_.id === _.categoryId)
      .map { case (categoriesT, productCategoriesT) => productCategoriesT.productId -> categoriesT }
      .result

  def findByCatalogId(catalogId: UUID): Future[Seq[Record]] =
    baseQuery
      .filter(_.catalogId === catalogId)
      .result
      .pipe(run)

  def findByCatalogIds(catalogIds: Seq[UUID]): Future[Seq[Record]] =
    baseQuery
      .filter(_.catalogId inSet catalogIds)
      .result
      .pipe(run)
}
