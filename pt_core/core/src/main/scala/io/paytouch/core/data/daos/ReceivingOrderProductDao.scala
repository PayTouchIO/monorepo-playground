package io.paytouch.core.data.daos

import java.util.UUID

import io.paytouch.core.data.daos.features.{ SlickFindAllDao, SlickMerchantDao }
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ ReceivingOrderProductRecord, ReceivingOrderProductUpdate }
import io.paytouch.core.data.tables.ReceivingOrderProductsTable
import io.paytouch.core.entities.MonetaryAmount
import io.paytouch.core.filters.ReceivingOrderProductFilters

import scala.concurrent._

class ReceivingOrderProductDao(
    receivingOrderDao: => ReceivingOrderDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickMerchantDao
       with SlickFindAllDao {

  type Record = ReceivingOrderProductRecord
  type Update = ReceivingOrderProductUpdate
  type Filters = ReceivingOrderProductFilters
  type Table = ReceivingOrderProductsTable

  val table = TableQuery[Table]

  def findByPurchaseOrderIdsAndProductIds(
      purchaseOrderIds: Seq[UUID],
      productIds: Seq[UUID],
    ): Future[Map[UUID, Seq[Record]]] = {
    val q =
      table
        .filter(_.productId inSet productIds)
        .join(receivingOrderDao.queryFindByPurchaseOrderIds(purchaseOrderIds))
        .on(_.receivingOrderId === _.id)
        .map {
          case (receivingOrderProductTable, _) =>
            receivingOrderProductTable.productId -> receivingOrderProductTable
        }
    run(q.result).map(
      _.groupBy(_._1)
        .transform((key, v) => v.map(_._2)),
    )
  }

  def queryFindByReceivingOrderIds(receivingOrderIds: Seq[UUID]) =
    table.filter(_.receivingOrderId inSet receivingOrderIds)

  def countProductsByReceivingOrderIds(receivingOrderIds: Seq[UUID]): Future[Map[UUID, BigDecimal]] = {
    val q = queryFindByReceivingOrderIds(receivingOrderIds).groupBy(_.receivingOrderId).map {
      case (roId, rows) => roId -> rows.map(_.quantity).sum
    }

    run(q.result).map(_.toMap.transform((_, v) => v.getOrElse[BigDecimal](0)))
  }

  def findStockValueByReceivingOrderIds(receivingOrderIds: Seq[UUID]): Future[Map[UUID, BigDecimal]] = {
    val q =
      table.filter(_.receivingOrderId inSet receivingOrderIds).groupBy(r => r.receivingOrderId).map {
        case (roId, rows) =>
          val stockValue = rows.map(r => r.quantity * r.costAmount).sum
          roId -> stockValue.getOrElse(BigDecimal(0))
      }
    run(q.result).map(_.toMap)
  }

  def findByReceivingOrderId(receivingOrderId: UUID): Future[Seq[Record]] =
    run(table.filter(_.receivingOrderId === receivingOrderId).result)

  def findOneByReceivingOrderIdAndProductId(receivingOrderId: UUID, productId: UUID): Future[Option[Record]] =
    run(table.filter(_.receivingOrderId === receivingOrderId).filter(_.productId === productId).result.headOption)

  def queryBulkUpsertAndDeleteTheRestByReceivingOrderId(
      updates: Seq[ReceivingOrderProductUpdate],
      receivingOrderId: UUID,
    ) =
    for {
      us <- queryBulkUpsert(updates)
      records = us.map { case (_, record) => record }
      _ <- queryDeleteTheRestByDeleteFilter(records, _.receivingOrderId === receivingOrderId)
    } yield records

  def findAllWithFilters(merchantId: UUID, f: Filters)(offset: Int, limit: Int): Future[Seq[Record]] = {
    val q = queryFindByMerchantId(merchantId, f.receivingOrderId).drop(offset).take(limit)
    run(q.result)
  }

  def countAllWithFilters(merchantId: UUID, f: Filters): Future[Int] = {
    val q = queryFindByMerchantId(merchantId = merchantId, receivingOrderId = f.receivingOrderId)
    run(q.length.result)
  }

  def queryFindByMerchantId(merchantId: UUID, receivingOrderId: UUID) =
    table.filter(_.merchantId === merchantId).filter(_.receivingOrderId === receivingOrderId)

  def findAverageCostByProductIdsAndLocationId(
      productIds: Seq[UUID],
      locationId: Option[UUID],
    ): Future[Map[UUID, BigDecimal]] = {
    val query = table
      .filter(t =>
        all(
          Some(t.productId inSet productIds),
          Some(t.costAmount.isDefined),
          locationId.map(lId => t.receivingOrderId in receivingOrderDao.queryFindByLocationId(lId).map(_.id)),
        ),
      )
      .groupBy(_.productId)
      .map {
        case (productId, rows) => productId -> rows.map(_.costAmount).avg
      }
    run(query.result).map { result =>
      result.flatMap {
        case (productId, averageCostAmount) => averageCostAmount.map(avgCstAmnt => (productId, avgCstAmnt))
      }.toMap
    }
  }
}
