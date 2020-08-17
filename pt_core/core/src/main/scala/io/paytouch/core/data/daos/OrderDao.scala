package io.paytouch.core.data.daos

import java.time.{ LocalDateTime, ZonedDateTime }
import java.util.UUID

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.core.data.daos.features.{ SlickFindAllDao, SlickLocationOptTimeZoneHelper, SlickMerchantDao }
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ OrderRecord, OrderUpdate }
import io.paytouch.core.data.model.enums._
import io.paytouch.core.data.model.upsertions.OrderUpsertion
import io.paytouch.core.data.queries.OrdersTableQuery
import io.paytouch.core.data.tables.OrdersTable
import io.paytouch.core.entities.enums.View
import io.paytouch.core.filters.OrderFilters
import io.paytouch.core.utils.ResultType
import io.paytouch.core.utils.UtcTime

class OrderDao(
    val customerLocationDao: CustomerLocationDao,
    val customerMerchantDao: CustomerMerchantDao,
    val giftCardPassDao: GiftCardPassDao,
    val locationDao: LocationDao,
    val nextNumberDao: NextNumberDao,
    val onlineOrderAttributeDao: OnlineOrderAttributeDao,
    val orderBundleDao: OrderBundleDao,
    val orderDeliveryAddressDao: OrderDeliveryAddressDao,
    val orderDiscountDao: OrderDiscountDao,
    orderItemDao: => OrderItemDao,
    val orderTaxRateDao: OrderTaxRateDao,
    val orderUserDao: OrderUserDao,
    val paymentTransactionDao: PaymentTransactionDao,
    val paymentTransactionFeeDao: PaymentTransactionFeeDao,
    val paymentTransactionOrderItemDao: PaymentTransactionOrderItemDao,
    val rewardRedemptionDao: RewardRedemptionDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickMerchantDao
       with SlickFindAllDao
       with SlickLocationOptTimeZoneHelper {
  type Record = OrderRecord
  type Update = OrderUpdate
  type Filters = OrderFilters
  type Table = OrdersTable

  val table = TableQuery[Table]

  final override val baseQuery =
    super
      .baseQuery
      .joinLeft(onlineOrderAttributeDao.baseQuery)
      .on(_.onlineOrderAttributeId === _.id)
      .filter {
        case (_, attribute) =>
          attribute.isEmpty || attribute.map(_.acceptanceStatus) =!= (AcceptanceStatus.Open: AcceptanceStatus)
      }
      .map(_._1)

  implicit val cmDao = customerMerchantDao
  implicit val lDao = locationDao
  implicit val ooaDao = onlineOrderAttributeDao
  implicit val ptDao = paymentTransactionDao

  def findAllWithFilters(merchantId: UUID, f: Filters)(offset: Int, limit: Int): Future[Seq[Record]] =
    findAllByMerchantId(
      merchantId,
      f.locationIds,
      f.customerId,
      f.tableId,
      f.paymentType,
      f.view,
      f.from,
      f.to,
      f.query,
      f.isInvoice,
      f.orderStatus,
      f.acceptanceStatus,
      f.paymentStatus,
      f.sourcesOrDeliveryProviders,
      f.updatedSince,
    )(offset, limit)

  def countAllWithFilters(merchantId: UUID, f: Filters): Future[Int] =
    countAllByMerchantId(
      merchantId,
      f.locationIds,
      f.customerId,
      f.tableId,
      f.paymentType,
      f.view,
      f.from,
      f.to,
      f.query,
      f.isInvoice,
      f.orderStatus,
      f.acceptanceStatus,
      f.paymentStatus,
      f.sourcesOrDeliveryProviders,
      f.updatedSince,
    )

  def findAllByMerchantId(
      merchantId: UUID,
      locationIds: Seq[UUID],
      customerId: Option[UUID],
      tableId: Option[UUID],
      paymentType: Option[OrderPaymentType],
      view: Option[View],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
      query: Option[String],
      isInvoice: Option[Boolean],
      orderStatus: Option[Seq[OrderStatus]],
      acceptanceStatus: Option[AcceptanceStatus],
      paymentStatus: Option[PaymentStatus],
      sourcesOrDeliveryProviders: Option[Either[Seq[Source], Seq[DeliveryProvider]]],
      updatedSince: Option[ZonedDateTime],
    )(
      offset: Int,
      limit: Int,
    ): Future[Seq[Record]] =
    queryFindAllByMerchantId(
      merchantId,
      locationIds,
      customerId,
      tableId,
      paymentType,
      view,
      from,
      to,
      query,
      isInvoice,
      orderStatus,
      acceptanceStatus,
      paymentStatus,
      sourcesOrDeliveryProviders,
      updatedSince,
    ).sortBy(_.receivedAt.desc).drop(offset).take(limit).result.pipe(run)

  def countAllByMerchantId(
      merchantId: UUID,
      locationIds: Seq[UUID],
      customerId: Option[UUID],
      tableId: Option[UUID],
      paymentType: Option[OrderPaymentType],
      view: Option[View],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
      query: Option[String],
      isInvoice: Option[Boolean],
      orderStatus: Option[Seq[OrderStatus]],
      acceptanceStatus: Option[AcceptanceStatus],
      paymentStatus: Option[PaymentStatus],
      sourcesOrDeliveryProviders: Option[Either[Seq[Source], Seq[DeliveryProvider]]],
      updatedSince: Option[ZonedDateTime],
    ): Future[Int] = {
    val q =
      queryFindAllByMerchantId(
        merchantId,
        locationIds,
        customerId,
        tableId,
        paymentType,
        view,
        from,
        to,
        query,
        isInvoice,
        orderStatus,
        acceptanceStatus,
        paymentStatus,
        sourcesOrDeliveryProviders,
        updatedSince,
      ).length.result
    run(q)
  }

  def queryFindAllByMerchantId(
      merchantId: UUID,
      locationIds: Seq[UUID],
      customerId: Option[UUID],
      tableId: Option[UUID],
      paymentType: Option[OrderPaymentType],
      view: Option[View],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
      query: Option[String],
      isInvoice: Option[Boolean],
      orderStatus: Option[Seq[OrderStatus]],
      acceptanceStatus: Option[AcceptanceStatus],
      paymentStatus: Option[PaymentStatus],
      sourcesOrDeliveryProviders: Option[Either[Seq[Source], Seq[DeliveryProvider]]],
      updatedSince: Option[ZonedDateTime],
    ) =
    baseQuery
      .filterByMerchantId(merchantId)
      .filterByLocationIds(locationIds)
      .filterByOptCustomerId(customerId)
      .filterByOptTableId(tableId)
      .filterByOptPaymentType(paymentType)
      .filterByOptView(view)
      .filterByOptQuery(query)
      .filterByOptIsInvoice(isInvoice)
      .filterByOptOrderStatus(orderStatus)
      .filterByOptAcceptanceStatus(acceptanceStatus)
      .filterByOptPaymentStatus(paymentStatus)
      .filterByOptEitherSourceOrDeliveryProvider(sourcesOrDeliveryProviders)
      .filterByOptUpdatedSince(updatedSince)
      .filter(t =>
        all(
          from.map(start => t.receivedAtTz.asColumnOf[LocalDateTime] >= start),
          to.map(end => t.receivedAtTz.asColumnOf[LocalDateTime] < end),
        ),
      )

  def queryFindOrderByMerchantIdAndNumber(merchantId: UUID, q: String) =
    baseQuery
      .filterByMerchantId(merchantId)
      .filterByNumber(q)

  def sumTotalAmount(merchantId: UUID, filters: Filters) =
    sumAmount(merchantId, filters)(_.totalAmount)

  def sumSubtotalAmount(merchantId: UUID, filters: Filters): Future[BigDecimal] =
    sumAmount(merchantId, filters)(_.subtotalAmount)

  private def sumAmount(
      merchantId: UUID,
      filters: Filters,
    )(
      f: OrdersTable => Rep[Option[BigDecimal]],
    ): Future[BigDecimal] =
    if (filters.locationIds.isEmpty)
      Future.successful(0)
    else
      queryFindAllByMerchantId(
        merchantId,
        filters.locationIds,
        filters.customerId,
        filters.tableId,
        filters.paymentType,
        Some(View.Completed),
        filters.from,
        filters.to,
        filters.query,
        filters.isInvoice,
        filters.orderStatus,
        filters.acceptanceStatus,
        filters.paymentStatus,
        filters.sourcesOrDeliveryProviders,
        filters.updatedSince,
      ).map(f).sum.getOrElse(BigDecimal(0)).result.pipe(run)

  def countByOrderType(merchantId: UUID, filters: Filters): Future[Map[OrderType, Int]] =
    if (filters.locationIds.isEmpty)
      Future.successful(Map.empty)
    else
      queryFindAllByMerchantId(
        merchantId,
        filters.locationIds,
        filters.customerId,
        filters.tableId,
        filters.paymentType,
        filters.view,
        filters.from,
        filters.to,
        filters.query,
        filters.isInvoice,
        filters.orderStatus,
        filters.acceptanceStatus,
        filters.paymentStatus,
        filters.sourcesOrDeliveryProviders,
        filters.updatedSince,
      ).groupBy(_.`type`)
        .map {
          case (k, rows) => (k, rows.length)
        }
        .result
        .pipe(run)
        .map { ordersCount =>
          ordersCount.flatMap {
            case (orderTypes, count) =>
              orderTypes.map(_ -> count)
          }.toMap
        }

  final override def queryUpsert(entityUpdate: Update) =
    queryUpsertByQuery(
      upsertion = entityUpdate,
      query = queryOpenByIds(Seq(entityUpdate.id.getOrElse(UUID.randomUUID))),
    )

  def upsert(upsertion: OrderUpsertion): Future[(ResultType, Record)] =
    (for {
      _ <- asOption(upsertion.deliveryAddress.map(orderDeliveryAddressDao.queryUpsert))
      _ <- asOption(upsertion.onlineOrderAttribute.map(onlineOrderAttributeDao.queryUpsert))
      (resultType, order) <- queryUpsert(upsertion.order)
      _ <- asOption(upsertion.customerLocation.map(customerLocationDao.queryUpsertByCustomerIdAndLocationId))
      _ <- {
        if (upsertion.canDeleteOrderItems)
          orderItemDao.queryBulkUpsertAndDeleteTheRestByOrderId(upsertion.orderItems, order.id)
        else
          orderItemDao.queryBulkUpsertion(upsertion.orderItems)
      }
      _ <- paymentTransactionDao.queryBulkUpsert(upsertion.paymentTransactions)
      _ <- paymentTransactionFeeDao.queryBulkUpsert(upsertion.paymentTransactionFees)
      _ <- paymentTransactionOrderItemDao.queryBulkUpsertByRelIds(upsertion.paymentTransactionOrderItems)
      _ <- asOption(upsertion.orderTaxRates.map(orderTaxRateDao.queryBulkUpsertByRelIds))
      _ <- asOption(
        upsertion
          .assignedOrderUsers
          .map { orderUsers =>
            orderUserDao.queryBulkUpsertAndDeleteTheRestByRelIds(orderUsers, _.orderId === order.id)
          },
      )
      _ <- asOption(upsertion.creatorOrderUsers.map(ou => orderUserDao.queryBulkUpsertByRelIds(Seq(ou))))
      _ <- asOption(
        upsertion.orderDiscounts.map(od => orderDiscountDao.queryBulkUpsertAndDeleteTheRestByOrderId(od, order.id)),
      )
      _ <- asOption(upsertion.giftCardPasses.map(giftCardPassDao.queryBulkUpsert))
      _ <- asOption(upsertion.rewardRedemptions.map(rewardRedemptionDao.queryBulkUpsert))
      _ <- asOption(
        upsertion.orderBundles.map(ob => orderBundleDao.queryBulkUpsertAndDeleteTheRestByOrderId(ob, order.id)),
      )
    } yield resultType -> order).pipe(runWithTransaction)

  override def queryInsert(entity: Record) =
    entity match {
      case e if e.locationId.isDefined && e.number.isEmpty =>
        queryInsertWithIncrementedNumber(e, e.locationId.get)

      case _ =>
        super.queryInsert(entity)
    }

  def queryInsertWithIncrementedNumber(entity: Record, locationId: UUID) =
    for {
      orderNumber <- nextNumberDao.queryNextOrderNumberForLocationId(locationId)
      entityWithOrderNumber = entity.copy(number = Some(orderNumber.toString))
      insert <- table returning table += entityWithOrderNumber
    } yield insert

  def getAvgTipsPerCustomer(customerIds: Seq[UUID], locationIds: Seq[UUID]): Future[Map[UUID, BigDecimal]] =
    if (customerIds.isEmpty || locationIds.isEmpty)
      Future.successful(Map.empty)
    else
      baseQuery
        .filterByLocationIds(locationIds)
        .filter(_.customerId inSet customerIds)
        .groupBy(r => r.customerId)
        .map {
          case (customerId, rows) =>
            val avgTipAmount = rows.map(_.tipAmount).avg.getOrElse(BigDecimal(0))
            (customerId, avgTipAmount)
        }
        .result
        .pipe(run)
        .map(result => result.toMap.view.filterKeys(_.isDefined).map { case (k, v) => k.get -> v }.toMap)

  def queryFindByLocationIds(locationIds: Seq[UUID]) =
    baseQuery.filterByLocationIds(locationIds)

  def queryFindByLocationId(locationId: UUID) =
    queryFindByLocationIds(Seq(locationId))

  def getTotalTipsPerUser(
      userIds: Seq[UUID],
      locationIds: Seq[UUID],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
    ): Future[Map[UUID, BigDecimal]] =
    if (userIds.isEmpty || locationIds.isEmpty)
      Future.successful(Map.empty)
    else
      baseQuery
        .filterCompletedOrders()
        .filterByLocationIds(locationIds)
        .filter(t =>
          all(
            from.map(start => t.completedAtTz.asColumnOf[LocalDateTime] >= start),
            to.map(end => t.completedAtTz.asColumnOf[LocalDateTime] < end),
          ),
        )
        .filter(_.userId inSet userIds)
        .groupBy(r => r.userId)
        .map {
          case (userId, rows) =>
            val tipTotalAmount = rows.map(_.tipAmount).sum.getOrElse(BigDecimal(0))
            (userId, tipTotalAmount)
        }
        .result
        .pipe(run)
        .map(result => result.toMap.view.filterKeys(_.isDefined).map { case (k, v) => k.get -> v }.toMap)

  def updateOrderNumber(numberPerId: Map[UUID, String]): Future[Boolean] =
    numberPerId
      .toSeq
      .map(Function.tupled(queryUpdateOrderNumber))
      .pipe(asSeq)
      .pipe(runWithTransaction)
      .map(_.forall(_ > 0))

  def findByDeliveryProviderId(deliveryProvider: DeliveryProvider, deliveryProviderId: String): Future[Option[Record]] =
    table
      .filter(_.deliveryProvider === deliveryProvider)
      .filter(_.deliveryProviderId === deliveryProviderId)
      .take(1)
      .result
      .headOption
      .pipe(run)

  private def queryUpdateOrderNumber(id: UUID, number: String) =
    table
      .withFilter(_.id === id)
      .map(o => o.number -> o.updatedAt)
      .update(Some(number), UtcTime.now)

  def findOpenById(id: UUID): Future[Option[Record]] =
    queryFindOpenById(id).pipe(run)

  def findOpenByIds(ids: Seq[UUID]): Future[Seq[Record]] =
    if (ids.isEmpty)
      Future.successful(Seq.empty)
    else
      queryOpenByIds(ids)
        .result
        .pipe(run)

  def queryFindOpenById(id: UUID) =
    queryOpenByIds(Seq(id)).result.headOption

  def queryOpenByIds(ids: Seq[UUID]) =
    table.filter(idColumnSelector(_) inSet ids)
}
