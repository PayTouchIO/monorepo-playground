package io.paytouch.core.data.daos

import java.util.UUID

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.core.data.daos.features.SlickProductDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ VariantOptionTypeRecord, VariantOptionTypeUpdate }
import io.paytouch.core.data.tables.VariantOptionTypesTable

class VariantOptionTypeDao(implicit val ec: ExecutionContext, val db: Database) extends SlickProductDao {
  type Record = VariantOptionTypeRecord
  type Update = VariantOptionTypeUpdate
  type Table = VariantOptionTypesTable

  val table = TableQuery[Table]

  def findByNamesAndMerchantId(names: Seq[String], merchantId: UUID): Future[Seq[Record]] =
    if (names.isEmpty)
      Future.successful(Seq.empty)
    else
      table
        .filter(t => t.name.inSet(names) && t.merchantId === merchantId)
        .result
        .pipe(run)
}
