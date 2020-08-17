package io.paytouch.core.data.daos

import java.util.UUID

import io.paytouch.core.data.daos.features.{ SlickFindAllDao, SlickRelDao }
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ TransferOrderProductRecord, TransferOrderProductUpdate }
import io.paytouch.core.data.tables.TransferOrderProductsTable
import io.paytouch.core.filters.TransferOrderProductFilters

import scala.concurrent._

class TransferOrderProductDao(implicit val ec: ExecutionContext, val db: Database)
    extends SlickRelDao
       with SlickFindAllDao {

  type Record = TransferOrderProductRecord
  type Update = TransferOrderProductUpdate
  type Table = TransferOrderProductsTable
  type Filters = TransferOrderProductFilters

  val table = TableQuery[Table]

  def findAllWithFilters(merchantId: UUID, filters: TransferOrderProductFilters)(offset: Int, limit: Int) =
    run(queryFindByTransferOrderId(filters.transferOrderId).drop(offset).take(limit).result)

  def countAllWithFilters(merchantId: UUID, filters: TransferOrderProductFilters) =
    run(queryFindByTransferOrderId(filters.transferOrderId).length.result)

  def countProductsByTransferOrderIds(transferOrderIds: Seq[UUID]): Future[Map[UUID, BigDecimal]] = {
    val q = queryFindByTransferOrderIds(transferOrderIds).groupBy(_.transferOrderId).map {
      case (roId, rows) => roId -> rows.map(_.quantity).sum
    }

    run(q.result).map(_.toMap.transform((_, v) => v.getOrElse[BigDecimal](0)))
  }

  def findByTransferOrderId(transferOrderId: UUID): Future[Seq[Record]] =
    findByTransferOrderIds(Seq(transferOrderId))

  def findByTransferOrderIds(transferOrderIds: Seq[UUID]): Future[Seq[Record]] =
    run(queryFindByTransferOrderIds(transferOrderIds).result)

  def queryFindByTransferOrderId(transferOrderId: UUID) =
    queryFindByTransferOrderIds(Seq(transferOrderId))

  def queryFindByTransferOrderIds(transferOrderIds: Seq[UUID]) =
    table.filter(_.transferOrderId inSet transferOrderIds)

  def queryByRelIds(upsertion: Update) = {
    require(
      upsertion.transferOrderId.isDefined,
      "TransferOrderProductDao - Impossible to find by transfer order id and product id without a transfer order id",
    )
    require(
      upsertion.productId.isDefined,
      "TransferOrderProductDao - Impossible to find by transfer order id and product id without a product id",
    )
    queryFindByTransferOrderIdAndProductId(upsertion.transferOrderId.get, upsertion.productId.get)
  }

  private def queryFindByTransferOrderIdAndProductId(transferOrderId: UUID, productId: UUID) =
    table.filter(_.transferOrderId === transferOrderId).filter(_.productId === productId)

  def findOneByTransferOrderIdAndProductId(transferOrderId: UUID, productId: UUID): Future[Option[Record]] =
    run(queryFindByTransferOrderIdAndProductId(transferOrderId, productId).result.headOption)

  def queryBulkUpsertAndDeleteTheRest(updates: Seq[Update], transferOrderId: UUID) =
    queryBulkUpsertAndDeleteTheRestByRelIds(updates, t => t.transferOrderId === transferOrderId)
}
