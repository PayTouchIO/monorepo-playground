package io.paytouch.core.data.daos

import java.time.LocalDateTime
import java.util.UUID

import scala.concurrent._

import slick.jdbc.GetResult

import io.paytouch.implicits._

import io.paytouch.core.data.daos.features.SlickItemLocationDao
import io.paytouch.core.data.driver.CustomPlainImplicits._
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ CustomerLocationRecord, CustomerLocationUpdate }
import io.paytouch.core.data.model.enums.{ OrderStatus, PaymentStatus }
import io.paytouch.core.data.tables.CustomerLocationsTable

class CustomerLocationDao(
    val locationDao: LocationDao,
    val customerMerchantDao: CustomerMerchantDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickItemLocationDao {

  type Record = CustomerLocationRecord
  type Update = CustomerLocationUpdate
  type Table = CustomerLocationsTable

  val table = TableQuery[Table]

  implicit val l: LocationDao = locationDao

  def queryFindByLocationIdAndMerchantId(locationId: UUID, merchantId: UUID) =
    queryFindByLocationId(locationId).filter(_.merchantId === merchantId)

  def queryFindByLocationIdsAndMerchantId(
      locationIds: Seq[UUID],
      merchantId: UUID,
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
    ) =
    queryFindByLocationIds(locationIds).filter(t =>
      all(
        Some(t.merchantId === merchantId),
        from
          .orElse(to)
          .map(_ => t.customerId in customerMerchantDao.queryFindByMerchantId(merchantId, from, to).map(_.customerId)),
      ),
    )

  def queryFindByCustomerIdAndLocationIds(customerId: UUID, locationIds: Seq[UUID]) =
    table.filter(_.customerId === customerId).filterByLocationIds(locationIds)

  def queryByRelIds(customerLocationUpdate: Update) = {
    require(
      customerLocationUpdate.customerId.isDefined,
      "CustomerLocationDao - Impossible to find by customer id and location id without a customer id",
    )
    require(
      customerLocationUpdate.locationId.isDefined,
      "CustomerLocationDao - Impossible to find by customer id and location id without a location id",
    )
    queryFindByItemIdAndLocationId(customerLocationUpdate.customerId.get, customerLocationUpdate.locationId.get)
  }

  def queryBulkUpsertAndDeleteTheRestByCustomerIds(customerLocations: Seq[Update], customerIds: Seq[UUID]) =
    queryBulkUpsertAndDeleteTheRestByRelIds(customerLocations, t => t.customerId inSet customerIds)

  def findByCustomerIdAndLocationIds(customerId: UUID, locationIds: Seq[UUID]): Future[Seq[Record]] =
    if (locationIds.isEmpty)
      Future.successful(Seq.empty)
    else
      queryFindByCustomerIdAndLocationIds(customerId, locationIds)
        .result
        .pipe(run)

  def findByItemIds(customerIds: Seq[UUID], locationId: Option[UUID]): Future[Seq[Record]] =
    if (customerIds.isEmpty)
      Future.successful(Seq.empty)
    else
      queryFindByItemIds(customerIds)
        .filter(t => all(locationId.map(lId => t.locationId === lId)))
        .result
        .pipe(run)

  def findOneByCustomerIdAndLocationId(customerId: UUID, locationId: UUID): Future[Option[Record]] =
    queryFindByItemIdAndLocationId(customerId, locationId)
      .result
      .headOption
      .pipe(run)

  def getTotalsPerCustomer(customerIds: Seq[UUID], locationIds: Seq[UUID]): Future[Map[UUID, (UUID, BigDecimal, Int)]] =
    if (customerIds.isEmpty || locationIds.isEmpty)
      Future.successful(Map.empty)
    else
      table
        .filter(_.customerId inSet customerIds)
        .filterByLocationIds(locationIds)
        .groupBy(_.customerId)
        .map {
          case (customerId, rows) =>
            val totalSpendAmount = rows.map(_.totalSpendAmount).sum.getOrElse(BigDecimal(0))
            val totalVisits = rows.map(_.totalVisits).sum.getOrElse(0)
            (customerId, totalSpendAmount, totalVisits)
        }
        .result
        .pipe(run)
        .map(_.map { case tuple @ (customerId, _, _) => customerId -> tuple }.toMap)

  def getTotalSpendPerGroup(
      groupIds: Seq[UUID],
      customerIds: Seq[UUID],
      locationIds: Seq[UUID],
    ): Future[Map[UUID, Seq[BigDecimal]]] =
    if (groupIds.isEmpty || customerIds.isEmpty || locationIds.isEmpty)
      Future.successful(Map.empty)
    else {
      implicit val getMapResult = GetResult(r => (r.nextUUID(), r.nextBigDecimal()))

      sql"""SELECT g.id, SUM(cl.total_spend_amount)
            FROM groups g, customer_groups cg, customer_locations cl
            WHERE g.id = cg.group_id
            AND cg.customer_id = cl.customer_id
            AND g.id IN (#${groupIds.asInParametersList})
            AND cg.customer_id IN (#${customerIds.asInParametersList})
            AND cl.location_id IN (SELECT id FROM locations WHERE id IN (#${locationIds.asInParametersList}) AND deleted_at IS NULL)
            GROUP BY g.id
            ;"""
        .as[(UUID, BigDecimal)]
        .pipe(run)
        .map(_.groupBy { case (groupId, _) => groupId }
          .transform((_, v) => v.map { case (_, value) => value }))
    }

  def getTotalVisitsPerGroup(
      groupIds: Seq[UUID],
      customerIds: Seq[UUID],
      locationIds: Seq[UUID],
    ): Future[Map[UUID, Int]] =
    if (groupIds.isEmpty || customerIds.isEmpty || locationIds.isEmpty)
      Future.successful(Map.empty)
    else {
      implicit val getMapResult = GetResult(r => (r.nextUUID(), r.nextInt()))

      sql"""SELECT g.id, SUM(cl.total_visits)
            FROM groups g, customer_groups cg, customer_locations cl
            WHERE g.id = cg.group_id
            AND cg.customer_id = cl.customer_id
            AND g.id IN (#${groupIds.asInParametersList})
            AND cg.customer_id IN (#${customerIds.asInParametersList})
            AND cl.location_id IN (SELECT id FROM locations WHERE id IN (#${locationIds.asInParametersList}) AND deleted_at IS NULL)
            GROUP BY g.id
            ;"""
        .as[(UUID, Int)]
        .pipe(run)
        .map(_.toMap)
    }

  def queryUpsertByCustomerIdAndLocationId(upsertion: Update) =
    queryUpsertByQuery(upsertion, queryByRelIds(upsertion))

  def upsertSpendingActivityByCustomerIdAndLocationId(upsertion: Update): Future[Record] =
    (for {
      totalVisits <- asOption(querySelectTotalVisitsByCustomerIdAndLocationId(upsertion))
        .map(_.headOption.flatMap(_.headOption))
      totalSpendAmount <- asOption(querySelectTotalSpendAmountByCustomerIdAndLocationId(upsertion))
        .map(_.headOption.flatMap(_.headOption))
      update = upsertion.copy(totalVisits = totalVisits, totalSpendAmount = totalSpendAmount)
      (resultType, record) <- queryUpsertByCustomerIdAndLocationId(update)
    } yield record).pipe(runWithTransaction)

  private def querySelectTotalVisitsByCustomerIdAndLocationId(upsertion: Update) =
    for {
      lId <- upsertion.locationId
      cId <- upsertion.customerId
    } yield sql"""SELECT COUNT(*) as total_visits
                  FROM orders
                  WHERE location_id = '#$lId'
                  AND customer_id = '#$cId'
                  AND status != ${OrderStatus.Canceled.entryName}
                  GROUP BY customer_id, location_id
                  LIMIT 1""".as[Int]

  private def querySelectTotalSpendAmountByCustomerIdAndLocationId(upsertion: Update) =
    for {
      lId <- upsertion.locationId
      cId <- upsertion.customerId
    } yield sql"""SELECT SUM(total_amount)
                  FROM orders
                  WHERE location_id = '#$lId'
                  AND customer_id = '#$cId'
                  AND payment_status IN (#${PaymentStatus.isPositive.map(_.entryName).asInParametersList})
                  GROUP BY customer_id, location_id
                  LIMIT 1""".as[BigDecimal]
}
