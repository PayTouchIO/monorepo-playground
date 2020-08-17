package io.paytouch.core.data.daos

import java.util.UUID

import io.paytouch.core.data.daos.features.SlickMerchantDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ LoyaltyPointsHistoryRecord, LoyaltyPointsHistoryUpdate }
import io.paytouch.core.data.tables.LoyaltyPointsHistoryTable

import scala.concurrent._

class LoyaltyPointsHistoryDao(implicit val ec: ExecutionContext, val db: Database) extends SlickMerchantDao {

  type Record = LoyaltyPointsHistoryRecord
  type Update = LoyaltyPointsHistoryUpdate
  type Table = LoyaltyPointsHistoryTable

  val table = TableQuery[Table]

  def queryGetPointsBalance(loyaltyMembershipId: UUID) =
    table
      .filter(_.loyaltyMembershipId === loyaltyMembershipId)
      .map(_.points)
      .sum
      .result

  def queryBulkInsertOrSkip(upsertions: Seq[Update]) = asSeq(upsertions.map(queryInsertOrSkip))

  private def queryInsertOrSkip(upsertion: Update) =
    for {
      existing <- queryFindByDefiningColumns(upsertion).result.headOption
      inserted <-
        if (existing.isDefined) DBIO.successful(existing.get) // no-op
        else queryInsert(upsertion.merge(None))
    } yield inserted

  private def queryFindByDefiningColumns(upsertion: Update) = {
    require(
      upsertion.loyaltyMembershipId.isDefined,
      "Can't insert or skip a LoyaltyPointsHistoryUpdate without a loyaltyMembershipId",
    )
    require(upsertion.`type`.isDefined, "Can't insert or skip a LoyaltyPointsHistoryUpdate without a `type`")
    table
      .filter(_.loyaltyMembershipId === upsertion.loyaltyMembershipId.get)
      .filter(_.`type` === upsertion.`type`.get)
      .filter(_.objectId === upsertion.objectId)
      .filter(_.objectType === upsertion.objectType)
  }

  def getPointsBalanceByOrderIds(orderIds: Seq[UUID]): Future[Map[UUID, Int]] = {
    val query = table
      .filter(_.orderId inSet orderIds)
      .groupBy(_.orderId)
      .map { case (orderId, row) => orderId.get -> row.map(_.points).sum.getOrElse(0) }
    run(query.result).map(_.toMap)
  }
}
