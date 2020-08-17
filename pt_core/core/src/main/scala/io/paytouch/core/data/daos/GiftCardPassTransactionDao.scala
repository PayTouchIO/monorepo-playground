package io.paytouch.core.data.daos

import java.util.UUID

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.core.data.daos.features.SlickMerchantDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ GiftCardPassTransactionRecord, GiftCardPassTransactionUpdate }
import io.paytouch.core.data.tables.GiftCardPassTransactionsTable

class GiftCardPassTransactionDao(implicit val ec: ExecutionContext, val db: Database) extends SlickMerchantDao {
  type Record = GiftCardPassTransactionRecord
  type Update = GiftCardPassTransactionUpdate
  type Table = GiftCardPassTransactionsTable

  val table = TableQuery[Table]

  def findByGiftCardPassId(giftCardPassId: UUID): Future[Seq[Record]] =
    findByGiftCardPassIds(Seq(giftCardPassId))

  def findByGiftCardPassIds(giftCardPassIds: Seq[UUID]): Future[Seq[Record]] =
    if (giftCardPassIds.isEmpty)
      Future.successful(Seq.empty)
    else
      table
        .filter(_.giftCardPassId inSet giftCardPassIds)
        .result
        .pipe(run)
}
