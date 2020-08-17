package io.paytouch.core.data.daos

import java.time.ZonedDateTime
import java.util.UUID

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.core.data.daos.features.{ SlickMerchantDao, SlickRelDao, SlickUaPassUrls }
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ LoyaltyMembershipRecord, LoyaltyMembershipUpdate, LoyaltyPointsHistoryUpdate }
import io.paytouch.core.data.tables.LoyaltyMembershipsTable
import io.paytouch.core.filters.LoyaltyMembershipFilter
import io.paytouch.core.utils.UtcTime

class LoyaltyMembershipDao(
    loyaltyPointsHistoryDao: LoyaltyPointsHistoryDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickMerchantDao
       with SlickRelDao
       with SlickUaPassUrls {
  type Record = LoyaltyMembershipRecord
  type Update = LoyaltyMembershipUpdate
  type Table = LoyaltyMembershipsTable

  val table = TableQuery[Table]

  def findByCustomerIds(
      merchantId: UUID,
      customerIds: Seq[UUID],
      filter: LoyaltyMembershipFilter,
    ): Future[Seq[Record]] =
    findByCustomerIds(
      merchantId,
      customerIds,
      filter.loyaltyProgramId,
      filter.updatedSince,
    )

  def findByCustomerIds(
      merchantId: UUID,
      customerIds: Seq[UUID],
      loyaltyProgramId: Option[UUID],
      updatedSince: Option[ZonedDateTime],
    ): Future[Seq[Record]] =
    if (customerIds.isEmpty)
      Future.successful(Seq.empty)
    else
      queryFindByCustomerIds(merchantId, customerIds, loyaltyProgramId, updatedSince)
        .result
        .pipe(run)

  def findByCustomerId(merchantId: UUID, customerId: UUID): Future[Seq[Record]] =
    queryFindByCustomerIds(merchantId, Seq(customerId))
      .result
      .pipe(run)

  def findByCustomerIdAndLoyaltyProgramId(
      merchantId: UUID,
      customerId: UUID,
      loyaltyProgramId: UUID,
    ): Future[Option[Record]] =
    queryFindByCustomerIds(merchantId, Seq(customerId))
      .filter(_.loyaltyProgramId === loyaltyProgramId)
      .result
      .headOption
      .pipe(run)

  def findByLookupId(lookupId: String): Future[Option[Record]] =
    table
      .filter(_.lookupId === lookupId)
      .result
      .headOption
      .pipe(run)

  private def queryFindByCustomerIds(
      merchantId: UUID,
      customerIds: Seq[UUID],
      loyaltyProgramId: Option[UUID] = None,
      updatedSince: Option[ZonedDateTime] = None,
    ) =
    table
      .filter(_.merchantId === merchantId)
      .filter(_.customerId inSet customerIds)
      .filter { t =>
        all(
          loyaltyProgramId.map(lpId => t.loyaltyProgramId === lpId),
          updatedSince.map(date => t.id in queryUpdatedSince(date).map(_.id)),
        )
      }

  def queryByRelIds(upsertion: Update): Query[Table, Record, Seq] = {
    require(upsertion.merchantId.isDefined, "LoyaltyMembershipDao - Impossible to find by rel ids without a merchantId")
    require(upsertion.customerId.isDefined, "LoyaltyMembershipDao - Impossible to find by rel ids without a customerId")
    require(
      upsertion.loyaltyProgramId.isDefined,
      "LoyaltyMembershipDao - Impossible to find by rel ids without a loyaltyProgramId",
    )

    queryByRelIds(
      upsertion.merchantId.get,
      upsertion.customerId.get,
      upsertion.loyaltyProgramId.get,
    )
  }

  def queryByRelIds(
      merchantId: UUID,
      customerId: UUID,
      loyaltyProgramId: UUID,
    ): Query[Table, Record, Seq] =
    table
      .filter(t =>
        all(
          Some(t.merchantId === merchantId),
          Some(t.customerId === customerId),
          Some(t.loyaltyProgramId === loyaltyProgramId),
        ),
      )

  def findByRelIds(
      merchantId: UUID,
      customerId: UUID,
      loyaltyProgramId: UUID,
    ) =
    queryByRelIds(merchantId, customerId, loyaltyProgramId)
      .result
      .headOption
      .pipe(run)

  def bulkLogPointsAndUpdateBalance(
      historyUpdatesLoyaltyMembership: Map[UUID, Seq[LoyaltyPointsHistoryUpdate]],
    ): Future[Seq[Int]] =
    if (historyUpdatesLoyaltyMembership.isEmpty)
      Future.successful(Seq.empty)
    else
      historyUpdatesLoyaltyMembership
        .map { case (id, upds) => queryLogPointsAndUpdateBalance(id, upds) }
        .toSeq
        .pipe(asSeq)
        .pipe(runWithTransaction)

  def queryLogPointsAndUpdateBalance(loyaltyMembershipId: UUID, historyUpdates: Seq[LoyaltyPointsHistoryUpdate]) =
    for {
      _ <- loyaltyPointsHistoryDao.queryBulkInsertOrSkip(historyUpdates)
      historySum <- loyaltyPointsHistoryDao.queryGetPointsBalance(loyaltyMembershipId)
      sum = historySum.getOrElse(0)
      _ <- queryUpdateBalanceById(loyaltyMembershipId, historySum.getOrElse(0))
    } yield sum

  private def queryUpdateBalanceById(loyaltyMembershipId: UUID, points: Int) =
    table
      .withFilter(_.id === loyaltyMembershipId)
      .map(o => o.points -> o.updatedAt)
      .update(points, UtcTime.now)
      .map(_ > 0)
}
