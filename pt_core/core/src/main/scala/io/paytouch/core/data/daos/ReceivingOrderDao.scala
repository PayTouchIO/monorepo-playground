package io.paytouch.core.data.daos

import java.time.LocalDateTime
import java.util.UUID

import io.paytouch.core.data.daos.features.{
  SlickFindAllDao,
  SlickLocationTimeZoneHelper,
  SlickMerchantDao,
  SlickUpsertDao,
}
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.enums.{ ReceivingOrderObjectType, ReceivingOrderStatus }
import io.paytouch.core.data.model.upsertions.ReceivingOrderUpsertion
import io.paytouch.core.data.model.{ ReceivingOrderRecord, ReceivingOrderUpdate }
import io.paytouch.core.data.tables.ReceivingOrdersTable
import io.paytouch.core.filters.ReceivingOrderFilters
import io.paytouch.core.utils.ResultType

import scala.concurrent._

class ReceivingOrderDao(
    val locationDao: LocationDao,
    val nextNumberDao: NextNumberDao,
    val receivingOrderProductDao: ReceivingOrderProductDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickMerchantDao
       with SlickFindAllDao
       with SlickUpsertDao
       with SlickLocationTimeZoneHelper {

  type Record = ReceivingOrderRecord
  type Update = ReceivingOrderUpdate
  type Upsertion = ReceivingOrderUpsertion
  type Filters = ReceivingOrderFilters
  type Table = ReceivingOrdersTable

  val table = TableQuery[Table]

  def findAllWithFilters(merchantId: UUID, f: Filters)(offset: Int, limit: Int): Future[Seq[Record]] =
    findAllByMerchantId(merchantId, f.locationId, f.from, f.to, f.query, f.status)(offset, limit)

  def countAllWithFilters(merchantId: UUID, f: Filters): Future[Int] =
    countAllByMerchantId(merchantId, f.locationId, f.from, f.to, f.query, f.status)

  def findAllByMerchantId(
      merchantId: UUID,
      locationId: Option[UUID],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
      query: Option[String],
      status: Option[ReceivingOrderStatus],
    )(
      offset: Int,
      limit: Int,
    ): Future[Seq[Record]] = {
    val q =
      queryFindAllByMerchantId(merchantId, locationId, from, to, query, status)
        .sortBy(_.createdAt)
        .drop(offset)
        .take(limit)
    run(q.result)
  }

  def countAllByMerchantId(
      merchantId: UUID,
      locationId: Option[UUID],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
      query: Option[String],
      status: Option[ReceivingOrderStatus],
    ): Future[Int] =
    run(queryFindAllByMerchantId(merchantId, locationId, from, to, query, status).length.result)

  private def queryFindAllByMerchantId(
      merchantId: UUID,
      locationId: Option[UUID],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
      query: Option[String],
      status: Option[ReceivingOrderStatus],
    ) =
    table.filter(t =>
      all(
        Some(t.merchantId === merchantId),
        locationId.map(lId => t.locationId === lId),
        from.map(start => t.id in itemIdsAtOrAfterCreatedAtDate(start)),
        to.map(end => t.id in itemIdsBeforeCreatedAtDate(end)),
        query.map(q => t.number.toLowerCase like s"${q.toLowerCase}%"),
        status.map(st => t.status === st),
      ),
    )

  def queryFindByPurchaseOrderIds(purchaseOrderIds: Seq[UUID]) =
    queryFindByReceivingObjTypeAndIds(ReceivingOrderObjectType.PurchaseOrder, purchaseOrderIds)

  def findByReceivingObjTypeAndIds(
      receivingObjType: ReceivingOrderObjectType,
      receivingObjIds: Seq[UUID],
    ): Future[Map[UUID, Seq[Record]]] = {
    val q = queryFindByReceivingObjTypeAndIds(receivingObjType, receivingObjIds)
    run(q.result).map { records =>
      records.groupBy(_.receivingObjectId).view.filterKeys(_.nonEmpty).map { case (k, v) => k.get -> v }.toMap
    }
  }

  private def queryFindByReceivingObjTypeAndIds(
      receivingObjType: ReceivingOrderObjectType,
      receivingObjIds: Seq[UUID],
    ) =
    table.filter(_.receivingObjectType === receivingObjType).filter(_.receivingObjectId inSet receivingObjIds)

  def countReceivedProductsByPurchaseOrderIds(purchaseOrderIds: Seq[UUID]): Future[Map[UUID, BigDecimal]] =
    run(queryCountReceivedOrderedProductsByPurchaseOrderIds(purchaseOrderIds).result).map(_.toMap)

  def queryCountReceivedOrderedProductsByPurchaseOrderIds(purchaseOrderIds: Seq[UUID]) =
    queryFindByReceivingObjTypeAndIds(ReceivingOrderObjectType.PurchaseOrder, purchaseOrderIds)
      .join(receivingOrderProductDao.table)
      .on(_.id === _.receivingOrderId)
      .filter {
        case (receivingOrdersT, receivingOrderProductsT) =>
          receivingOrdersT.receivingObjectId.isDefined && receivingOrderProductsT.quantity.isDefined
      }
      .groupBy { case (receivingOrdersT, _) => receivingOrdersT.receivingObjectId }
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
      receivingOrderNumber <- nextNumberDao.queryNextReceivingOrderNumberForMerchantId(entity.merchantId)
      entityWithReceivingOrderNumber = entity.copy(number = receivingOrderNumber.toString)
      insert <- table returning table += entityWithReceivingOrderNumber
    } yield insert

  def upsert(upsertion: Upsertion): Future[(ResultType, Record)] = {
    val q = for {
      (resultType, receivingOrder) <- queryUpsert(upsertion.receivingOrder)
      receivingOrderProducts <- asOption(
        upsertion
          .receivingOrderProducts
          .map(receivingOrderProductDao.queryBulkUpsertAndDeleteTheRestByReceivingOrderId(_, receivingOrder.id)),
      )
    } yield (resultType, receivingOrder)

    runWithTransaction(q)
  }

  def requestedQuantitiesByPurchaseOrderId(purchaseOrderId: UUID) =
    queryFindByPurchaseOrderIds(Seq(purchaseOrderId))
      .join(receivingOrderProductDao.table)
      .on(_.id === _.receivingOrderId)
      .groupBy { case (_, receivingOrderProductsT) => receivingOrderProductsT.productId }
      .map {
        case (productId, rows) => productId -> rows.map { case (_, ropt) => ropt.quantity }.sum
      }

  def markAsReceivedAndSynced(id: UUID): Future[(ResultType, Record)] = {
    val update = ReceivingOrderUpdate(
      id = Some(id),
      merchantId = None,
      locationId = None,
      userId = None,
      receivingObjectType = None,
      receivingObjectId = None,
      status = Some(ReceivingOrderStatus.Received),
      synced = Some(true),
      invoiceNumber = None,
      paymentMethod = None,
      paymentStatus = None,
      paymentDueDate = None,
    )
    upsert(update)
  }

  def queryFindByObjectType(objectType: ReceivingOrderObjectType) = {
    val `type`: Rep[Option[ReceivingOrderObjectType]] = Some(objectType)
    table.filter(_.receivingObjectType === `type`).filter(_.receivingObjectId.isDefined)
  }

  def queryFindByLocationId(locationId: UUID) = table.filter(_.locationId === locationId)

  def findBySyncedAndMerchantId(synced: Boolean, merchantId: UUID): Future[Seq[Record]] =
    run(table.filter(t => t.synced === synced && t.merchantId === merchantId).result)
}
