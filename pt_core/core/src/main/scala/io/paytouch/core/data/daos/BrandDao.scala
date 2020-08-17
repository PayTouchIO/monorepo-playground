package io.paytouch.core.data.daos

import java.util.UUID

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.core.data.daos.features.{ SlickDefaultUpsertDao, SlickFindAllDao, SlickMerchantDao }
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ BrandRecord, BrandUpdate }
import io.paytouch.core.data.tables.BrandsTable
import io.paytouch.core.filters.NoFilters

class BrandDao(implicit val ec: ExecutionContext, val db: Database)
    extends SlickMerchantDao
       with SlickFindAllDao
       with SlickDefaultUpsertDao {
  type Record = BrandRecord
  type Update = BrandUpdate
  type Table = BrandsTable
  type Filters = NoFilters

  val table = TableQuery[Table]

  def findAllWithFilters(merchantId: UUID, filters: Filters)(offset: Int, limit: Int): Future[Seq[Record]] =
    findByMerchantIdWithPagination(merchantId)(offset, limit)

  def countAllWithFilters(merchantId: UUID, filters: Filters): Future[Int] =
    countAllByMerchantId(merchantId)

  private def findByMerchantIdWithPagination(merchantId: UUID)(offset: Int, limit: Int): Future[Seq[Record]] =
    findAllByMerchantIdWithOrdering(merchantId, offset, limit) { t: BrandsTable => t.name.asc }

  def findByNamesAndMerchantId(names: Seq[String], merchantId: UUID): Future[Seq[Record]] =
    if (names.isEmpty)
      Future.successful(Seq.empty)
    else
      table
        .filter(t => t.name.toLowerCase.inSet(names.map(_.toLowerCase)) && t.merchantId === merchantId)
        .result
        .pipe(run)
}
