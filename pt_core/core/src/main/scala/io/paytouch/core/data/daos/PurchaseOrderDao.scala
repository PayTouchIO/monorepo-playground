package io.paytouch.core.data.daos

import java.time.LocalDateTime
import java.util.UUID

import io.paytouch.core.data.daos.features._
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.driver.CustomPlainImplicits._
import io.paytouch.core.data.model.enums.{ PurchaseOrderPaymentStatus, ReceivingObjectStatus, ReceivingOrderObjectType }
import io.paytouch.core.data.model.upsertions.PurchaseOrderUpsertion
import io.paytouch.core.data.model.{ PurchaseOrderRecord, PurchaseOrderUpdate }
import io.paytouch.core.data.tables.PurchaseOrdersTable
import io.paytouch.core.entities.enums.ReceivingOrderView
import io.paytouch.core.filters.PurchaseOrderFilters
import io.paytouch.core.utils.ResultType
import io.paytouch.core.utils.UtcTime
import slick.jdbc.GetResult

import scala.concurrent._

class PurchaseOrderDao(
    val locationDao: LocationDao,
    val nextNumberDao: NextNumberDao,
    val purchaseOrderProductDao: PurchaseOrderProductDao,
    val returnOrderDao: ReturnOrderDao,
    val returnOrderProductDao: ReturnOrderProductDao,
    val receivingOrderDao: ReceivingOrderDao,
    val receivingOrderProductDao: ReceivingOrderProductDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickFindAllDao
       with SlickUpsertDao
       with SlickLocationTimeZoneHelper
       with SlickSoftDeleteDao
       with SlickReceivingObjectDao {

  type Record = PurchaseOrderRecord
  type Update = PurchaseOrderUpdate
  type Upsertion = PurchaseOrderUpsertion
  type Filters = PurchaseOrderFilters
  type Table = PurchaseOrdersTable

  val table = TableQuery[Table]

  def findAllWithFilters(merchantId: UUID, f: Filters)(offset: Int, limit: Int): Future[Seq[Record]] =
    findAllByMerchantId(merchantId, f.locationId, f.supplierId, f.status, f.from, f.to, f.query, f.view)(offset, limit)

  def countAllWithFilters(merchantId: UUID, f: Filters): Future[Int] =
    countAllByMerchantId(merchantId, f.locationId, f.supplierId, f.status, f.from, f.to, f.query, f.view)

  def findAllByMerchantId(
      merchantId: UUID,
      locationId: Option[UUID],
      supplierId: Option[UUID],
      status: Option[ReceivingObjectStatus],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
      query: Option[String],
      view: Option[ReceivingOrderView],
    )(
      offset: Int,
      limit: Int,
    ): Future[Seq[Record]] = {
    val q =
      queryFindAllByMerchantId(merchantId, locationId, supplierId, status, from, to, query, view)
        .sortBy(t => (t.createdAt.desc, t.updatedAt.desc))
        .drop(offset)
        .take(limit)
    run(q.result)
  }

  def countAllByMerchantId(
      merchantId: UUID,
      locationId: Option[UUID],
      supplierId: Option[UUID],
      status: Option[ReceivingObjectStatus],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
      query: Option[String],
      view: Option[ReceivingOrderView],
    ): Future[Int] =
    run(queryFindAllByMerchantId(merchantId, locationId, supplierId, status, from, to, query, view).length.result)

  private def queryFindAllByMerchantId(
      merchantId: UUID,
      locationId: Option[UUID],
      supplierId: Option[UUID],
      status: Option[ReceivingObjectStatus],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
      query: Option[String],
      view: Option[ReceivingOrderView],
    ) =
    queryFilterByOptionalView(view).filter(t =>
      all(
        Some(t.merchantId === merchantId),
        locationId.map(lId => t.locationId === lId),
        supplierId.map(sId => t.supplierId === sId),
        status.map(s => t.status === s),
        from.map(start => t.id in itemIdsAtOrAfterCreatedAtDate(start)),
        to.map(end => t.id in itemIdsBeforeCreatedAtDate(end)),
        query.map(q => t.number.toLowerCase like s"${q.toLowerCase}%"),
      ),
    )

  private def hasStatusCompleted(t: Table) = t.status === (ReceivingObjectStatus.Completed: ReceivingObjectStatus)

  override def queryInsert(entity: Record) =
    entity match {
      case e if e.number.isEmpty => queryInsertWithIncrementedNumber(e)
      case _                     => super.queryInsert(entity)
    }

  private def queryInsertWithIncrementedNumber(entity: Record) =
    for {
      purchaseOrderNumber <- nextNumberDao.queryNextPurchaseOrderNumberForMerchantId(entity.merchantId)
      entityWithPurchaseOrderNumber = entity.copy(number = purchaseOrderNumber.toString)
      insert <- table returning table += entityWithPurchaseOrderNumber
    } yield insert

  def upsert(upsertion: Upsertion): Future[(ResultType, Record)] = {
    val q = for {
      (resultType, purchaseOrder) <- queryUpsert(upsertion.purchaseOrder)
      purchaseOrderProducts <- asOption(
        upsertion
          .purchaseOrderProducts
          .map(purchaseOrderProductDao.queryBulkUpsertAndDeleteTheRestByPurchaseOrderId(_, purchaseOrder.id)),
      )
    } yield (resultType, purchaseOrder)

    runWithTransaction(q)
  }

  def missingQuantitiesPerProductId(purchaseOrderId: UUID): Future[Map[UUID, BigDecimal]] = {
    implicit val getMapResult = GetResult(r => (r.nextUUID(), r.nextBigDecimal()))
    run(sql"""SELECT pop.product_id, (pop.quantity - rop.quantity_sum) as missing_quantity
              FROM purchase_order_products pop
              JOIN purchase_orders po ON po.id = pop.purchase_order_id
              JOIN (
                SELECT inner_rop.product_id, SUM(quantity) as quantity_sum
                FROM receiving_orders ro, receiving_order_products inner_rop
                WHERE ro.id = inner_rop.receiving_order_id
                AND ro.receiving_object_id = '#$purchaseOrderId'
                AND ro.receiving_object_type = ${ReceivingOrderObjectType.PurchaseOrder.entryName}
                AND ro.status = 'received'
                GROUP BY(inner_rop.product_id)
              ) rop
              ON pop.product_id = rop.product_id
              WHERE pop.purchase_order_id = '#$purchaseOrderId'
              AND (pop.quantity - rop.quantity_sum) > 0
              AND po.deleted_at IS NULL
            ;""".as[(UUID, BigDecimal)]).map(
      _.toMap,
    )
  }

  def setPaymentStatus(id: UUID, status: PurchaseOrderPaymentStatus): Future[Int] = {
    val field = for { o <- table if o.id === id } yield (o.paymentStatus, o.updatedAt)
    runWithTransaction(field.update(Some(status), UtcTime.now))
  }

  def markAsSent(id: UUID): Future[(ResultType, Record)] = {
    val update = PurchaseOrderUpdate(
      id = Some(id),
      merchantId = None,
      supplierId = None,
      locationId = None,
      userId = None,
      paymentStatus = None,
      expectedDeliveryDate = None,
      sent = Some(true),
      notes = None,
      deletedAt = None,
    )
    upsert(update)
  }

  override protected def filterAvailableForReturn = {
    val rcType: Rep[Option[ReceivingOrderObjectType]] = Some(ReceivingOrderObjectType.PurchaseOrder)

    val rcQuery =
      receivingOrderDao
        .table
        .filter(_.receivingObjectType === rcType)
        .filter(_.receivingObjectId.isDefined)
        .join(receivingOrderProductDao.table)
        .on(_.id === _.receivingOrderId)
        .filter {
          case (_, rcpT) =>
            rcpT.quantity.isDefined
        }
        .groupBy {
          case (rcT, rcpT) => (rcT.receivingObjectId, rcpT.productId)
        }
        .map {
          case ((purchaseOrderId, productId), rows) =>
            (purchaseOrderId, productId) -> rows.map { case (_, rcpT) => rcpT.quantity }.sum
        }

    val rtQuery =
      returnOrderDao
        .table
        .filter(_.purchaseOrderId.isDefined)
        .join(returnOrderProductDao.table)
        .on(_.id === _.returnOrderId)
        .filter {
          case (_, rtpT) =>
            rtpT.quantity.isDefined
        }
        .groupBy {
          case (rtT, rtpT) => (rtT.purchaseOrderId, rtpT.productId)
        }
        .map {
          case ((purchaseOrderId, productId), rows) =>
            (purchaseOrderId, productId) -> rows.map { case (_, rtpT) => rtpT.quantity }.sum
        }

    val q = rcQuery
      .joinLeft(rtQuery)
      .on {
        // rc.purchaseOrderId == rt.purchaseOrderId
        case (rcQ, rtQ) =>
          rcQ._1._1 === rtQ._1._1
      }
      .filter {
        case (rcQ, maybeRtQ) =>
          // rc.quantity > rt.quantity OR rt.quantity IS NULL
          maybeRtQ.map(rtQ => rcQ._2.getOrElse(BigDecimal(0)) > rtQ._2.getOrElse(BigDecimal(0))).getOrElse(true)
      }
      .groupBy {
        case (rcQ, _) => rcQ._1._1
      }
      .map {
        case (purchaseOrderId, _) => purchaseOrderId
      }

    table.filter(_.id in q)
  }
}
