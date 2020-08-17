package io.paytouch.core.data.daos

import java.time.LocalDateTime
import java.util.UUID

import io.paytouch.core.data.daos.features.{
  SlickFindAllDao,
  SlickLocationTimeZoneHelper,
  SlickReceivingObjectDao,
  SlickUpsertDao,
}
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.driver.CustomPlainImplicits._
import io.paytouch.core.data.model.enums.{ ReceivingObjectStatus, ReceivingOrderObjectType }
import io.paytouch.core.data.model.upsertions.TransferOrderUpsertion
import io.paytouch.core.data.model.{ TransferOrderRecord, TransferOrderUpdate }
import io.paytouch.core.data.tables.TransferOrdersTable
import io.paytouch.core.entities.enums.ReceivingOrderView
import io.paytouch.core.filters.TransferOrderFilters
import io.paytouch.core.utils.ResultType
import slick.jdbc.GetResult

import scala.concurrent._

class TransferOrderDao(
    val locationDao: LocationDao,
    val nextNumberDao: NextNumberDao,
    val receivingOrderDao: ReceivingOrderDao,
    val transferOrderProductDao: TransferOrderProductDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickFindAllDao
       with SlickUpsertDao
       with SlickLocationTimeZoneHelper
       with SlickReceivingObjectDao {

  type Record = TransferOrderRecord
  type Update = TransferOrderUpdate
  type Upsertion = TransferOrderUpsertion
  type Filters = TransferOrderFilters
  type Table = TransferOrdersTable

  val table = TableQuery[Table]

  def findAllWithFilters(merchantId: UUID, f: Filters)(offset: Int, limit: Int): Future[Seq[Record]] =
    findAllByMerchantId(merchantId, f.locationId, f.status, f.from, f.to, f.query, f.view)(offset, limit)

  def findAllByMerchantId(
      merchantId: UUID,
      locationId: Option[UUID],
      status: Option[ReceivingObjectStatus],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
      query: Option[String],
      view: Option[ReceivingOrderView],
    )(
      offset: Int,
      limit: Int,
    ): Future[Seq[Record]] = {
    val q = queryFindAllByMerchantId(merchantId, locationId, status, from, to, query, view).drop(offset).take(limit)
    run(q.result)
  }

  def countAllWithFilters(merchantId: UUID, f: Filters): Future[Int] =
    countAllByMerchantId(merchantId, f.locationId, f.status, f.from, f.to, f.query, f.view)

  def countAllByMerchantId(
      merchantId: UUID,
      locationId: Option[UUID],
      status: Option[ReceivingObjectStatus],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
      query: Option[String],
      view: Option[ReceivingOrderView],
    ): Future[Int] = {
    val q = queryFindAllByMerchantId(merchantId, locationId, status, from, to, query, view).length
    run(q.result)
  }

  def queryFindAllByMerchantId(
      merchantId: UUID,
      locationId: Option[UUID],
      status: Option[ReceivingObjectStatus],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
      query: Option[String],
      view: Option[ReceivingOrderView],
    ) =
    queryFilterByOptionalView(view)
      .filter(t =>
        all(
          Some(t.merchantId === merchantId),
          locationId.map(lId => t.fromLocationId === lId),
          status.map(s => t.status === s),
          from.map(start => t.id in itemIdsAtOrAfterCreatedAtDate(start)),
          to.map(end => t.id in itemIdsBeforeCreatedAtDate(end)),
          query.map(q => t.number.toLowerCase like s"${q.toLowerCase}%"),
        ),
      )
      .sortBy(_.createdAt)

  def upsert(upsertion: Upsertion): Future[(ResultType, Record)] = {
    val q = for {
      (result, transferOrder) <- queryUpsert(upsertion.transferOrder)
      _ <- asOption(
        upsertion
          .transferOrderProducts
          .map(transferOrderProductDao.queryBulkUpsertAndDeleteTheRest(_, transferOrder.id)),
      )
    } yield (result, transferOrder)
    runWithTransaction(q)
  }

  override def queryInsert(entity: Record) =
    entity match {
      case e if e.number.isEmpty => queryInsertWithIncrementedNumber(e)
      case _                     => super.queryInsert(entity)
    }

  private def queryInsertWithIncrementedNumber(entity: Record) =
    for {
      transferOrderNumber <- nextNumberDao.queryNextTransferOrderNumberForMerchantId(entity.merchantId)
      entityWithTransferOrderNumber = entity.copy(number = transferOrderNumber.toString)
      insert <- table returning table += entityWithTransferOrderNumber
    } yield insert

  def missingQuantitiesPerProductId(transferOrderId: UUID): Future[Map[UUID, BigDecimal]] = {
    implicit val getMapResult = GetResult(r => (r.nextUUID(), r.nextBigDecimal()))
    run(sql"""SELECT top.product_id, (top.quantity - rop.quantity_sum) as missing_quantity
              FROM transfer_order_products top
              JOIN (
                SELECT inner_rop.product_id, SUM(quantity) as quantity_sum
                FROM receiving_orders ro, receiving_order_products inner_rop
                WHERE ro.id = inner_rop.receiving_order_id
                AND ro.receiving_object_id = '#$transferOrderId'
                AND ro.receiving_object_type = ${ReceivingOrderObjectType.Transfer.entryName}
                AND ro.status = 'received'
                GROUP BY(inner_rop.product_id)
              ) rop
              ON top.product_id = rop.product_id
              WHERE top.transfer_order_id = '#$transferOrderId'
              AND (top.quantity - rop.quantity_sum) > 0
            ;""".as[(UUID, BigDecimal)]).map(
      _.toMap,
    )
  }

}
