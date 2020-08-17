package io.paytouch.core.data.daos

import java.util.UUID

import io.paytouch.core.data.daos.features.{ SlickFindAllDao, SlickMerchantDao }
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ PurchaseOrderProductRecord, PurchaseOrderProductUpdate }
import io.paytouch.core.data.tables.PurchaseOrderProductsTable
import io.paytouch.core.filters.PurchaseOrderProductFilters

import scala.concurrent._

class PurchaseOrderProductDao(implicit val ec: ExecutionContext, val db: Database)
    extends SlickMerchantDao
       with SlickFindAllDao {

  type Record = PurchaseOrderProductRecord
  type Update = PurchaseOrderProductUpdate
  type Filters = PurchaseOrderProductFilters
  type Table = PurchaseOrderProductsTable

  val table = TableQuery[Table]

  def findAllWithFilters(merchantId: UUID, filters: Filters)(offset: Int, limit: Int): Future[Seq[Record]] =
    run(queryFindAllByMerchantId(merchantId, filters.purchaseOrderId).drop(offset).take(limit).result)

  def countAllWithFilters(merchantId: UUID, filters: Filters): Future[Int] =
    run(queryFindAllByMerchantId(merchantId, filters.purchaseOrderId).length.result)

  def queryFindAllByMerchantId(merchantId: UUID, purchaseOrderProductId: UUID) =
    super.queryFindAllByMerchantId(merchantId).filter(_.purchaseOrderId === purchaseOrderProductId)

  def countOrderedProductsByPurchaseOrderIds(purchaseOrderIds: Seq[UUID]): Future[Map[UUID, BigDecimal]] = {
    val q = table.filter(_.purchaseOrderId inSet purchaseOrderIds).groupBy(_.purchaseOrderId).map {
      case (poId, rows) => poId -> rows.map(_.quantity).sum
    }

    run(q.result).map(_.toMap.transform((_, v) => v.getOrElse[BigDecimal](0)))
  }

  def queryBulkUpsertAndDeleteTheRestByPurchaseOrderId(updates: Seq[Update], purchaseOrderId: UUID) =
    for {
      us <- queryBulkUpsert(updates)
      records = us.map { case (_, record) => record }
      _ <- queryDeleteTheRestByDeleteFilter(records, _.purchaseOrderId === purchaseOrderId)
    } yield records

  def queryFindByPurchaseOrderId(purchaseOrderId: UUID) = table.filter(_.purchaseOrderId === purchaseOrderId)

  def findByPurchaseOrderId(purchaseOrderId: UUID): Future[Seq[Record]] =
    run(queryFindByPurchaseOrderId(purchaseOrderId).result)

  def findOneByPurchaseOrderIdAndProductId(purchaseOrderId: UUID, productId: UUID): Future[Option[Record]] =
    run(table.filter(_.purchaseOrderId === purchaseOrderId).filter(_.productId === productId).result.headOption)

}
