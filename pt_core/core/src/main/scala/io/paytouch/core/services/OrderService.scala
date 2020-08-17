package io.paytouch.core.services

import java.time.{ ZoneId, ZonedDateTime }
import java.util.UUID

import scala.concurrent._

import cats._
import cats.data._
import cats.implicits._

import com.typesafe.scalalogging.LazyLogging

import io.paytouch._
import io.paytouch.implicits._

import io.paytouch.core._
import io.paytouch.core.conversions.OrderConversions
import io.paytouch.core.data.daos._
import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums._
import io.paytouch.core.data.model.OrderItemUpdate
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.TicketStatus
import io.paytouch.core.errors._
import io.paytouch.core.expansions._
import io.paytouch.core.filters.OrderFilters
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.services.features._
import io.paytouch.core.utils._

import io.paytouch.core.utils.UtcTime
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.validators._

import OrderService._

final class OrderService(
    val customerLocationService: CustomerLocationService,
    val customerMerchantService: CustomerMerchantService,
    val locationService: LocationService,
    val locationSettingsService: LocationSettingsService,
    val loyaltyPointsHistoryService: LoyaltyPointsHistoryService,
    val giftCardPassService: GiftCardPassService,
    val kitchenService: KitchenService,
    val messageHandler: SQSMessageHandler,
    val orderDeliveryAddressService: OrderDeliveryAddressService,
    val onlineOrderAttributeService: OnlineOrderAttributeService,
    val orderBundleService: OrderBundleService,
    val orderDiscountService: OrderDiscountService,
    val orderItemService: OrderItemService,
    val orderSyncService: OrderSyncService,
    val orderTaxRateService: OrderTaxRateService,
    val orderUserService: OrderUserService,
    val paymentTransactionService: PaymentTransactionService,
    val rewardRedemptionService: RewardRedemptionService,
    val userService: UserService,
    val ticketService: TicketService,
    val tipsAssignmentService: TipsAssignmentService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends OrderConversions
       with FindByIdFeature
       with FindAllFeature
       with LazyLogging {
  type Dao = OrderDao
  type Entity = entities.Order
  type Expansions = OrderExpansions
  type Filters = OrderFilters
  type Record = OrderRecord
  type Validator = OrderValidator

  protected val dao = daos.orderDao
  protected val validator = new OrderValidator
  private[this] val openOrderValidator = new OpenOrderValidator
  val defaultFilters = OrderFilters()
  val defaultExpansions = OrderExpansions.empty

  def enrich(orders: Seq[Record], filters: Filters)(e: Expansions)(implicit user: UserContext): Future[Seq[Entity]] = {
    implicit val m: MerchantContext = user.toMerchantContext
    val orderIds = orders.map(_.id)
    val userIds = orders.flatMap(_.merchantNotes.map(_.userId)) ++ orders.flatMap(_.userId)
    val locationsPerOrderR = getLocationsPerOrder(orders)
    val customersPerOrderR = getCustomerMerchantsPerOrder(orders)
    val discountsPerOrderR = getDiscountsPerOrder(orders)
    val loyaltyPointsPerOrderR = getOptionalLoyaltyPointsPerOrder(orders)(e.withLoyaltyPoints)
    val paymentTransactionsPerOrderR = getOptionalPaymentTransactionsPerOrderId(orderIds)(e.withPaymentTransactions)
    val itemsPerOrderR = getOptionalItemsPerOrderId(orderIds)(e.withItems, e.withGiftCardPasses)
    val usersPerOrderR = getUsersPerOrder(orders)
    val usersR = userService.getUserInfoByIds(userIds)
    val taxRatePerOrderR = orderTaxRateService.findOrderTaxRatesPerOrder(orders)
    val ticketsPerOrderR = ticketService.findByOrders(orders)
    val ticketInfosPerOrderR = getOptionalTicketInfoPerOrder(orders, ticketsPerOrderR)(e)
    val rewardRedemptionsPerOrderR = rewardRedemptionService.findByOrders(orders)
    val deliveryAddressesPerOrderR = orderDeliveryAddressService.findAllByOrders(orders)
    val onlineOrderAttributesPerOrderR = onlineOrderAttributeService.findAllByOrders(orders)
    val orderBundlesPerOrderR = orderBundleService.findAllByOrders(orders)
    val kitchensR = kitchenService.getKitchensMap()
    val tipsAssignmentsPerOrderR = getTipsAssignmentsPerOrder(orders)
    for {
      customersPerOrder <- customersPerOrderR
      deliveryAddressesPerOrder <- deliveryAddressesPerOrderR
      onlineOrderAttributesPerOrder <- onlineOrderAttributesPerOrderR
      discountsPerOrder <- discountsPerOrderR
      locationsPerOrder <- locationsPerOrderR
      loyaltyPointsPerOrder <- loyaltyPointsPerOrderR
      paymentTransactionsPerOrder <- paymentTransactionsPerOrderR
      itemsPerOrder <- itemsPerOrderR
      orderBundlesPerOrder <- orderBundlesPerOrderR
      rewardRedemptionsPerOrder <- rewardRedemptionsPerOrderR
      usersPerOrder <- usersPerOrderR
      users <- usersR
      taxRatesPerOrder <- taxRatePerOrderR
      ticketsPerOrder <- ticketsPerOrderR
      ticketInfosPerOrder <- ticketInfosPerOrderR
      kitchens <- kitchensR
      tipsAssignmentsPerOrder <- tipsAssignmentsPerOrderR
    } yield fromRecordsAndOptionsToEntities(
      orders,
      customersPerOrder,
      deliveryAddressesPerOrder,
      onlineOrderAttributesPerOrder,
      discountsPerOrder,
      locationsPerOrder,
      loyaltyPointsPerOrder,
      paymentTransactionsPerOrder,
      rewardRedemptionsPerOrder,
      itemsPerOrder,
      orderBundlesPerOrder,
      usersPerOrder,
      users,
      taxRatesPerOrder,
      ticketsPerOrder,
      ticketInfosPerOrder,
      kitchens,
      tipsAssignmentsPerOrder,
    )
  }

  def findAllWithMetadata(
      filters: Filters,
    )(
      expansions: Expansions,
    )(implicit
      user: UserContext,
      pagination: Pagination,
    ): Future[(Seq[Entity], OrdersMetadata)] =
    findAll(filters)(expansions).flatMap {
      case (records, count) =>
        val salesSummaryR = getOptionalSalesSummary(filters)(expansions.withSalesSummary)
        val typeSummaryR = getOptionalTypeSummary(filters)(expansions.withTypeSummary)
        for {
          salesSummary <- salesSummaryR
          typeSummary <- typeSummaryR
        } yield (records, OrdersMetadata(count, salesSummary = salesSummary, typeSummary = typeSummary))
    }

  private def getOptionalLoyaltyPointsPerOrder(
      orders: Seq[Record],
    )(
      withLoyaltyPoints: Boolean,
    )(implicit
      user: UserContext,
    ): Future[Option[Map[Record, LoyaltyPoints]]] =
    if (withLoyaltyPoints)
      for {
        transactions <- getPaymentTransactionPerOrderId(orders.map(_.id))
        items <- getOrderItemsPerOrderId(orders.map(_.id))
        pointsPerOrder <- loyaltyPointsHistoryService.findByOrders(orders, transactions, items).map(Some(_))
      } yield pointsPerOrder
    else Future.successful(None)

  private def getOptionalPaymentTransactionsPerOrderId(
      orderIds: Seq[UUID],
    )(
      withPaymentTransactions: Boolean,
    ): Future[Option[Map[UUID, Seq[PaymentTransaction]]]] =
    if (withPaymentTransactions)
      getPaymentTransactionPerOrderId(orderIds).map(Some(_))
    else
      Future.successful(None)

  def getOptionalTicketInfoPerOrder(
      orders: Seq[Record],
      ticketsPerOrderR: Future[Map[OrderRecord, Seq[TicketRecord]]],
    )(
      e: Expansions,
    )(implicit
      user: UserContext,
    ): Future[Option[Map[OrderRecord, Seq[TicketInfo]]]] =
    if (e.withTickets)
      for {
        ticketsPerOrder <- ticketsPerOrderR
        enrichedTickets <-
          ticketService
            .enrich(ticketsPerOrder.values.toSeq.flatten, ticketService.defaultFilters)(TicketExpansions.empty)
      } yield Some(enrichedTickets.groupBy(_.orderId).mapKeysToRecords(orders).transform { (_, v) =>
        v.map(TicketInfo.apply)
      })
    else
      Future.successful(None)

  private def getPaymentTransactionPerOrderId(orderIds: Seq[UUID]): Future[Map[UUID, Seq[PaymentTransaction]]] =
    paymentTransactionService
      .findByOrderIds(orderIds)
      .map(_.groupBy(_.orderId))

  private def getOrderItemsPerOrderId(orderIds: Seq[UUID]): Future[Map[UUID, Seq[OrderItemRecord]]] =
    orderItemService
      .findRecordsByOrderIds(orderIds)
      .map(_.groupBy(_.orderId))

  private def getOptionalItemsPerOrderId(
      orderIds: Seq[UUID],
    )(
      withItems: Boolean,
      withGiftCardPasses: Boolean,
    )(implicit
      user: UserContext,
    ): Future[Option[Map[UUID, Seq[OrderItem]]]] =
    if (withItems)
      orderItemService
        .findByOrderIds(orderIds)(withGiftCardPasses)
        .map(_.groupBy(_.orderId).some)
    else
      Future.successful(None)

  private def getOptionalSalesSummary(
      filters: Filters,
    )(
      withSalesSummary: Boolean,
    )(implicit
      user: UserContext,
    ): Future[Option[SalesSummary]] =
    if (withSalesSummary)
      (
        dao.sumTotalAmount(user.merchantId, filters),
        dao.sumSubtotalAmount(user.merchantId, filters),
      ).mapN(SalesSummary.apply).map(_.some)
    else
      Future.successful(None)

  private def getOptionalTypeSummary(
      filters: Filters,
    )(
      withTypeSummary: Boolean,
    )(implicit
      user: UserContext,
    ): Future[Option[Seq[OrdersCountByType]]] =
    if (withTypeSummary)
      dao
        .countByOrderType(user.merchantId, filters)
        .map(_.map(OrdersCountByType.tupled).toSeq.some)
    else
      Future.successful(None)

  private def getLocationsPerOrder(
      orders: Seq[OrderRecord],
    )(implicit
      user: UserContext,
    ): Future[Map[OrderRecord, Location]] =
    locationService
      .findByIds(orders.flatMap(_.locationId))
      .map { locations =>
        orders.flatMap { order =>
          locations
            .find(l => order.locationId.contains(l.id))
            .map(order -> _)
        }.toMap
      }

  private def getCustomerMerchantsPerOrder(
      orders: Seq[Record],
    )(implicit
      user: UserContext,
    ): Future[Map[Record, CustomerMerchant]] = {
    val customerIds = orders.flatMap(_.customerId)
    val expansions = CustomerExpansions(
      withVisits = true,
      withSpend = true,
      withLocations = false,
      withLoyaltyPrograms = true,
      withAvgTips = true,
      withLoyaltyMemberships = true,
      withBillingDetails = true,
    )

    customerMerchantService.findByCustomerIds(customerIds)(expansions).map { customers =>
      orders.flatMap { order =>
        val customer = customers.find(c => order.customerId.contains(c.id))
        customer.map(c => (order, c))
      }.toMap
    }
  }

  private def getDiscountsPerOrder(
      orders: Seq[Record],
    )(implicit
      user: UserContext,
    ): Future[Map[Record, Seq[OrderDiscount]]] =
    orderDiscountService.findByOrderIds(orders.map(_.id)).map(result => result.mapKeysToRecords(orders))

  def getUsersPerOrder(records: Seq[Record]): Future[Map[Record, Seq[UserInfo]]] =
    orderUserService.findAllUsersByOrderIds(records.map(_.id)).map(_.mapKeysToRecords(records))

  def getAvgTipsPerCustomer(
      customerIds: Seq[UUID],
      locationId: Option[UUID],
    )(implicit
      user: UserContext,
    ): Future[Map[UUID, Seq[MonetaryAmount]]] =
    dao
      .getAvgTipsPerCustomer(customerIds = customerIds, locationIds = user.accessibleLocations(locationId))
      .map {
        _.transform((_, amount) => Seq(MonetaryAmount(amount)))
      }

  private def getTipsAssignmentsPerOrder(
      orders: Seq[Record],
    )(implicit
      user: UserContext,
    ): Future[Map[Record, Seq[TipsAssignment]]] =
    tipsAssignmentService
      .findByOrderIds(orders.map(_.id))
      .map(_.mapKeysToRecords(orders))

  def findByIds(ids: Seq[UUID])(e: Expansions)(implicit user: UserContext): Future[Seq[entities.Order]] =
    dao
      .findByIds(ids)
      .flatMap(enrich(_, defaultFilters)(e))

  def findOpenById(
      id: UUID,
      filters: Filters = defaultFilters,
    )(
      expansions: Expansions,
    )(implicit
      user: UserContext,
    ): Future[Option[Entity]] =
    openOrderValidator
      .accessOneById(id)
      .flatMapTraverse(enrich(_, filters)(expansions))
      .map(_.toOption)

  def sendReceipt(
      orderId: UUID,
      paymentTransactionId: Option[UUID],
      sendReceiptData: SendReceiptData,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Unit]] =
    validator
      .canSendReceipt(orderId, paymentTransactionId, sendReceiptData)
      .flatMapTraverse { order =>
        val email = sendReceiptData.recipientEmail

        updateCustomerInformation(email, order)
          .map(messageHandler.sendPrepareOrderReceiptMsg(_, paymentTransactionId, email))
      }

  private def updateCustomerInformation(email: String, order: Record)(implicit user: UserContext): Future[Entity] =
    order.customerId.fold(createNewCustomerWithEmailAndUpdateOrder(email, order)) {
      updateEmptyEmailCustomerWithEmail(email, _, order)
        .flatMap(enrich(_, defaultFilters)(OrderExpansions.withFullOrderItems))
    }

  private def createNewCustomerWithEmailAndUpdateOrder(
      email: String,
      order: Record,
    )(implicit
      user: UserContext,
    ): Future[Entity] =
    for {
      validatedResult <- customerMerchantService.create(CustomerMerchantUpsertion(email = email.some))
      customerId = validatedResult.toOption.map { case (_, customer) => customer.id }
      update = OrderUpdate.empty(order.id).copy(customerId = customerId)
      (_, updatedOrder) <- orderSyncService.updateOrder(order.id, update)
    } yield updatedOrder

  private def updateEmptyEmailCustomerWithEmail(
      email: String,
      customerId: UUID,
      order: Record,
    )(implicit
      user: UserContext,
    ): Future[Record] =
    for {
      customer <- customerMerchantService.findById(customerId)(CustomerExpansions.empty)
      _ <- updateCustomerEmailIfNeeded(customer, email)
    } yield order

  private def updateCustomerEmailIfNeeded(
      maybeCustomer: Option[CustomerMerchant],
      email: String,
    )(implicit
      user: UserContext,
    ): Future[Option[CustomerMerchant]] =
    maybeCustomer match {
      case Some(customer) if customer.email.isEmpty =>
        val update = CustomerMerchantUpsertion(email = Some(email))
        customerMerchantService.update(customer.id, update).map { case _ => maybeCustomer }
      case cust => Future.successful(cust)
    }

  def syncById(
      id: UUID,
      upsertion: entities.OrderUpsertion,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Result[Entity]]] =
    orderSyncService.syncById(id, upsertion)

  def validatedSyncById(
      id: UUID,
      upsertion: entities.OrderUpsertion,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Result[Entity]]] =
    orderSyncService.validatedSyncById(id, upsertion)

  def findOrderPointsDataById(orderId: UUID): Future[Option[OrderPointsData]] =
    (for {
      order <- OptionT(dao.findById(orderId))
      paymentTransactions <- OptionT.liftF(paymentTransactionService.findByOrderIds(Seq(orderId)))
      orderItems <- OptionT.liftF(orderItemService.findRecordsByOrderIds(Seq(orderId)))
      orderPointsData <- OptionT.fromOption[Future](OrderPointsData.extract(order, paymentTransactions, orderItems))
    } yield orderPointsData).value

  def rejectOrder(order: OrderRecord)(implicit user: UserContext): Future[Entity] =
    OrderUpdate
      .empty(order.id)
      .copy(
        status = Some(OrderStatus.Canceled),
        paymentStatus = Some(PaymentStatus.Voided),
      )
      .pipe(orderSyncService.updateOrder(order.id, _))
      .map(_._2)

  def findByDeliveryProviderId(
      deliveryProvider: DeliveryProvider,
      deliveryProviderId: String,
    )(
      expansions: Expansions,
    )(implicit
      user: UserContext,
    ): Future[Option[Entity]] =
    dao
      .findByDeliveryProviderId(deliveryProvider, deliveryProviderId)
      .flatMap(enrich(_, defaultFilters)(expansions))

  def storePaymentTransaction(
      orderId: UUID,
      transaction: OrderService.PaymentTransactionUpsertion,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Result[Entity]]] =
    openOrderValidator.accessOneById(orderId).flatMapTraverse { order =>
      (
        getOrderItemsPerOrderId(Seq(orderId)).map(_.getOrElse(orderId, Seq.empty)),
        paymentTransactionService.findRecordsById(orderId),
        ticketService.findByOrderId(order.id),
        locationSettingsService
          .findAllByLocationIds(order.locationId.toSeq)
          .map(_.headOption.exists(_.orderAutocomplete)),
        locationService.findTimezoneForLocationWithFallback(order.locationId)(user.toMerchantContext),
        onlineOrderAttributeService.findRecordByOrderId(orderId),
      ).mapN { (orderItems, paymentTransactions, tickets, orderAutocomplete, zoneId, onlineOrderAttribute) =>
        (
          orderItems,
          services
            .ordertransitions
            .PaymentTransactionSubmitted(
              order,
              orderItems,
              paymentTransactions,
              tickets,
              transaction,
              orderAutocomplete,
              zoneId,
              onlineOrderAttribute,
            ),
        )
      }.flatMap {
        case (orderItems, services.ordertransitions.Errors.ErrorsAndResult(errors, upsertion)) =>
          errors.map(e => logger.error(e.message))
          // TODO: move this logic inside ordertransitions.PaymentTransactionSubmitted
          // TODO: handle stocks like orderSyncService.upsertOrder -> stockModifierService.modifyStocks
          val justPaidOrderItemUpdatesIds: Seq[UUID] =
            upsertion
              .orderItems
              .filter(_.orderItem.paymentStatus.contains(PaymentStatus.Paid))
              .flatMap(_.orderItem.id)
          for {
            result <- orderSyncService.updateOrder(orderId, upsertion)
            justPaidGiftCardItems =
              orderItems
                .filter(i => i.productType.exists(_.isGiftCard) && justPaidOrderItemUpdatesIds.contains(i.id))
            giftCardPasses <- giftCardPassService.createGiftCardPasses(justPaidGiftCardItems)
            _ <-
              giftCardPassService.sendGiftCardPassReceipts(justPaidGiftCardItems, giftCardPasses.getOrElse(Seq.empty))
          } yield result
      }
    }

  def postTicketUpsert(orderId: UUID, locationId: UUID)(implicit user: UserContext): Future[Option[Entity]] =
    (
      dao.findById(orderId),
      ticketService.findByOrderId(orderId),
      locationSettingsService
        .findAllByLocationIds(Seq(locationId))
        .map(_.headOption.exists(_.orderAutocomplete)),
      locationService.findTimezoneForLocationWithFallback(locationId.some)(user.toMerchantContext),
    ).mapN {
      case (maybeOrder, tickets, orderAutocomplete, zoneId) =>
        maybeOrder match {
          case Some(order) =>
            val errorsAndResult =
              services.ordertransitions.TicketUpserted(order, tickets, orderAutocomplete, zoneId)
            errorsAndResult.errors.map(e => logger.error(e.message))
            errorsAndResult.data
          case _ => none
        }
    }.flatMap(
      _.fold[Future[Option[Entity]]](none.pure[Future])(update =>
        orderSyncService.updateOrder(orderId, update).map(_._2.some),
      ),
    )
}

object OrderService {
  final case class PaymentTransactionUpsertion(
      id: UUID,
      `type`: TransactionType,
      paymentType: TransactionPaymentType,
      paymentDetails: PaymentDetails,
      paidAt: ZonedDateTime,
      orderItemIds: Option[Seq[UUID]] = none,
      version: Int,
      paymentProcessor: TransactionPaymentProcessor,
    ) {
    def toPaymentTransactionUpdate(merchantId: UUID, orderId: UUID): PaymentTransactionUpdate =
      PaymentTransactionUpdate(
        id = id.some,
        merchantId = merchantId.some,
        orderId = orderId.some,
        customerId = none,
        `type` = `type`.some,
        refundedPaymentTransactionId = none,
        paymentType = paymentType.some,
        paymentDetails = paymentDetails.some,
        version = version.some,
        paidAt = paidAt.some,
        paymentProcessor = paymentProcessor.some,
      )
  }
}
