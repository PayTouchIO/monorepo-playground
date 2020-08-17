package io.paytouch.core.data.daos

import java.util.UUID

import scala.concurrent._

import slick.lifted.{ CanBeQueryCondition, Rep }

import io.paytouch._
import io.paytouch.implicits._

import io.paytouch.core.data.daos.features.SlickRelDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ ProductCategoryOrdering, ProductCategoryRecord, ProductCategoryUpdate }
import io.paytouch.core.data.model.upsertions.ProductCategoryUpsertion
import io.paytouch.core.data.tables.ProductCategoriesTable
import io.paytouch.core.utils.UtcTime

class ProductCategoryDao(
    articleDao: => ArticleDao,
    categoryDao: => CategoryDao,
    productLocationDao: => ProductLocationDao,
    productCategoryOptionDao: ProductCategoryOptionDao,
    systemCategoryDao: => SystemCategoryDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickRelDao {
  type Record = ProductCategoryRecord
  type Update = ProductCategoryUpdate
  type Upsertion = ProductCategoryUpsertion
  type Table = ProductCategoriesTable

  val table = TableQuery[Table]

  def queryByRelIds(productCategoryUpdate: Update) = {
    require(
      productCategoryUpdate.productId.isDefined,
      "ProductCategoryDao - Impossible to find by product id and category id without a product id",
    )

    require(
      productCategoryUpdate.categoryId.isDefined,
      "ProductCategoryDao - Impossible to find by product id and category id without a category id",
    )

    queryFindByProductIdAndCategoryId(productCategoryUpdate.productId.get, productCategoryUpdate.categoryId.get)
  }

  private def nonDeletedProductsJoin =
    baseQuery
      .join(articleDao.nonDeletedTable)
      .on(_.productId === _.id)
      .map {
        case (productCategoriesTable, _) => productCategoriesTable
      }

  def nonDeletedAccessibleProductsJoin(locationIds: Option[Seq[UUID]]) =
    locationIds.fold(nonDeletedProductsJoin) { lIds =>
      nonDeletedProductsJoin
        .join(productLocationDao.queryFindByLocationIds(lIds))
        .on(_.productId === _.productId)
        .map {
          case (productCategoriesTable, _) => productCategoriesTable
        }
    }

  def queryFindByProductIdAndCategoryId(productId: UUID, categoryId: UUID) =
    baseQuery.filter(t => t.productId === productId && t.categoryId === categoryId)

  def queryFindByProductIds(ids: Seq[UUID]) =
    baseQuery.filter(t => t.productId inSet ids)

  def queryBulkUpsertAndDeleteTheRestSystemCategoriesByProductIds(
      productCategories: Seq[Upsertion],
      productIds: Seq[UUID],
    ) =
    queryBulkUpsertAndDeleteTheRestByProductIds(productCategories, productIds)(legacySystemCategoriesOnly = true)

  def queryBulkUpsertAndDeleteTheRestCatalogCategoriesByProductIds(
      productCategories: Seq[Upsertion],
      productIds: Seq[UUID],
    ) =
    queryBulkUpsertAndDeleteTheRestByProductIds(productCategories, productIds)(legacySystemCategoriesOnly = false)

  private def queryBulkUpsertAndDeleteTheRestByProductIds(
      productCategories: Seq[Upsertion],
      productIds: Seq[UUID],
    )(
      legacySystemCategoriesOnly: Boolean,
    ) =
    for {
      oldCategoryIds <- queryFindByProductIds(productIds, legacySystemCategoriesOnly = Some(legacySystemCategoriesOnly))
        .map(_.categoryId)
        .result
      newCategoryIds = productCategories.flatMap(_.productCategory.categoryId)
      updates <- queryBulkUpsertionAndDeleteTheRestByRelIds(
        productCategories,
        t => t.productId.inSet(productIds) && t.categoryId.inSet(oldCategoryIds),
      )
      _ <- categoryDao.queryMarkAsUpdatedByIds(oldCategoryIds ++ newCategoryIds)
    } yield updates

  def bulkUpsertAndDeleteTheRestByCategoryId(productCategories: Seq[Upsertion], categoryId: UUID) =
    run(queryBulkUpsertAndDeleteTheRestByCategoryId(productCategories, categoryId))

  def queryBulkUpsertAndDeleteTheRestByCategoryId(productCategoryUpsertions: Seq[Upsertion], categoryId: UUID) =
    for {
      oldProductIds <- queryFindByCategoryId(categoryId).map(_.productId).result
      newProductIds = productCategoryUpsertions.flatMap(_.productCategory.productId)
      updates <- queryBulkUpsertionAndDeleteTheRestByRelIds(productCategoryUpsertions, _.categoryId === categoryId)
      _ <- categoryDao.queryMarkAsUpdatedById(categoryId)
      _ <- articleDao.queryMarkAsUpdatedByIds(oldProductIds ++ newProductIds)
    } yield updates

  private def queryBulkUpsertionAndDeleteTheRestByRelIds[R <: Rep[_], S <: Effect](
      upsertions: Seq[Upsertion],
      query: Table => R,
    )(implicit
      wt: CanBeQueryCondition[R],
    ) = {
    val productCategoryUpdates = upsertions.map(_.productCategory)
    val productCategoryOptionUpdates = upsertions.flatMap(_.productCategoryOption)

    for {
      records <- queryBulkUpsertAndDeleteTheRestByRelIds(productCategoryUpdates, query)
      _ <- productCategoryOptionDao.queryBulkUpsert(productCategoryOptionUpdates)
    } yield records
  }

  def bulkUpsertionByRelIds(upsertions: Seq[Upsertion]) =
    run(queryBulkUpsertionByRelIds(upsertions))

  def queryBulkUpsertionByRelIds(upsertions: Seq[Upsertion]) = {
    val productCategoryUpdates = upsertions.map(_.productCategory)
    val productCategoryOptionUpdates = upsertions.flatMap(_.productCategoryOption)

    for {
      records <- queryBulkUpsertByRelIds(productCategoryUpdates)
      _ <- productCategoryOptionDao.queryBulkUpsert(productCategoryOptionUpdates)
    } yield records
  }

  def queryFindByCategoryIds(categoryIds: Seq[UUID], maybeCatalogIds: Option[Seq[CatalogIdPostgres]] = None) =
    baseQuery
      .filter(_.categoryId inSet categoryIds)
      .pipe(q => maybeCatalogIds.fold(q)(queryFindByCatalogIds(q, _)))

  def queryFindByCategoryId(categoryId: UUID) =
    queryFindByCategoryIds(Seq(categoryId))

  def queryFindByProductId(productId: UUID) = queryFindByProductIds(Seq(productId))

  def findByProductId(productId: UUID, legacySystemCategoriesOnly: Option[Boolean] = None): Future[Seq[Record]] =
    findByProductIds(Seq(productId), legacySystemCategoriesOnly)

  def findByProductIds(productIds: Seq[UUID], legacySystemCategoriesOnly: Option[Boolean] = None): Future[Seq[Record]] =
    if (productIds.isEmpty)
      Future.successful(Seq.empty)
    else
      queryFindByProductIds(productIds, legacySystemCategoriesOnly)
        .result
        .pipe(run)

  def findByCategoryId(categoryId: UUID): Future[Seq[Record]] =
    findByCategoryIds(Seq(categoryId))

  def findByCategoryIds(categoryIds: Seq[UUID]): Future[Seq[Record]] =
    run(queryFindByCategoryIds(categoryIds).result)

  def findByProductIdAndCategoryId(productId: UUID, categoryId: UUID) =
    run(queryFindByProductIdAndCategoryId(productId, categoryId).result.headOption)

  def updateOrdering(ordering: Seq[ProductCategoryOrdering]) =
    runWithTransaction(queryUpdateOrdering(ordering))

  private def queryUpdateOrdering(ordering: Seq[ProductCategoryOrdering]) =
    for {
      _ <- asSeq(
        ordering.map(pco =>
          queryFindByProductIdAndCategoryId(pco.productId, pco.categoryId)
            .map(t => (t.position, t.updatedAt))
            .update(pco.position, UtcTime.now),
        ),
      )
      _ <- articleDao.queryMarkAsUpdatedByIds(ordering.map(_.productId))
      _ <- categoryDao.queryMarkAsUpdatedByIds(ordering.map(_.categoryId))
    } yield ()

  private def queryFindByProductIds(productIds: Seq[UUID], legacySystemCategoriesOnly: Option[Boolean] = None) = {
    val filteredTable = legacySystemCategoriesOnly match {
      case Some(true) =>
        baseQuery
          .join(systemCategoryDao.baseQuery)
          .on(_.categoryId === _.id)
          .map { case (pc, c) => pc }
      case _ =>
        baseQuery
    }

    filteredTable.filter(_.productId inSet productIds)
  }

  def queryFindByCatalogIds(catalogIds: Seq[CatalogIdPostgres]): Query[Table, Record, Seq] =
    queryFindByCatalogIds(baseQuery, catalogIds)

  private def queryFindByCatalogIds(
      q: Query[Table, Record, Seq],
      catalogIds: Seq[CatalogIdPostgres],
    ): Query[Table, Record, Seq] =
    q.join(categoryDao.baseQuery)
      .on(_.categoryId === _.id)
      .filter {
        case (productCategoriesTable, categoriesTable) => categoriesTable.catalogId inSet catalogIds.map(_.value)
      }
      .map { case (productCategoriesTable, categoriesTable) => productCategoriesTable }
}
