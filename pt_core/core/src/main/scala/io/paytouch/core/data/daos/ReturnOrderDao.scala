package io.paytouch.core.data.daos

import java.time.LocalDateTime
import java.util.UUID

import io.paytouch.core.data.daos.features.{ SlickFindAllDao, SlickLocationTimeZoneHelper, SlickUpsertDao }
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.enums.ReturnOrderStatus
import io.paytouch.core.data.model.upsertions.ReturnOrderUpsertion
import io.paytouch.core.data.model.{ ReturnOrderRecord, ReturnOrderUpdate }
import io.paytouch.core.data.tables.ReturnOrdersTable
import io.paytouch.core.filters.ReturnOrderFilters
import io.paytouch.core.utils.ResultType

import scala.concurrent._

class ReturnOrderDao(
    val locationDao: LocationDao,
    val nextNumberDao: NextNumberDao,
    returnOrderProductDao: => ReturnOrderProductDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickFindAllDao
       with SlickUpsertDao
       with SlickLocationTimeZoneHelper {

  type Record = ReturnOrderRecord
  type Update = ReturnOrderUpdate
  type Upsertion = ReturnOrderUpsertion
  type Filters = ReturnOrderFilters
  type Table = ReturnOrdersTable

  val table = TableQuery[Table]

  def findAllWithFilters(merchantId: UUID, f: Filters)(offset: Int, limit: Int): Future[Seq[Record]] =
    findAllByMerchantId(merchantId, f.locationId, f.from, f.to, f.query, f.status)(offset, limit)

  def findAllByMerchantId(
      merchantId: UUID,
      locationId: Option[UUID],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
      query: Option[String],
      status: Option[ReturnOrderStatus],
    )(
      offset: Int,
      limit: Int,
    ): Future[Seq[Record]] = {
    val q = queryFindAllByMerchantId(merchantId, locationId, from, to, query, status).drop(offset).take(limit)
    run(q.result)
  }

  def countAllWithFilters(merchantId: UUID, f: Filters): Future[Int] =
    countAllByMerchantId(merchantId, f.locationId, f.from, f.to, f.query, f.status)

  def countAllByMerchantId(
      merchantId: UUID,
      locationId: Option[UUID],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
      query: Option[String],
      status: Option[ReturnOrderStatus],
    ): Future[Int] = {
    val q = queryFindAllByMerchantId(merchantId, locationId, from, to, query, status).length
    run(q.result)
  }

  def queryFindAllByMerchantId(
      merchantId: UUID,
      locationId: Option[UUID],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
      query: Option[String],
      status: Option[ReturnOrderStatus],
    ) =
    table
      .filter(t =>
        all(
          Some(t.merchantId === merchantId),
          locationId.map(lId => t.locationId === lId),
          from.map(start => t.id in itemIdsAtOrAfterCreatedAtDate(start)),
          to.map(end => t.id in itemIdsBeforeCreatedAtDate(end)),
          query.map(q => t.number.toLowerCase like s"${q.toLowerCase}%"),
          status.map(st => t.status === st),
        ),
      )
      .sortBy(_.createdAt)

  def queryFindByPurchaseOrderIds(purchaseOrderIds: Seq[UUID]) =
    table
      .filter(_.purchaseOrderId inSet purchaseOrderIds)

  def countReturnedProductsByPurchaseOrderIds(purchaseOrderIds: Seq[UUID]): Future[Map[UUID, BigDecimal]] =
    run(queryCountReturnedOrderedProductsByPurchaseOrderIds(purchaseOrderIds).result).map(_.toMap)

  def queryCountReturnedOrderedProductsByPurchaseOrderIds(purchaseOrderIds: Seq[UUID]) =
    table
      .filter(_.purchaseOrderId inSet purchaseOrderIds)
      .join(returnOrderProductDao.table)
      .on(_.id === _.returnOrderId)
      .filter {
        case (returnOrdersT, returnOrderProductsT) =>
          returnOrdersT.purchaseOrderId.isDefined && returnOrderProductsT.quantity.isDefined
      }
      .groupBy { case (returnOrdersT, _) => returnOrdersT.purchaseOrderId }
      .map {
        case (purchaseOrderId, rows) =>
          purchaseOrderId.get -> rows.map { case (_, ropt) => ropt.quantity }.sum.get
      }

  override def queryInsert(entity: Record) =
    entity match {
      case e if e.number.isEmpty => queryInsertWithIncrementedNumber(e)
      case _                     => super.queryInsert(entity)
    }

  private def queryInsertWithIncrementedNumber(entity: Record) =
    for {
      returnOrderNumber <- nextNumberDao.queryNextReturnOrderNumberForMerchantId(entity.merchantId)
      entityWithReturnOrderNumber = entity.copy(number = returnOrderNumber.toString)
      insert <- table returning table += entityWithReturnOrderNumber
    } yield insert

  def upsert(upsertion: Upsertion): Future[(ResultType, Record)] = {
    val q = for {
      (resultType, returnOrder) <- queryUpsert(upsertion.returnOrder)
      _ <- asOption(
        upsertion
          .returnOrderProducts
          .map(returnOrderProductDao.queryBulkUpsertAndDeleteTheRestByReturnOrderId(_, returnOrder.id)),
      )
    } yield (resultType, returnOrder)
    runWithTransaction(q)
  }

  def markAsReturnedAndSynced(id: UUID): Future[(ResultType, Record)] = {
    val update = ReturnOrderUpdate(
      id = Some(id),
      merchantId = None,
      userId = None,
      supplierId = None,
      locationId = None,
      purchaseOrderId = None,
      notes = None,
      status = Some(ReturnOrderStatus.Sent),
      synced = Some(true),
    )
    upsert(update)
  }
}
