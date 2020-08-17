package io.paytouch.core.data.daos

import java.time.{ Duration, LocalDateTime }
import java.util.UUID

import scala.concurrent._

import io.paytouch._
import io.paytouch.implicits._

import io.paytouch.core.data.daos.features.SlickMerchantDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.OrderItemIdColumn
import io.paytouch.core.data.model.{ OrderItemRecord, OrderItemUpdate }
import io.paytouch.core.data.model.enums.PaymentStatus
import io.paytouch.core.data.model.upsertions.OrderItemUpsertion
import io.paytouch.core.data.tables.{ OrderItemsTable, SlickTable }
import io.paytouch.core.utils.UtcTime

class OrderItemDao(
    val orderItemDiscountDao: OrderItemDiscountDao,
    val orderItemModifierOptionDao: OrderItemModifierOptionDao,
    val orderItemTaxRateDao: OrderItemTaxRateDao,
    val orderItemVariantOptionDao: OrderItemVariantOptionDao,
    val orderDao: OrderDao,
    val orderBundleDao: OrderBundleDao,
    val productDao: ProductDao,
    val ticketOrderItemDao: TicketOrderItemDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickMerchantDao {
  type Record = OrderItemRecord
  type Update = OrderItemUpdate
  type Table = OrderItemsTable

  val table = TableQuery[Table]

  def findByOrderId(orderId: UUID): Future[Seq[Record]] =
    findByOrderIds(Seq(orderId))

  def findByOrderIds(orderIds: Seq[UUID]): Future[Seq[Record]] =
    if (orderIds.isEmpty)
      Future.successful(Seq.empty)
    else
      queryFindByOrderIds(orderIds)
        .pipe(run)

  def findByTicketIds(ticketIds: Seq[UUID]): Future[Map[UUID, Seq[Record]]] =
    table
      .join(ticketOrderItemDao.queryFindByTicketIds(ticketIds))
      .on(_.id === _.orderItemId)
      .map {
        case (orderItemsT, ticketOrderItemsT) => ticketOrderItemsT.ticketId -> orderItemsT
      }
      .result
      .pipe(run)
      .map { result =>
        result
          .groupBy { case (ticketId, _) => ticketId }
          .transform((_, v) => v.map { case (_, records) => records })
      }

  def findBundleItemsByOrderIds(orderIds: Seq[UUID]): Future[Map[UUID, Seq[Record]]] =
    table
      .join(orderBundleDao.queryFindByOrderIds(orderIds))
      .on(_.id === _.bundleOrderItemId)
      .map {
        case (orderItemsT, _) => orderItemsT.orderId -> orderItemsT
      }
      .result
      .pipe(run)
      .map { result =>
        result
          .groupBy { case (orderId, _) => orderId }
          .transform((_, v) => v.map { case (_, records) => records })
      }

  def queryFindByOrderIds(orderIds: Seq[UUID]) =
    table.filter(_.orderId inSet orderIds).result

  def queryBulkUpsertAndDeleteTheRestByOrderId(upsertions: Seq[OrderItemUpsertion], orderId: UUID) =
    for {
      us <- queryBulkUpsertion(upsertions)
      records = us.map { case (_, record) => record }
      _ <- queryDeleteTheRestByDeleteFilter(records, _.orderId === orderId)
    } yield records

  def queryBulkUpsertion(upsertions: Seq[OrderItemUpsertion]) =
    asSeq(upsertions.map(queryUpsertion))

  def queryUpsertion(upsertion: OrderItemUpsertion) =
    for {
      (resultType, orderItem) <- queryUpsert(upsertion.orderItem)
      filterByOrderItemId = { t: SlickTable[_] with OrderItemIdColumn => t.orderItemId === orderItem.id }
      discounts <- asOption(
        upsertion
          .discounts
          .map(discounts =>
            orderItemDiscountDao
              .queryBulkUpsertAndDeleteTheRest(discounts, filterByOrderItemId),
          ),
      )
      modifierOptions <- asOption(
        upsertion
          .modifierOptions
          .map(modifierOptions =>
            orderItemModifierOptionDao
              .queryBulkUpsertAndDeleteTheRestByRelIds(modifierOptions, filterByOrderItemId),
          ),
      )
      variantOptions <- asOption(
        upsertion
          .variantOptions
          .map(variantOptions =>
            orderItemVariantOptionDao
              .queryBulkUpsertAndDeleteTheRestByRelIds(variantOptions, filterByOrderItemId),
          ),
      )
      orderItemTaxRates <- asOption(
        upsertion
          .taxRates
          .map(taxRates =>
            orderItemTaxRateDao
              .queryBulkUpsertAndDeleteTheRestByRelIds(taxRates, filterByOrderItemId),
          ),
      )
    } yield (resultType, orderItem)

  def findAllByProductIds(
      merchantId: UUID,
      productIds: Seq[UUID],
      locationIds: Seq[UUID],
    ): Future[Seq[Record]] =
    if (productIds.isEmpty || locationIds.isEmpty)
      Future.successful(Seq.empty)
    else
      queryFindAllByProductIds(merchantId, productIds, locationIds)
        .result
        .pipe(run)

  private def queryFindAllByProductIds(
      merchantId: UUID,
      productIds: Seq[UUID],
      locationIds: Seq[UUID],
    ) =
    table
      .filter(_.productId inSet productIds)
      .join(
        orderDao.queryFindAllByMerchantId(
          merchantId = merchantId,
          locationIds = locationIds,
          customerId = None,
          tableId = None,
          paymentType = None,
          view = None,
          from = None,
          to = None,
          query = None,
          isInvoice = None,
          orderStatus = None,
          acceptanceStatus = None,
          paymentStatus = None,
          sourcesOrDeliveryProviders = None,
          updatedSince = None,
        ),
      )
      .on(_.orderId === _.id)
      .map { case (orderItems, _) => orderItems }

  def findAllPaidByProductIds(
      merchantId: UUID,
      productIds: Seq[UUID],
      locationIds: Seq[UUID],
    ): Future[Seq[Record]] =
    if (productIds.isEmpty || locationIds.isEmpty)
      Future.successful(Seq.empty)
    else
      queryFindAllByProductIds(merchantId, productIds, locationIds)
        .filter(_.paymentStatus inSet PaymentStatus.isPaid)
        .result
        .pipe(run)

  def findByMerchantIdAndProductIdPerLocation(
      merchantId: UUID,
      productId: UUID,
      locationIds: Seq[UUID],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
    ): Future[Map[UUID, Seq[Record]]] =
    if (locationIds.isEmpty)
      Future.successful(Map.empty)
    else
      table
        .filter(_.merchantId === merchantId)
        .filter(_.paymentStatus inSet PaymentStatus.isPaid)
        .filter(_.productId in productDao.queryFindByIdsWithVariantsIncludedDeleted(productId).map(_.id))
        .join(
          orderDao.queryFindAllByMerchantId(
            merchantId = merchantId,
            locationIds = locationIds,
            customerId = None,
            tableId = None,
            paymentType = None,
            view = None,
            from = from,
            to = to,
            query = None,
            isInvoice = None,
            orderStatus = None,
            acceptanceStatus = None,
            paymentStatus = None,
            sourcesOrDeliveryProviders = None,
            updatedSince = None,
          ),
        )
        .on(_.orderId === _.id)
        .map { case (orderItems, orders) => (orders.locationId.get, orderItems) }
        .result
        .pipe(run)
        .map(_.groupBy { case (locationId, _) => locationId }.transform { (_, v) =>
          v.map {
            case (_, record) => record
          }
        })

  def queryFindTopPopularProductIdsWithCount(
      merchantId: UUID,
      locationIds: Seq[UUID],
    )(
      duration: Duration,
      offset: Int,
      limit: Int,
    ) = {
    val from = UtcTime.now.minus(duration).toLocalDateTime
    table
      .join(productDao.table)
      .on(_.productId === _.id)
      .filter { case (orderItemsT, _) => orderItemsT.merchantId === merchantId }
      .filter {
        case (orderItemsT, _) =>
          orderItemsT.orderId in orderDao
            .queryFindAllByMerchantId(
              merchantId = merchantId,
              locationIds = locationIds,
              customerId = None,
              tableId = None,
              paymentType = None,
              view = None,
              from = Some(from),
              to = None,
              query = None,
              isInvoice = None,
              orderStatus = None,
              acceptanceStatus = None,
              paymentStatus = None,
              sourcesOrDeliveryProviders = None,
              updatedSince = None,
            )
            .map(_.id)
      }
      .filter { case (_, productsT) => productsT.isVariantOfProductId.isDefined }
      .groupBy { case (_, productsT) => productsT.isVariantOfProductId }
      .map { case (pId, rows) => pId -> rows.size }
      .sortBy { case (_, count) => count.desc }
      .drop(offset)
      .take(limit)
      .result
  }
}
