package io.paytouch.core.data.daos

import java.util.UUID

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.core.data.daos.features.SlickMerchantDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ ArticleIdentifierRecord, ArticleIdentifierUpdate }
import io.paytouch.core.data.model.enums.ArticleType
import io.paytouch.core.data.tables.ArticleIdentifiersTable

class ArticleIdentifierDao(implicit val ec: ExecutionContext, val db: Database) extends SlickMerchantDao {

  type Record = ArticleIdentifierRecord
  type Update = ArticleIdentifierUpdate
  type Table = ArticleIdentifiersTable

  val table = TableQuery[Table]

  def findPotentialMatch(
      merchantId: UUID,
      `type`: ArticleType,
      names: Seq[String],
      upcs: Seq[String],
      skus: Seq[String],
      variantOptionIdentifiers: Seq[String],
    ): Future[Seq[Record]] =
    if (names.isEmpty)
      Future.successful(Seq.empty)
    else
      table
        .filter(t => t.merchantId === merchantId && t.`type` === `type`)
        .filter(t =>
          t.name.inSet(names) ||
            t.upc.inSet(upcs) ||
            t.sku.inSet(skus) ||
            (t.sku.inSet(skus) && t.variantOptions.inSet(variantOptionIdentifiers)),
        )
        .result
        .pipe(run)
}
