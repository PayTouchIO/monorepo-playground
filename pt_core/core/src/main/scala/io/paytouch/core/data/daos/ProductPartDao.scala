package io.paytouch.core.data.daos

import java.util.UUID

import io.paytouch.core.data.daos.features.{ SlickBulkUpsertDao, SlickFindAllDao, SlickProductDao }
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.upsertions.ProductPartUpsertion
import io.paytouch.core.data.model.{ ProductPartRecord, ProductPartUpdate }
import io.paytouch.core.data.tables.ProductPartsTable
import io.paytouch.core.filters.ProductPartFilters

import scala.concurrent._

class ProductPartDao(val articleDao: ArticleDao)(implicit val ec: ExecutionContext, val db: Database)
    extends SlickProductDao
       with SlickFindAllDao
       with SlickBulkUpsertDao {

  type Record = ProductPartRecord
  type Update = ProductPartUpdate
  type Filters = ProductPartFilters
  type Upsertion = ProductPartUpsertion
  type Table = ProductPartsTable

  val table = TableQuery[Table]

  override def bulkUpsert(upsertion: Upsertion): Future[Seq[Record]] = {
    val q = for {
      (_, product) <- articleDao.queryUpsert(upsertion.product)
      oldPartIds <- queryFindByProductId(product.id).map(_.partId).result
      results <- queryBulkUpsertAndDeleteTheRestByProductId(upsertion.productParts, product.id)
      productIds = upsertion.productParts.flatMap(_.productId)
      newPartIds = upsertion.productParts.flatMap(_.partId)
      _ <- articleDao.queryMarkAsUpdatedByIds(productIds ++ newPartIds ++ oldPartIds)
    } yield results
    runWithTransaction(q)
  }

  override def queryUpsert(update: Update) = {
    require(
      update.productId.isDefined,
      "ProductPartDao - impossible to upsert by product and part id without a product id",
    )
    require(update.partId.isDefined, "ProductPartDao - impossible to upsert by product and part id without a part id")
    val selectionQuery = queryFindOneByProductIdAndPartId(update.productId.get, update.partId.get)
    queryUpsertByQuery(update, selectionQuery)
  }

  private def queryFindOneByProductIdAndPartId(productId: UUID, partId: UUID) =
    table.filter(t => t.productId === productId && t.partId === partId)

  def findAllWithFilters(merchantId: UUID, f: Filters)(offset: Int, limit: Int): Future[Seq[ProductPartRecord]] =
    run(queryFindAllByMerchantId(merchantId, f.productId).drop(offset).take(limit).result)

  def countAllWithFilters(merchantId: UUID, f: Filters): Future[Int] =
    run(queryFindAllByMerchantId(merchantId, f.productId).length.result)

  def queryFindAllByMerchantId(merchantId: UUID, productId: UUID) =
    queryFindByProductId(productId).filter(_.merchantId === merchantId)

}
