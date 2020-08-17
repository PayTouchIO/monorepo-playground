package io.paytouch.core.data.daos

import java.time.LocalDateTime
import java.util.UUID

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.core.data.daos.features.SlickRelDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ CustomerGroupRecord, CustomerGroupUpdate }
import io.paytouch.core.data.tables.CustomerGroupsTable

class CustomerGroupDao(
    val customerLocationDao: CustomerLocationDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickRelDao {
  type Record = CustomerGroupRecord
  type Update = CustomerGroupUpdate
  type Table = CustomerGroupsTable

  val table = TableQuery[Table]

  def queryFindByGroupIdAndMerchantId(groupId: UUID, merchantId: UUID) =
    table.filter(_.groupId === groupId).filter(_.merchantId === merchantId)

  def queryFindByGroupIdsMerchantIdLocationIds(
      groupIds: Seq[UUID],
      merchantId: UUID,
      locationIds: Seq[UUID],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
    ) =
    table
      .filter(_.groupId inSet groupIds)
      .filter(_.merchantId === merchantId)
      .filter(
        _.customerId in customerLocationDao
          .queryFindByLocationIdsAndMerchantId(locationIds, merchantId, from, to)
          .map(_.customerId),
      )

  def queryFindByGroupIdAndCustomerId(groupId: UUID, customerId: UUID) =
    table.filter(t => t.groupId === groupId && t.customerId === customerId)

  def queryByRelIds(customerGroup: CustomerGroupUpdate) = {
    require(
      customerGroup.groupId.isDefined,
      "CustomerGroupDao - Impossible to find by group id and customer id without a group id",
    )

    require(
      customerGroup.customerId.isDefined,
      "CustomerGroupDao - Impossible to find by group id and customer id without a customer id",
    )

    queryFindByGroupIdAndCustomerId(customerGroup.groupId.get, customerGroup.customerId.get)
  }

  def queryBulkUpsertAndDeleteTheRestByGroupId(customerGroupUpdates: Seq[Update], groupId: UUID) =
    queryBulkUpsertAndDeleteTheRestByRelIds(customerGroupUpdates, t => t.groupId === groupId)

  def findByGroupIdAndMerchantId(groupId: UUID, merchantId: UUID) =
    queryFindByGroupIdAndMerchantId(groupId, merchantId)
      .result
      .pipe(run)

  def findByGroupIdsAndMerchantId(
      groupIds: Seq[UUID],
      merchantId: UUID,
      locationIds: Seq[UUID],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
    ): Future[Seq[Record]] =
    if (groupIds.isEmpty || locationIds.isEmpty)
      Future.successful(Seq.empty)
    else
      queryFindByGroupIdsMerchantIdLocationIds(groupIds, merchantId, locationIds, from, to)
        .result
        .pipe(run)

  def findByGroupId(groupId: UUID): Future[Seq[Record]] =
    table
      .filter(_.groupId === groupId)
      .result
      .pipe(run)

  def countByGroupIdsAndMerchantId(groupIds: Seq[UUID], merchantId: UUID): Future[Map[UUID, Int]] =
    if (groupIds.isEmpty)
      Future.successful(Map.empty)
    else
      table
        .filter(_.groupId inSet groupIds)
        .filter(_.merchantId === merchantId)
        .groupBy(_.groupId)
        .map {
          case (groupId, rows) => (groupId, rows.size)
        }
        .result
        .pipe(run)
        .map(_.toMap)
}
