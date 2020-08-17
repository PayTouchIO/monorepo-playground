package io.paytouch.core.data.daos

import java.util.UUID

import io.paytouch.core.data.daos.features.SlickRelDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ LoyaltyRewardRecord, LoyaltyRewardUpdate }
import io.paytouch.core.data.tables.LoyaltyRewardsTable

import scala.concurrent._

class LoyaltyRewardDao(implicit val ec: ExecutionContext, val db: Database) extends SlickRelDao {

  type Record = LoyaltyRewardRecord
  type Update = LoyaltyRewardUpdate
  type Table = LoyaltyRewardsTable

  val table = TableQuery[Table]

  def queryFindByLoyaltyProgramIds(loyaltyProgramIds: Seq[UUID]) =
    table.filter(_.loyaltyProgramId inSet loyaltyProgramIds)

  def findByLoyaltyProgramIds(loyaltyProgramIds: Seq[UUID]): Future[Seq[Record]] =
    run(queryFindByLoyaltyProgramIds(loyaltyProgramIds).result)

  def queryFindByLoyaltyProgramId(loyaltyProgramId: UUID) = queryFindByLoyaltyProgramIds(Seq(loyaltyProgramId))

  def findByLoyaltyProgramId(loyaltyProgramId: UUID): Future[Option[Record]] =
    run(queryFindByLoyaltyProgramId(loyaltyProgramId).result.headOption)

  def queryBulkUpsertAndDeleteTheRest(rewards: Seq[Update], loyaltyProgramId: UUID) =
    queryBulkUpsertAndDeleteTheRestByRelIds(rewards, t => t.loyaltyProgramId === loyaltyProgramId)

  def queryByRelIds(upsertion: Update) = {
    require(
      upsertion.loyaltyProgramId.isDefined,
      "LoyaltyRewardDao - Impossible to find by program id without a program id",
    )
    queryFindByLoyaltyProgramId(upsertion.loyaltyProgramId.get)
  }
}
