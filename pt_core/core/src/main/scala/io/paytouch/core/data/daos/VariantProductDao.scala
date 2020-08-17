package io.paytouch.core.data.daos

import java.util.UUID

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.core.data.daos.features.SlickSoftDeleteDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums.ArticleType
import io.paytouch.core.data.model.upsertions.VariantArticleUpsertion
import io.paytouch.core.data.tables.ArticlesTable

class VariantProductDao(
    val imageUploadDao: ImageUploadDao,
    productLocationDao: => ProductLocationDao,
    productLocationTaxRateDao: => ProductLocationTaxRateDao,
    productVariantOptionDao: => ProductVariantOptionDao,
    val supplierProductDao: SupplierProductDao,
    val variantOptionTypeDao: VariantOptionTypeDao,
    val variantOptionDao: VariantOptionDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickSoftDeleteDao {
  type Record = ArticleRecord
  type Update = ArticleUpdate
  type Table = ArticlesTable

  val table = TableQuery[Table]

  def queryUpsert(upsertion: VariantArticleUpsertion) =
    for {
      (resultType, product) <- super.queryUpsert(upsertion.product)
      productLocations <- productLocationDao.queryBulkUpsertAndDeleteTheRest(upsertion.productLocations, product.id)
      productVariantOptions <- asOption(
        upsertion
          .productVariantOptions
          .map(productVariantOptionDao.queryBulkUpsertAndDeleteTheRestByProductId(_, product.id)),
      )
      productLocationTaxRates <-
        productLocationTaxRateDao
          .queryBulkUpsertAndDeleteTheRest(upsertion.productLocationTaxRates, product.id)
    } yield product

  def bulkUpsertAndDeleteTheRestByParentId(products: Seq[Update], parentProductIds: Seq[UUID]) =
    runWithTransaction(queryBulkUpsertAndDeleteTheRestByParentIds(products, parentProductIds))

  def queryBulkUpsertAndDeleteTheRestByParentIds(products: Seq[Update], parentProductIds: Seq[UUID]) =
    for {
      us <- queryBulkUpsert(products)
      records = us.map { case (_, record) => record }
      _ <- queryDeleteTheRestByParentIds(records, parentProductIds)
    } yield records

  def queryDeleteTheRestByParentIds(records: Seq[Record], parentProductIds: Seq[UUID]) =
    queryDeleteTheRestByDeleteFilter(
      records,
      t => (t.isVariantOfProductId inSet parentProductIds) && (t.`type` === (ArticleType.Variant: ArticleType)),
    )

  def findVariantByParentId(mainProductId: UUID): Future[Seq[Record]] =
    findVariantsByParentIds(Seq(mainProductId))

  def findVariantsByParentIds(mainProductIds: Seq[UUID]): Future[Seq[Record]] =
    if (mainProductIds.isEmpty)
      Future.successful(Seq.empty)
    else
      queryFindVariantsByParentIds(mainProductIds)
        .result
        .pipe(run)

  def queryFindVariantsByParentIds(parentIds: Seq[UUID]) =
    nonDeletedTable
      .filter(_.isVariantOfProductId inSet parentIds)
      .filter(_.`type` === (ArticleType.Variant: ArticleType))

  def querySoftDeleteVariantsWithNoOptions(merchantId: UUID) = {
    val variantsPerMerchant =
      table
        .filter(_.merchantId === merchantId)
        .filter(_.`type` === (ArticleType.Variant: ArticleType))

    val optionsPerMerchant =
      productVariantOptionDao
        .table
        .filter(_.merchantId === merchantId)

    val unreferencedVariants = for {
      (variants, options) <-
        variantsPerMerchant
          .joinLeft(optionsPerMerchant)
          .on(_.id === _.productId) if options.isEmpty
    } yield variants.id

    for {
      unreferencedVariantIds <- unreferencedVariants.result
      deletedVariants <- queryDeleteByIdsAndMerchantId(unreferencedVariantIds, merchantId)
    } yield deletedVariants
  }
}
