package io.paytouch.core.data.daos

import java.util.UUID

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.core.data.daos.features.{ SlickFindAllDao, SlickSoftDeleteDao }
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums.{ ArticleScope, ArticleType }
import io.paytouch.core.data.tables.ArticlesTable
import io.paytouch.core.filters.InventoryFilters

class InventoryDao(
    val productCategoryDao: ProductCategoryDao,
    val articleDao: ArticleDao,
    val productLocationDao: ProductLocationDao,
    val stockDao: StockDao,
    val supplierProductDao: SupplierProductDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickFindAllDao
       with SlickSoftDeleteDao {
  type Record = ArticleRecord
  type Update = ArticleUpdate
  type Table = ArticlesTable
  type Filters = InventoryFilters

  val table = TableQuery[Table]

  def findAllWithFilters(merchantId: UUID, f: InventoryFilters)(offset: Int, limit: Int): Future[Seq[Record]] =
    findAllByMerchantId(
      merchantId = merchantId,
      categoryIds = f.categoryIds,
      locationIds = f.locationIds,
      lowInventory = f.lowInventory,
      query = f.query,
      supplierId = f.supplierId,
      isCombo = f.isCombo,
      articleTypes = f.articleTypes,
      articleScope = f.articleScope,
    )(offset, limit)

  def findAllByMerchantId(
      merchantId: UUID,
      categoryIds: Option[Seq[UUID]],
      locationIds: Option[Seq[UUID]],
      lowInventory: Option[Boolean],
      query: Option[String],
      supplierId: Option[UUID],
      isCombo: Option[Boolean],
      articleTypes: Option[Seq[ArticleType]],
      articleScope: Option[ArticleScope],
    )(
      offset: Int,
      limit: Int,
    ): Future[Seq[Record]] =
    queryFindAllByMerchantId(
      merchantId,
      categoryIds,
      locationIds,
      lowInventory,
      query,
      supplierId,
      isCombo,
      articleTypes,
      articleScope,
    ).drop(offset).take(limit).result.pipe(run)

  def countAllWithFilters(merchantId: UUID, f: InventoryFilters): Future[Int] =
    countAllByMerchantId(
      merchantId = merchantId,
      categoryIds = f.categoryIds,
      locationIds = f.locationIds,
      lowInventory = f.lowInventory,
      query = f.query,
      supplierId = f.supplierId,
      isCombo = f.isCombo,
      articleTypes = f.articleTypes,
      articleScope = f.articleScope,
    )

  def countAllByMerchantId(
      merchantId: UUID,
      categoryIds: Option[Seq[UUID]],
      locationIds: Option[Seq[UUID]],
      lowInventory: Option[Boolean],
      query: Option[String],
      supplierId: Option[UUID],
      isCombo: Option[Boolean],
      articleTypes: Option[Seq[ArticleType]],
      articleScope: Option[ArticleScope],
    ): Future[Int] =
    queryFindAllByMerchantId(
      merchantId,
      categoryIds,
      locationIds,
      lowInventory,
      query,
      supplierId,
      isCombo,
      articleTypes,
      articleScope,
    ).length.result.pipe(run)

  def findStorableProductsByMainProductIds(productIds: Seq[UUID]): Future[Seq[Record]] =
    if (productIds.isEmpty)
      Future.successful(Seq.empty)
    else
      table
        .filter(_.isVariantOfProductId inSet productIds)
        .filter(_.`type` inSet ArticleType.storables)
        .result
        .pipe(run)

  def queryFindAllByMerchantId(
      merchantId: UUID,
      categoryIds: Option[Seq[UUID]],
      locationIds: Option[Seq[UUID]],
      lowInventory: Option[Boolean],
      query: Option[String],
      supplierId: Option[UUID],
      isCombo: Option[Boolean],
      articleTypes: Option[Seq[ArticleType]],
      articleScope: Option[ArticleScope],
    ) =
    queryFindByOptionalCategoryIdsOrdered(categoryIds).filter { t =>
      all(
        Some(t.merchantId === merchantId),
        locationIds.map(lIds => t.id in productLocationDao.queryFindByLocationIds(lIds).map(_.productId)),
        query.map(q => articleDao.querySearchProduct(t, q)),
        articleTypes.map(types => t.`type` inSet types),
        articleScope.map(scope => t.scope === scope),
        lowInventory.map { lowInv =>
          if (lowInv)
            ((t.id in stockDao
              .queryProductsWithLowInventory(merchantId, locationIds)
              .map(_.id)) || (t.id in stockDao
              .queryProductsWithLowInventory(merchantId, locationIds)
              .map(_.isVariantOfProductId)).getOrElse(false))
          else
            ((t.id in stockDao
              .queryProductsWithHighInventory(merchantId, locationIds)
              .map(_.id)) || (t.id in stockDao
              .queryProductsWithHighInventory(merchantId, locationIds)
              .map(_.isVariantOfProductId)).getOrElse(false))
        },
        supplierId.map(sId => t.id in supplierProductDao.queryFindBySupplierId(sId).map(_.productId)),
        isCombo.map(isCmb => t.isCombo === isCmb),
      )
    }

  def queryFindByOptionalCategoryIdsOrdered(categoryIds: Option[Seq[UUID]]) =
    categoryIds.fold(nonDeletedTable.sortBy(_.name.asc)) { cIds =>
      for {
        (p, pc) <-
          nonDeletedTable
            .join(productCategoryDao.queryFindByCategoryIds(cIds))
            .on(_.id === _.productId)
            .sortBy {
              case (productsTable, productCategoriesTable) =>
                (productCategoriesTable.position, productsTable.name.asc)
            }
      } yield p
    }
}
