package io.paytouch.core.data.daos

import java.util.UUID

import io.paytouch.core.data.daos.features.{ SlickFindAllDao, SlickMerchantDao, SlickRelDao }
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ ReturnOrderProductRecord, ReturnOrderProductUpdate }
import io.paytouch.core.data.tables.ReturnOrderProductsTable
import io.paytouch.core.filters.ReturnOrderProductFilters

import scala.concurrent._

class ReturnOrderProductDao(returnOrderDao: => ReturnOrderDao)(implicit val ec: ExecutionContext, val db: Database)
    extends SlickMerchantDao
       with SlickRelDao
       with SlickFindAllDao {

  type Record = ReturnOrderProductRecord
  type Update = ReturnOrderProductUpdate
  type Table = ReturnOrderProductsTable
  type Filters = ReturnOrderProductFilters

  val table = TableQuery[Table]

  def findByPurchaseOrderIdsAndProductIds(
      purchaseOrderIds: Seq[UUID],
      productIds: Seq[UUID],
    ): Future[Map[UUID, Seq[Record]]] = {
    val q =
      table
        .filter(_.productId inSet productIds)
        .join(returnOrderDao.queryFindByPurchaseOrderIds(purchaseOrderIds))
        .on(_.returnOrderId === _.id)
        .map {
          case (returnOrderProductTable, _) =>
            returnOrderProductTable.productId -> returnOrderProductTable
        }
    run(q.result).map(_.groupBy(_._1).transform((_, v) => v.map(_._2)))
  }

  def findAllWithFilters(merchantId: UUID, filters: Filters)(offset: Int, limit: Int): Future[Seq[Record]] =
    run(queryFindAllByMerchantId(merchantId, filters.returnOrderId).drop(offset).take(limit).result)

  def countAllWithFilters(merchantId: UUID, filters: Filters): Future[Int] =
    run(queryFindAllByMerchantId(merchantId, filters.returnOrderId).length.result)

  def queryFindAllByMerchantId(merchantId: UUID, returnOrderProductId: UUID) =
    super.queryFindAllByMerchantId(merchantId).filter(_.returnOrderId === returnOrderProductId)

  def findByReturnOrderIds(returnOrderIds: Seq[UUID]): Future[Seq[Record]] = {
    val q = queryFindByReturnOrderIds(returnOrderIds)
    run(q.result)
  }

  private def queryFindByReturnOrderIds(returnOrderIds: Seq[UUID]) =
    table.filter(_.returnOrderId inSet returnOrderIds)

  override def queryByRelIds(upsertion: Update) = {
    require(
      upsertion.productId.isDefined,
      "ReturnOrderProductDao - Impossible to find by product id and return order id without a product id",
    )
    require(
      upsertion.returnOrderId.isDefined,
      "ReturnOrderProductDao - Impossible to find by return order id and category id without a category id",
    )
    queryFindByProductIdAndReturnOrderId(upsertion.productId.get, upsertion.returnOrderId.get)
  }

  def findByProductIdAndReturnOrderId(productId: UUID, returnOrderId: UUID): Future[Option[Record]] = {
    val q = queryFindByProductIdAndReturnOrderId(productId, returnOrderId)
    run(q.result.headOption)
  }

  def queryFindByProductIdAndReturnOrderId(productId: UUID, returnOrderId: UUID) =
    table.filter(t => t.productId === productId && t.returnOrderId === returnOrderId)

  def queryBulkUpsertAndDeleteTheRestByReturnOrderId(updates: Seq[Update], returnOrderId: UUID) =
    queryBulkUpsertAndDeleteTheRestByRelIds(updates, t => t.returnOrderId === returnOrderId)

  def countProductsByReturnOrderIds(returnOrderIds: Seq[UUID]): Future[Map[UUID, BigDecimal]] = {
    val q = queryFindByReturnOrderIds(returnOrderIds).groupBy(_.returnOrderId).map {
      case (roId, rows) => roId -> rows.map(_.quantity).sum
    }

    run(q.result).map(_.toMap.transform((_, v) => v.getOrElse[BigDecimal](0)))
  }

  def findByReturnOrderId(returnOrderId: UUID): Future[Seq[Record]] =
    run(table.filter(_.returnOrderId === returnOrderId).result)
}
