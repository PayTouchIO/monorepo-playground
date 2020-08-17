package io.paytouch.core.data.daos

import java.util.UUID

import io.paytouch.core.data.daos.features.SlickRelDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ SupplierProductRecord, SupplierProductUpdate }
import io.paytouch.core.data.tables.SupplierProductsTable

import scala.concurrent._

class SupplierProductDao(
    val productCategoryDao: ProductCategoryDao,
    val supplierLocationDao: SupplierLocationDao,
    articleDao: => ArticleDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickRelDao {

  type Record = SupplierProductRecord
  type Update = SupplierProductUpdate
  type Table = SupplierProductsTable

  val table = TableQuery[Table]

  def queryDeleteByProductId(productId: UUID) =
    queryFindByProductId(productId).delete

  def findByProductId(productId: UUID): Future[Seq[Record]] =
    run(queryFindByProductId(productId).result)

  def findBySupplierId(supplierId: UUID): Future[Seq[Record]] =
    findBySupplierIds(Seq(supplierId))

  def queryFindByProductId(productId: UUID) = queryFindByProductIds(Seq(productId))

  def queryFindByProductIds(productIds: Seq[UUID]) = table.filter(_.productId inSet productIds)

  def queryFindBySupplierIds(supplierIds: Seq[UUID]) = table.filter(_.supplierId inSet supplierIds)

  def queryFindBySupplierId(supplierId: UUID) = queryFindBySupplierIds(Seq(supplierId))

  def findBySupplierIds(supplierIds: Seq[UUID]) = run(queryFindBySupplierIds(supplierIds).result)

  def findBySupplierIdsAndProductIds(supplierIds: Seq[UUID], productIds: Seq[UUID]) = {
    val q = table
      .filter(_.productId inSet productIds)
      .filter(_.supplierId inSet supplierIds)
    run(q.result)
  }

  def queryFindByProductIdAndSupplierId(productId: UUID, supplierId: UUID) =
    table.filter(t => t.productId === productId && t.supplierId === supplierId)

  def queryByRelIds(supplierProduct: Update) = {
    require(
      supplierProduct.productId.isDefined,
      "SupplierProductDao - Impossible to find by product id and supplier id without a product id",
    )
    require(
      supplierProduct.supplierId.isDefined,
      "SupplierProductDao - Impossible to find by product id and supplier id without a supplier id",
    )
    queryFindByProductIdAndSupplierId(supplierProduct.productId.get, supplierProduct.supplierId.get)
  }

  def queryBulkUpsertAndDeleteTheRestByProductIds(supplierProducts: Seq[Update], productIds: Seq[UUID]) =
    queryBulkUpsertAndDeleteTheRestByRelIds(supplierProducts, t => t.productId inSet productIds)

  def queryFindByCategoryIds(categoryIds: Seq[UUID]) =
    table.filter(filterByCategoryIds(_, categoryIds))

  def filterByCategoryIds(t: Table, categoryIds: Seq[UUID]) =
    t.productId in productCategoryDao.queryFindByCategoryIds(categoryIds).map(_.productId)

  def filterByLocationIds(t: Table, locationIds: Seq[UUID]) =
    t.supplierId in supplierLocationDao.queryFindByLocationIds(locationIds).map(_.supplierId)

  def countProductsBySupplierIds(
      ids: Seq[UUID],
      locationIds: Option[Seq[UUID]],
      categoryIds: Option[Seq[UUID]],
    ): Future[Map[UUID, Int]] = {
    val query = queryFindBySupplierIds(ids)
      .filter { t =>
        all(
          Some(t.productId in articleDao.nonDeletedTable.map(_.id)),
          locationIds.map(lIds => filterByLocationIds(t, lIds)),
          categoryIds.map(cIds => filterByCategoryIds(t, cIds)),
        )
      }
      .groupBy(_.supplierId)
      .map { case (supplierId, rows) => (supplierId, rows.size) }

    run(query.result).map(_.toMap)
  }

  def queryBulkUpsertAndDeleteTheRestBySupplierIds(supplierProducts: Seq[Update], supplierIds: Seq[UUID]) =
    queryBulkUpsertAndDeleteTheRestByRelIds(supplierProducts, t => t.supplierId inSet supplierIds)
}
