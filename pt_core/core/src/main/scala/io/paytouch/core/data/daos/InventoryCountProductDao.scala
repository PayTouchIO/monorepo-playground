package io.paytouch.core.data.daos

import java.util.UUID

import io.paytouch.core.data.daos.features.{ SlickFindAllDao, SlickMerchantDao }
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ InventoryCountProductRecord, InventoryCountProductUpdate }
import io.paytouch.core.data.tables.InventoryCountProductsTable
import io.paytouch.core.filters.InventoryCountProductFilters

import scala.concurrent._

class InventoryCountProductDao(implicit val ec: ExecutionContext, val db: Database)
    extends SlickMerchantDao
       with SlickFindAllDao {

  type Record = InventoryCountProductRecord
  type Update = InventoryCountProductUpdate
  type Filters = InventoryCountProductFilters
  type Table = InventoryCountProductsTable

  val table = TableQuery[Table]

  def findAllWithFilters(merchantId: UUID, filters: Filters)(offset: Int, limit: Int): Future[Seq[Record]] =
    run(queryFindAllByMerchantId(merchantId, filters.inventoryCountId).drop(offset).take(limit).result)

  def countAllWithFilters(merchantId: UUID, filters: Filters): Future[Int] =
    run(queryFindAllByMerchantId(merchantId, filters.inventoryCountId).length.result)

  def queryFindAllByMerchantId(merchantId: UUID, inventoryCountProductId: UUID) =
    super.queryFindAllByMerchantId(merchantId).filter(_.inventoryCountId === inventoryCountProductId)

  def queryBulkUpsertAndDeleteTheRestByInventoryCountId(updates: Seq[Update], inventoryCountId: UUID) =
    for {
      us <- queryBulkUpsert(updates)
      records = us.map { case (_, record) => record }
      _ <- queryDeleteTheRestByDeleteFilter(records, _.inventoryCountId === inventoryCountId)
    } yield records

  def queryFindByInventoryCountId(inventoryCountId: UUID) = table.filter(_.inventoryCountId === inventoryCountId)

  def findByInventoryCountId(inventoryCountId: UUID): Future[Seq[Record]] =
    run(queryFindByInventoryCountId(inventoryCountId).result)

  def findOneByInventoryCountIdAndProductId(inventoryCountId: UUID, productId: UUID): Future[Option[Record]] =
    run(table.filter(_.inventoryCountId === inventoryCountId).filter(_.productId === productId).result.headOption)

  def querySumValueChangeAmountByInventoryCountId(inventoryCountId: UUID) =
    table
      .filter(_.inventoryCountId === inventoryCountId)
      .map(_.valueChangeAmount)
      .sum
      .result

  def countOrderedProductsByInventoryCountIds(inventoryCountIds: Seq[UUID]): Future[Map[UUID, Int]] = {
    val q = table.filter(_.inventoryCountId inSet inventoryCountIds).groupBy(_.inventoryCountId).map {
      case (icId, rows) => icId -> rows.length
    }

    run(q.result).map(_.toMap)
  }
}
