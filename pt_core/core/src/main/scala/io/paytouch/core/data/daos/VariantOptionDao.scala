package io.paytouch.core.data.daos

import java.util.UUID

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.core.data.daos.features.SlickProductDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ VariantOptionRecord, VariantOptionUpdate }
import io.paytouch.core.data.tables.VariantOptionsTable

class VariantOptionDao(implicit val ec: ExecutionContext, val db: Database) extends SlickProductDao {
  type Record = VariantOptionRecord
  type Update = VariantOptionUpdate
  type Table = VariantOptionsTable

  val table = TableQuery[Table]

  def findByVariantOptionTypeIds(variantOptionTypeIds: Seq[UUID]): Future[Seq[Record]] =
    if (variantOptionTypeIds.isEmpty)
      Future.successful(Seq.empty)
    else
      table
        .filter(_.variantOptionTypeId inSet variantOptionTypeIds)
        .result
        .pipe(run)

  def findByNamesAndMerchantId(names: Seq[String], merchantId: UUID): Future[Seq[Record]] =
    if (names.isEmpty)
      Future.successful(Seq.empty)
    else
      table
        .filter(t => t.name.inSet(names) && t.merchantId === merchantId)
        .result
        .pipe(run)
}
