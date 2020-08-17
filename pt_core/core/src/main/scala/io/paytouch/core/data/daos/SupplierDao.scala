package io.paytouch.core.data.daos

import java.util.UUID

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.core.data.daos.features.{ SlickFindAllDao, SlickMerchantDao, SlickSoftDeleteDao, SlickUpsertDao }
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ SupplierRecord, SupplierUpdate }
import io.paytouch.core.data.model.upsertions.SupplierUpsertion
import io.paytouch.core.data.tables.SuppliersTable
import io.paytouch.core.entities.SupplierInfo
import io.paytouch.core.filters.SupplierFilters
import io.paytouch.core.utils.ResultType

class SupplierDao(
    supplierLocationDao: => SupplierLocationDao,
    supplierProductDao: => SupplierProductDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickMerchantDao
       with SlickFindAllDao
       with SlickUpsertDao
       with SlickSoftDeleteDao {
  type Record = SupplierRecord
  type Update = SupplierUpdate
  type Upsertion = SupplierUpsertion
  type Filters = SupplierFilters
  type Table = SuppliersTable

  val table = TableQuery[Table]

  def findAllWithFilters(merchantId: UUID, f: Filters)(offset: Int, limit: Int): Future[Seq[Record]] =
    findAllByMerchantId(merchantId = merchantId, f.locationIds, f.categoryIds, f.query)(offset, limit)

  def findAllByMerchantId(
      merchantId: UUID,
      locationIds: Option[Seq[UUID]] = None,
      categoryIds: Option[Seq[UUID]] = None,
      query: Option[String] = None,
    )(
      offset: Int,
      limit: Int,
    ): Future[Seq[Record]] =
    queryFindAllByMerchantId(merchantId, locationIds, categoryIds, query)
      .sortBy(_.name.asc)
      .drop(offset)
      .take(limit)
      .result
      .pipe(run)

  def countAllWithFilters(merchantId: UUID, f: Filters): Future[Int] =
    countAllByMerchantId(merchantId, f.locationIds, f.categoryIds, f.query)

  def countAllByMerchantId(
      merchantId: UUID,
      locationIds: Option[Seq[UUID]] = None,
      categoryIds: Option[Seq[UUID]] = None,
      query: Option[String] = None,
    ): Future[Int] =
    queryFindAllByMerchantId(merchantId, locationIds, categoryIds, query)
      .length
      .result
      .pipe(run)

  def queryFindAllByMerchantId(
      merchantId: UUID,
      locationIds: Option[Seq[UUID]] = None,
      categoryIds: Option[Seq[UUID]] = None,
      query: Option[String] = None,
    ) =
    nonDeletedTable.filter { t =>
      all(
        Some(t.merchantId === merchantId),
        locationIds.map(lIds => t.id in supplierLocationDao.queryFindByLocationIds(lIds).map(_.supplierId)),
        categoryIds.map(cIds => t.id in supplierProductDao.queryFindByCategoryIds(cIds).map(_.supplierId)),
        query.map(q => t.name.toLowerCase like s"%${q.toLowerCase}%"),
      )
    }

  def findAllSupplierInfoByProductIds(productIds: Seq[UUID]): Future[Seq[(UUID, SupplierInfo)]] =
    if (productIds.isEmpty)
      Future.successful(Seq.empty)
    else
      nonDeletedTable
        .join(supplierProductDao.queryFindByProductIds(productIds))
        .on(_.id === _.supplierId)
        .map {
          case (suppliersTable, supplierProductsTable) =>
            supplierProductsTable.productId -> suppliersTable.supplierInfo
        }
        .result
        .pipe(run)

  def upsert(upsertion: Upsertion): Future[(ResultType, Record)] =
    (for {
      (resultType, supplier) <- queryUpsert(upsertion.supplier)
      _ <- supplierLocationDao.queryBulkUpsertAndDeleteTheRest(upsertion.supplierLocations, supplier.id)
      _ <- asOption(
        upsertion
          .supplierProducts
          .map(supplierProductDao.queryBulkUpsertAndDeleteTheRestBySupplierIds(_, Seq(supplier.id))),
      )
    } yield (resultType, supplier)).pipe(runWithTransaction)

  def findByNamesAndMerchantId(names: Seq[String], merchantId: UUID): Future[Seq[Record]] =
    if (names.isEmpty)
      Future.successful(Seq.empty)
    else
      table
        .filter(t => t.name.toLowerCase.inSet(names.map(_.toLowerCase)) && t.merchantId === merchantId)
        .result
        .pipe(run)
}
