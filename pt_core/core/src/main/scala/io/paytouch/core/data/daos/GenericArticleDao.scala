package io.paytouch.core.data.daos

import java.time.{ Duration, ZonedDateTime }
import java.util.UUID

import scala.concurrent._

import io.paytouch._
import io.paytouch.implicits._

import io.paytouch.core.data.daos.features.{ SlickFindAllDao, SlickSoftDeleteDao, SlickUpsertDao }
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums.{ ArticleScope, ArticleType, ImageUploadType }
import io.paytouch.core.data.model.upsertions.ArticleUpsertion
import io.paytouch.core.data.tables._
import io.paytouch.core.filters.ArticleFilters
import io.paytouch.core.utils.ResultType
import io.paytouch.core.utils.UtcTime

trait GenericArticleDao extends SlickFindAllDao with SlickUpsertDao with SlickSoftDeleteDao {
  implicit def ec: ExecutionContext
  def db: Database

  type Record = ArticleRecord
  type Update = ArticleUpdate
  type Upsertion = ArticleUpsertion
  type Filters = ArticleFilters
  type Table = ArticlesTable

  def bundleSetDao: BundleSetDao
  def imageUploadDao: ImageUploadDao
  def loyaltyRewardProductDao: LoyaltyRewardProductDao
  def modifierSetProductDao: ModifierSetProductDao
  def orderItemDao: OrderItemDao
  def productCategoryDao: ProductCategoryDao
  def productLocationDao: ProductLocationDao
  def productLocationTaxRateDao: ProductLocationTaxRateDao
  def recipeDetailDao: RecipeDetailDao
  def supplierProductDao: SupplierProductDao
  def stockDao: StockDao
  def variantOptionTypeDao: VariantOptionTypeDao
  def variantOptionDao: VariantOptionDao
  def articleIdentifierDao: ArticleIdentifierDao
  def variantProductDao: VariantProductDao

  val table = TableQuery[Table]

  def scope: Option[ArticleScope]

  override def nonDeletedTable = super.nonDeletedTable.filter(byScope)

  private val targetTable = table.filter(byScope)
  private def byScope(tb: Table): Rep[Boolean] = scope.map(scp => tb.scope === scp).getOrElse(true)

  def upsert(upsertion: Upsertion): Future[(ResultType, Record)] =
    runWithTransaction(queryUpsertion(upsertion))

  def queryUpsertion(upsertion: Upsertion) =
    for {
      (resultType, product) <- queryUpsert(upsertion.product)
      parentIds = Seq(product.id)
      variantOptionTypes <- asOption(
        upsertion
          .variantOptionTypes
          .map(variantOptionTypeDao.queryBulkUpsertAndDeleteTheRestByProductIds(_, parentIds)),
      )
      variantOptions <- asOption(
        upsertion.variantOptions.map(variantOptionDao.queryBulkUpsertAndDeleteTheRestByProductIds(_, parentIds)),
      )
      variantProducts <- asSeq(upsertion.variantProducts.map(_.map(variantProductDao.queryUpsert)).getOrElse(Seq.empty))
      productIds = (product +: variantProducts).map(_.id)
      _ <- asOption(
        upsertion.variantProducts.map(_ => variantProductDao.queryDeleteTheRestByParentIds(variantProducts, parentIds)),
      )
      productLocations <- productLocationDao.queryBulkUpsertAndDeleteTheRest(upsertion.productLocations, product.id)
      productCategories <- asOption(
        upsertion
          .productCategories
          .map(productCategoryDao.queryBulkUpsertAndDeleteTheRestSystemCategoriesByProductIds(_, productIds)),
      )
      supplierProducts <- asOption(
        upsertion.supplierProducts.map(supplierProductDao.queryBulkUpsertAndDeleteTheRestByProductIds(_, productIds)),
      )
      productLocationTaxRates <-
        productLocationTaxRateDao
          .queryBulkUpsertAndDeleteTheRest(upsertion.productLocationTaxRates, product.id)
      imageUploads <- asOption(upsertion.imageUploads.map { imgs =>
        val imgType = ImageUploadType.Product
        imageUploadDao.queryBulkUpsertAndDeleteTheRestByObjectIdsAndImageType(imgs, productIds, imgType)
      })
      recipeDetails <- asOption(upsertion.recipeDetails.map(recipeDetailDao.queryUpsertByRelIds))
      bundleSets <- asOption(
        upsertion.bundleSets.map(bundleSetDao.queryBulkUpsertAndDeleteTheRestByProductIds(_, productIds)),
      )

      _ <- variantProductDao.querySoftDeleteVariantsWithNoOptions(product.merchantId)
    } yield (resultType, product)

  def findAllWithFilters(merchantId: UUID, f: Filters)(offset: Int, limit: Int): Future[Seq[Record]] =
    run(
      queryFindAllByMerchantId(
        merchantId,
        f.categoryIds,
        f.locationIds,
        f.modifierSetId,
        f.supplierId,
        f.loyaltyRewardId,
        f.query,
        f.updatedSince,
        f.lowInventory,
        f.isCombo,
        f.scope,
        f.articleTypes,
        f.ids,
        f.catalogIds,
      ).drop(offset).take(limit).result,
    )

  def countAllWithFilters(merchantId: UUID, f: Filters): Future[Int] =
    run(
      queryFindAllByMerchantId(
        merchantId,
        f.categoryIds,
        f.locationIds,
        f.modifierSetId,
        f.supplierId,
        f.loyaltyRewardId,
        f.query,
        f.updatedSince,
        f.lowInventory,
        f.isCombo,
        f.scope,
        f.articleTypes,
        f.ids,
        f.catalogIds,
      ).length.result,
    )

  def findAllByMerchantId(
      merchantId: UUID,
      articleTypes: Option[Seq[ArticleType]],
      categoryIds: Option[Seq[UUID]] = None,
      locationIds: Option[Seq[UUID]] = None,
      modifierSetId: Option[UUID] = None,
      supplierId: Option[UUID] = None,
      loyaltyRewardId: Option[UUID] = None,
      query: Option[String] = None,
      lowInventory: Option[Boolean] = None,
      isCombo: Option[Boolean] = None,
      updatedSince: Option[ZonedDateTime] = None,
      scope: Option[ArticleScope] = None,
    ): Future[Seq[ArticleRecord]] =
    queryFindAllByMerchantId(
      merchantId,
      categoryIds,
      locationIds,
      modifierSetId,
      supplierId,
      loyaltyRewardId,
      query,
      updatedSince,
      lowInventory,
      isCombo,
      scope,
      articleTypes,
      ids = None,
      catalogIds = None,
    ).result.pipe(run)

  def queryFindAllByMerchantId(
      merchantId: UUID,
      categoryIds: Option[Seq[UUID]] = None,
      locationIds: Option[Seq[UUID]] = None,
      modifierSetId: Option[UUID] = None,
      supplierId: Option[UUID] = None,
      loyaltyRewardId: Option[UUID] = None,
      query: Option[String] = None,
      updatedSince: Option[ZonedDateTime] = None,
      lowInventory: Option[Boolean] = None,
      isCombo: Option[Boolean] = None,
      scope: Option[ArticleScope] = None,
      articleTypes: Option[Seq[ArticleType]] = None,
      ids: Option[Seq[UUID]] = None,
      catalogIds: Option[Seq[CatalogIdPostgres]] = None,
    ) =
    nonDeletedTable
      .sortBy(productsTable => (productsTable.name.asc, productsTable.id))
      .pipe(t => queryFindByCategoryAndCatalogIdsOrdered(t, categoryIds, catalogIds))
      .filter(t =>
        all(
          Some(t.merchantId === merchantId),
          locationIds.map(lIds => t.id in productLocationDao.queryFindByLocationIds(lIds).map(_.productId)),
          modifierSetId.map(mId =>
            t.id in modifierSetProductDao
              .queryFindByModifierSetIdAndOptionalLocationIds(mId, locationIds)
              .map(_.productId),
          ),
          supplierId.map(sId =>
            (t.id in supplierProductDao
              .queryFindBySupplierId(sId)
              .map(_.productId)) || (t.isVariantOfProductId in supplierProductDao
              .queryFindBySupplierId(sId)
              .map(_.productId)).getOrElse(false),
          ),
          loyaltyRewardId
            .map(lrId => t.id in loyaltyRewardProductDao.queryFindByLoyaltyRewardId(lrId).map(_.productId)),
          query.map(q => querySearchProduct(t, q)),
          updatedSince.map { date =>
            any(
              t.id in queryUpdatedSince(date).map(_.id),
              t.id in productLocationDao.queryUpdatedSince(date).map(_.productId),
            )
          },
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
          isCombo.map(isCmb => t.isCombo === isCmb),
          scope.map(scp => t.scope === scp),
          articleTypes.map(types => t.`type` inSet types),
          ids.map(idsValue => t.id inSet idsValue),
        ),
      )

  def querySearchProduct(t: Table, q: String) =
    querySearchByName(t, q) || querySearchByUpc(t, q) || querySearchBySku(t, q)

  private def querySearchByName(t: Table, q: String): Rep[Boolean] =
    t.name.toLowerCase.like(s"%${q.toLowerCase}%") && byScope(t)

  private def querySearchByUpc(t: Table, q: String): Rep[Boolean] =
    (t.upc.toLowerCase like s"%${q.toLowerCase}%").getOrElse(false) && byScope(t)

  private def querySearchBySku(t: Table, q: String): Rep[Boolean] =
    (t.sku.toLowerCase like s"%${q.toLowerCase}%").getOrElse(false) && byScope(t)

  private def queryFindByCategoryAndCatalogIdsOrdered(
      q: Query[Table, Record, Seq],
      maybeCategoryIds: Option[Seq[UUID]],
      maybeCatalogIds: Option[Seq[CatalogIdPostgres]],
    ): Query[Table, Record, Seq] =
    (maybeCategoryIds, maybeCatalogIds) match {
      case (Some(categoryIds), _) =>
        q.filter(p =>
          p.isVariantOfProductId
            .ifNull(p.id) in productCategoryDao.queryFindByCategoryIds(categoryIds, maybeCatalogIds).map(_.productId),
        ).sortBy(t => (t.name.asc, t.id))
      case (None, Some(catalogIds)) =>
        q.filter(p =>
          p.isVariantOfProductId.ifNull(p.id) in productCategoryDao.queryFindByCatalogIds(catalogIds).map(_.productId),
        ).sortBy(t => (t.name.asc, t.id))
      case _ => q
    }

  def updateStatusByIdAndMerchantId(
      id: UUID,
      merchantId: UUID,
      active: Boolean,
    ): Future[Boolean] = {
    val field = for { o <- nonDeletedTable if o.id === id && o.merchantId === merchantId } yield (o.active, o.updatedAt)
    run(field.update(active, UtcTime.now).map(_ > 0))
  }

  def queryFindMainByParentIds(parentIds: Seq[UUID]) =
    nonDeletedTable.filter(_.isVariantOfProductId inSet parentIds).filter(_.`type` inSet ArticleType.storables)

  def queryFindByIdWithVariants(id: UUID) =
    queryFindByIdsWithVariants(Seq(id))

  def queryFindByIdsWithVariantsIncludedDeleted(id: UUID) =
    targetTable.filter(t => t.id === id || t.isVariantOfProductId === id)

  def queryFindByIdsWithVariants(ids: Seq[UUID]) =
    nonDeletedTable.filter(t => t.id.inSet(ids) || t.isVariantOfProductId.inSet(ids))

  def deleteByLocationId(locationId: UUID, merchantId: UUID): Future[(Seq[UUID], Seq[UUID])] =
    for {
      productLocations <- productLocationDao.findByLocationId(locationId)
      productIds = productLocations.map(_.productId)
      deletedProductLocations <- productLocationDao.deleteByIds(productLocations.map(_.id))
      remainingProductLocations <- productLocationDao.findByItemIds(productIds)
      productIdsToDelete = productIds diff remainingProductLocations.map(_.productId)
      deletedProducts <- deleteByIdsAndMerchantId(productIdsToDelete, merchantId)
    } yield (deletedProductLocations, deletedProducts)

  def findAllByUpcsAndMerchantId(merchantId: UUID, upcs: Seq[String]): Future[Seq[Record]] = {
    val q = nonDeletedTable.filter(_.merchantId === merchantId).filter(_.upc.toLowerCase inSet upcs.map(_.toLowerCase))
    run(q.result)
  }

  def findAllBySkusAndMerchantId(merchantId: UUID, skus: Seq[String]): Future[Seq[Record]] = {
    val q = nonDeletedTable.filter(_.merchantId === merchantId).filter(_.sku.toLowerCase inSet skus.map(_.toLowerCase))
    run(q.result)
  }

  def findAllByMerchantIdAndName(merchantId: UUID, name: String): Future[Seq[Record]] = {
    val q = nonDeletedTable.filter(_.merchantId === merchantId).filter(_.name.toLowerCase === name.toLowerCase)
    run(q.result)
  }

  override def queryDeleteByIdsAndMerchantId(ids: Seq[UUID], merchantId: UUID) = {
    val field = for {
      o <- table if o.merchantId === merchantId && (o.id.inSet(ids) || o.isVariantOfProductId.inSet(ids))
    } yield (o.deletedAt, o.updatedAt)
    val deletionTime = Some(UtcTime.now)
    field.update(deletionTime, UtcTime.now).map(_ => ids)
  }

  def findTopPopularProducts(
      merchantId: UUID,
      locationIds: Seq[UUID],
    )(
      duration: Duration,
      offset: Int,
      limit: Int,
    ): Future[Seq[Record]] = {
    val query = for {
      productIdsWithCount <-
        orderItemDao
          .queryFindTopPopularProductIdsWithCount(merchantId, locationIds)(duration, offset, limit)
      productIds = productIdsWithCount.flatMap { case (pId, _) => pId }
      products <- queryFindByIds(productIds).result
    } yield productIds.flatMap(productId => products.find(_.id == productId))
    run(query)
  }

  def findAllByArticleTypes(articleTypes: Seq[ArticleType], merchantId: UUID): Future[Seq[Record]] = {
    val query =
      nonDeletedTable.filter(t => t.`type`.inSet(articleTypes) && t.merchantId === merchantId && t.deletedAt.isEmpty)
    run(query.result)
  }
}
