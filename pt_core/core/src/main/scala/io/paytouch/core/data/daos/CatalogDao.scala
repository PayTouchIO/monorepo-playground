package io.paytouch.core.data.daos

import java.util.UUID

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.core.Availabilities
import io.paytouch.core.data.daos.features._
import io.paytouch.core.data.daos.features.SlickUpsertDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ CatalogRecord, CatalogUpdate }
import io.paytouch.core.data.model.upsertions.CatalogUpsertion
import io.paytouch.core.data.tables.CatalogsTable
import io.paytouch.core.entities.enums.CatalogType
import io.paytouch.core.filters._
import io.paytouch.core.utils.ResultType

class CatalogDao(
    categoryDao: => CategoryDao,
    productCategoryDao: => ProductCategoryDao,
    catalogAvailabilityDao: => CatalogAvailabilityDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickMerchantDao
       with SlickFindAllDao
       with SlickUpsertDao {
  type Record = CatalogRecord
  type Update = CatalogUpdate
  type Upsertion = CatalogUpsertion
  type Table = CatalogsTable
  type Filters = CatalogFilters

  val table = TableQuery[Table]

  def findAllWithFilters(merchantId: UUID, f: Filters)(offset: Int, limit: Int) =
    findAllByMerchantId(merchantId, f.ids)(offset, limit)

  def countAllWithFilters(merchantId: UUID, f: Filters) =
    countAllByMerchantId(merchantId, f.ids)

  def findAllByMerchantId(
      merchantId: UUID,
      ids: Option[Seq[UUID]] = None,
    )(
      offset: Int,
      limit: Int,
    ): Future[Seq[Record]] =
    queryFindAllByMerchantId(merchantId, ids)
      .sortBy(_.name.asc)
      .drop(offset)
      .take(limit)
      .result
      .pipe(run)

  def countAllByMerchantId(
      merchantId: UUID,
      ids: Option[Seq[UUID]] = None,
    ): Future[Int] =
    queryFindAllByMerchantId(merchantId, ids)
      .length
      .result
      .pipe(run)

  def queryFindAllByMerchantId(
      merchantId: UUID,
      ids: Option[Seq[UUID]] = None,
    ) =
    table.filter { t =>
      all(
        Some(t.merchantId === merchantId),
        ids.map(_ids => t.id inSet _ids),
      )
    }

  def productsCountByCatalogs(ids: Seq[UUID]): Future[Map[Record, Int]] = {
    val q = baseQuery
      .filter(_.id inSet ids)
      .join(categoryDao.baseQuery)
      .on(_.id === _.catalogId)
      .join(productCategoryDao.baseQuery)
      .on { case ((_, ccs), pcs) => ccs.id === pcs.categoryId }
      .groupBy { case ((cs, _), _) => cs }
      .map { case (record, q) => record -> q.length }

    run(q.result).map(_.toMap)
  }

  def upsert(upsertion: Upsertion): Future[(ResultType, Record)] = {
    val upserts = for {
      (resultType, catalog) <- queryUpsert(upsertion.catalog)
      _ <-
        catalogAvailabilityDao
          .queryBulkUpsertAndDeleteTheRestByItemIds(upsertion.availabilities, Seq(catalog.id))
    } yield (resultType, catalog)

    runWithTransaction(upserts)
  }

  def queryFindByType(catalogType: CatalogType) =
    table.filter(_.`type` === catalogType)

  def queryFindByMerchantIdAndType(merchantId: UUID, catalogType: CatalogType) =
    table.filter(_.merchantId === merchantId).filter(_.`type` === catalogType)

  def findByMerchantIdAndType(merchantId: UUID, catalogType: CatalogType): Future[Option[Record]] =
    run(queryFindByMerchantIdAndType(merchantId, catalogType).result.headOption)
}
