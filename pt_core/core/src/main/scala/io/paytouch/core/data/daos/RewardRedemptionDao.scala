package io.paytouch.core.data.daos

import java.util.UUID

import io.paytouch.core.data.daos.features.SlickMerchantDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ RewardRedemptionRecord, RewardRedemptionUpdate }
import io.paytouch.core.data.tables.RewardRedemptionTable

import scala.concurrent._

class RewardRedemptionDao(implicit val ec: ExecutionContext, val db: Database) extends SlickMerchantDao {

  type Record = RewardRedemptionRecord
  type Update = RewardRedemptionUpdate
  type Table = RewardRedemptionTable

  val table = TableQuery[Table]

  def queryFindByOrderIds(orderIds: Seq[UUID]) =
    table.filter(_.orderId inSet orderIds)

  def findPerOrderIds(orderIds: Seq[UUID]): Future[Seq[Record]] =
    run(queryFindByOrderIds(orderIds).result)
}
