package io.paytouch.core.services

import java.time.ZonedDateTime
import java.util.UUID

import scala.concurrent._

import cats.data._
import cats.implicits._

import io.paytouch.implicits._

import io.paytouch.core._
import io.paytouch.core.conversions.OrderSyncConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums._
import io.paytouch.core.data.model.upsertions.{ CustomerUpsertion, OrderUpsertion => OrderUpsertionModel }
import io.paytouch.core.entities.{ Order => OrderEntity, OrderUpsertion => OrderUpsertionEntity, _ }
import io.paytouch.core.entities.enums.CustomerSource
import io.paytouch.core.expansions.OrderExpansions
import io.paytouch.core.filters.OrderFilters
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.utils._
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators._

class OrderSyncService(
    val messageHandler: SQSMessageHandler,
    val locationService: LocationService,
    val customerLocationService: CustomerLocationService,
    val customerMerchantService: CustomerMerchantService,
    val customerMerchantSyncService: CustomerMerchantSyncService,
    giftCardPassService: => GiftCardPassService,
    val loyaltyProgramService: LoyaltyProgramService,
    merchantService: => MerchantService,
    onlineOrderAttributeService: => OnlineOrderAttributeService,
    orderService: => OrderService,
    val orderBundleService: OrderBundleService,
    val orderDeliveryAddressService: OrderDeliveryAddressService,
    val orderDiscountService: OrderDiscountService,
    val orderItemService: OrderItemService,
    val orderTaxRateService: OrderTaxRateService,
    val orderUserService: OrderUserService,
    val paymentTransactionService: PaymentTransactionService,
    val paymentTransactionFeeService: PaymentTransactionFeeService,
    val paymentTransactionOrderItemService: PaymentTransactionOrderItemService,
    val rewardRedemptionService: RewardRedemptionService,
    val stockModifierService: StockModifierService,
    val storeService: StoreService,
    val ticketService: TicketService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
    val logger: PaytouchLogger,
  ) extends OrderSyncConversions {
  protected val dao = daos.orderDao
  val recoveryValidator = new OrderRecoveryValidator

  val orderItemDao = daos.orderItemDao
  val orderBundleDao = daos.orderBundleDao

  val defaultFilters = OrderFilters()
  val defaultExpansions = OrderExpansions.withFullOrderItems

  def syncById(
      id: UUID,
      upsertion: OrderUpsertionEntity,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Result[OrderEntity]]] =
    recoverOrder(id, upsertion).flatMapTraverse {
      case (orderUpsertion, customerUpsertion) =>
        upsertOrder(id, upsertion, orderUpsertion, customerUpsertion)
    }

  def validatedSyncById(
      id: UUID,
      upsertion: OrderUpsertionEntity,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Result[OrderEntity]]] =
    validateOrder(id, upsertion).flatMapTraverse {
      case (orderUpsertion, customerUpsertion) =>
        upsertOrder(id, upsertion, orderUpsertion, customerUpsertion)
    }

  def postOrderUpdateCustomerUpdates(orderRecord: OrderRecord)(implicit user: UserContext) =
    for {
      _ <- customerLocationService.updateSpendingActivity(orderRecord.locationId, orderRecord.customerId)
      _ <- loyaltyProgramService.logPoints(orderRecord)(user.toMerchantContext)
    } yield ()

  private def validateOrder(
      id: UUID,
      upsertion: OrderUpsertionEntity,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[(OrderUpsertionModel, Option[CustomerUpsertion])]] =
    for {
      validOrder <- recoveryValidator.validateUpsertion(id, upsertion)
      validOrderUpsertion <- recoverUpsertionModel(id, upsertion)
      validCustomerUpsertion <- validateCustomerUpsertionIfOnlineOrder(upsertion)
    } yield Multiple.combine(validOrder, validOrderUpsertion, validCustomerUpsertion) {
      (_, orderUpsertion, customerUpsertion) =>
        val maybeCustomerId = customerUpsertion.flatMap(_.globalCustomer.id)

        val updatedOrderUpsertion = maybeCustomerId.fold(orderUpsertion) { customerId =>
          orderUpsertion.copy(order = orderUpsertion.order.copy(customerId = Some(customerId)))
        }

        updatedOrderUpsertion -> customerUpsertion
    }

  private def recoverOrder(
      id: UUID,
      upsertion: OrderUpsertionEntity,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[(OrderUpsertionModel, Option[CustomerUpsertion])]] =
    (for {
      validOrderUpsertion <- recoverUpsertionModel(id, upsertion)
      validCustomerUpsertion <- validateCustomerUpsertionIfOnlineOrder(upsertion)
    } yield (validOrderUpsertion, validCustomerUpsertion).tupled).flatMapTraverse {
      case (orderUpsertion, customerUpsertion) =>
        locationService
          .findTimezoneForLocationWithFallback(orderUpsertion.order.locationId)(user.toMerchantContext)
          .map { locationZoneId =>
            val maybeCustomerId =
              customerUpsertion.flatMap(_.globalCustomer.id)

            val updatedOrderUpsertion =
              maybeCustomerId.fold(orderUpsertion) { customerId =>
                orderUpsertion.copy(
                  order = {
                    val withCustomer =
                      orderUpsertion.order.copy(customerId = customerId.some)

                    // This currently sets the fields to exactly the same values as they are
                    // (i.e. they remain unchanged) so it's technically redundant but
                    // we are hoping that this design will be useful in the future.
                    orderUpsertion.events.foldLeft(withCustomer) { (update, event) =>
                      event match {
                        case RecoveredOrderUpsertion.Event.OrderMarkedAsReadyForPickup =>
                          update.copy(`type` = OrderType.TakeOut.some)

                        case RecoveredOrderUpsertion.Event.OrderCompleted =>
                          val completedAt = UtcTime.now

                          update.copy(
                            status = OrderStatus.Completed.some,
                            completedAt = completedAt.some,
                            completedAtTz = completedAt.toLocationTimezone(locationZoneId).some,
                          )
                      }
                    }
                  },
                )
              }

            updatedOrderUpsertion -> customerUpsertion
          }
    }

  private def validateCustomerUpsertionIfOnlineOrder(
      upsertion: OrderUpsertionEntity,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[CustomerUpsertion]]] =
    (
      upsertion.customerId,
      upsertion.onlineOrderAttribute,
      upsertion.source,
      upsertion.deliveryProvider,
    ) match {
      case (None, Some(u), Some(Source.Storefront), _) =>
        convertOnlineOrderCustomer(u, upsertion.deliveryAddress, CustomerSource.PtStorefront)

      case (None, Some(u), Some(Source.DeliveryProvider), Some(provider)) =>
        convertOnlineOrderCustomer(u, upsertion.deliveryAddress, provider.toCustomerSourceType)

      case _ =>
        Multiple.empty.pure[Future]
    }

  private def convertOnlineOrderCustomer(
      attribute: OnlineOrderAttributeUpsertion,
      deliveryAddress: Option[DeliveryAddressUpsertion],
      customerSource: CustomerSource,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[CustomerUpsertion]]] =
    customerMerchantSyncService
      .validateSyncAndConvert(
        id = None,
        source = customerSource,
        update = CustomerMerchantUpsertion(
          firstName = attribute.firstName,
          lastName = attribute.lastName,
          email = attribute.email,
          phoneNumber = attribute.phoneNumber,
          address = AddressSync(
            line1 = deliveryAddress.flatMap(_.address.line1),
            line2 = deliveryAddress.flatMap(_.address.line2),
            city = deliveryAddress.flatMap(_.address.city),
            state = deliveryAddress.flatMap(_.address.state),
            country = deliveryAddress.flatMap(_.address.country),
            stateCode = deliveryAddress.flatMap(_.address.stateCode),
            countryCode = deliveryAddress.flatMap(_.address.countryCode),
            postalCode = deliveryAddress.flatMap(_.address.postalCode),
          ),
        ),
      )

  private def upsertOrder(
      id: UUID,
      upsertion: OrderUpsertionEntity,
      orderUpsertion: OrderUpsertionModel,
      customerUpsertion: Option[CustomerUpsertion],
    )(implicit
      user: UserContext,
    ): Future[(ResultType, OrderEntity)] = {
    implicit val merchant = user.toMerchantContext
    for {
      existingOrderItems <- orderItemDao.findByOrderId(id)
      existingOrderBundles <- orderBundleDao.findByOrderId(id)
      _ <- customerSync(customerUpsertion)
      result <- baseOrderUpsert(id, orderUpsertion)(defaultExpansions) { existingOrder =>
        for {
          _ <- stockModifierService.modifyStocks(
            id,
            orderUpsertion,
            existingOrder,
            existingOrderItems,
            existingOrderBundles,
          )
          _ <- sendGiftCardPassReceipts(upsertion.items.toList, existingOrderItems)
          _ <- giftCardPassService.upsertPassesByOrderItemIds(upsertion.items.map(_.id))
        } yield ()
      }
    } yield result
  }

  private def sendGiftCardPassReceipts(
      upsertionItems: List[(OrderItemUpsertion)],
      existingOrderItems: Seq[OrderItemRecord],
    )(implicit
      user: UserContext,
    ): Future[Unit] =
    upsertionItems
      .map(item => item.id -> item.giftCardPassRecipientEmail)
      .collect {
        case (orderItemId, Some(recipientEmail)) =>
          getOptionalGiftCardPasses(existingOrderItems)(withGiftCardPasses = true)
            .map(_.exists(_.get(orderItemId).exists(_.recipientEmail.isEmpty)))
            .flatMap { isRecipientEmailEmpty =>
              if (isRecipientEmailEmpty)
                giftCardPassService
                  .sendReceipt(orderItemId, SendReceiptData(recipientEmail))
                  .void
              else
                Future.unit
            }
      }
      .sequence
      .void

  private def getOptionalGiftCardPasses(
      records: Seq[OrderItemRecord],
    )(
      withGiftCardPasses: Boolean,
    )(implicit
      user: UserContext,
    ): Future[Option[Map[UUID, GiftCardPassInfo]]] =
    orderItemService
      .getOptionalGiftCardPasses(records)(withGiftCardPasses)
      .map(_.map(_.map(_.leftMap(_.id))))

  def updateOrder(id: UUID, update: OrderUpdate)(implicit user: UserContext): Future[(ResultType, OrderEntity)] =
    updateOrder(id, OrderUpsertionModel.empty(update))

  def updateOrder(
      id: UUID,
      update: OrderUpsertionModel,
    )(implicit
      user: UserContext,
    ): Future[(ResultType, OrderEntity)] =
    baseOrderUpsert(id, update)(OrderExpansions.withFullOrderItems)(_ => Future.unit)

  private def baseOrderUpsert[T](
      id: UUID,
      orderUpsertion: OrderUpsertionModel,
    )(
      expansions: OrderExpansions,
    )(
      extraActions: Option[OrderEntity] => Future[T],
    )(implicit
      user: UserContext,
    ): Future[(ResultType, OrderEntity)] =
    for {
      existingOrder <- orderService.findOpenById(id, defaultFilters)(defaultExpansions)
      (resultType, order) <- dao.upsert(orderUpsertion)
      _ <- ticketService.postOrderUpsertActions(order)
      _ <- postOrderUpdateCustomerUpdates(order)
      _ <- extraActions(existingOrder)
      orderEntity <- enrich(order)(OrderExpansions.withFullOrderItems)
      _ <- sendOrderMessages(resultType, orderEntity, existingOrder, orderUpsertion.events)
    } yield (resultType, orderEntity)

  private def customerSync(
      maybeUpsertion: Option[CustomerUpsertion],
    )(implicit
      user: UserContext,
    ): Future[Option[CustomerMerchant]] =
    maybeUpsertion match {
      case Some(upsertion) =>
        customerMerchantSyncService
          .upsert(upsertion, None)
          .map { case (_, customer) => Some(customer) }

      case _ =>
        Future.successful(None)
    }

  private def recoverUpsertionModel(
      orderId: UUID,
      upsertion: OrderUpsertionEntity,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[OrderUpsertionModel]] =
    for {
      recoveredUpsertion <- recoveryValidator.recoverUpsertion(orderId, upsertion)
      customerLocation <- customerLocationService.recoverCustomerLocationUpdate(recoveredUpsertion)
      order <- recoverOrderUpdate(recoveredUpsertion)
      orderItems <- orderItemService.recoverUpsertionModels(recoveredUpsertion)
      paymentTransactions <- paymentTransactionService.recoverPaymentTransactionUpdates(recoveredUpsertion)
      paymentTransactionFees <- paymentTransactionFeeService.recoverPaymentTransactionFeeUpdates(recoveredUpsertion)
      paymentTransactionOrderItems <-
        paymentTransactionOrderItemService.recoverPaymentTransactionOrderItemUpdates(recoveredUpsertion)
      creatorOrderUser <- orderUserService.recoverCreatorOrderUserUpdates(recoveredUpsertion)
      assignedOrderUsers <- orderUserService.recoverAssignedOrderUserUpdates(recoveredUpsertion)
      orderTaxRates <- orderTaxRateService.recoverOrderTaxRateUpdates(recoveredUpsertion)
      orderDiscounts <- orderDiscountService.convertToOrderDiscountUpdates(recoveredUpsertion)
      validGiftCardPasses <- giftCardPassService.convertToGiftCardPassUpdates(recoveredUpsertion)
      rewardRedemptions <- rewardRedemptionService.recoverRewardRedemptions(recoveredUpsertion)
      deliveryAddress <- orderDeliveryAddressService.convertToOrderDeliveryAddressUpdate(recoveredUpsertion)
      onlineOrderAttribute <- onlineOrderAttributeService.convertToOnlineOrderAttributeUpdate(recoveredUpsertion)
      orderBundles <- orderBundleService.recoverOrderBundleUpdates(recoveredUpsertion)
    } yield validGiftCardPasses.map { giftCardPasses =>
      OrderUpsertionModel(
        order,
        orderItems,
        creatorOrderUser,
        assignedOrderUsers,
        paymentTransactions,
        paymentTransactionFees,
        paymentTransactionOrderItems,
        customerLocation,
        orderTaxRates.some,
        orderDiscounts.some,
        giftCardPasses.some,
        rewardRedemptions.some,
        recoveredUpsertion.canDeleteOrderItems,
        recoveredUpsertion.events,
        deliveryAddress,
        onlineOrderAttribute,
        orderBundles.some,
      )
    }

  def recoverOrderUpdate(upsertion: RecoveredOrderUpsertion)(implicit user: UserContext): Future[OrderUpdate] =
    fromUpsertionToUpdate(upsertion).pure[Future]

  def enrich(item: OrderRecord)(expansions: OrderExpansions)(implicit user: UserContext): Future[OrderEntity] =
    orderService.enrich(item, defaultFilters)(expansions)

  private def sendOrderMessages(
      resultType: ResultType,
      order: OrderEntity,
      existingOrder: Option[OrderEntity],
      events: List[RecoveredOrderUpsertion.Event],
    )(implicit
      user: UserContext,
    ): Future[Unit] = {
    implicit val m: MerchantContext = user.toMerchantContext

    if (order.isOpen)
      Future.unit
    else {
      val sendOrderStatusMessagesR: Future[Unit] =
        sendOrderStatusMessages(resultType, order).pure[Future]

      val sendOnlineOrderMessagesR: Future[Option[Unit]] =
        (for {
          recipientEmail <- OptionT.fromOption[Future](order.customer.flatMap(_.email))
          receiptContext <- OptionT(merchantService.prepareReceiptContext(recipientEmail, order))
          source <- OptionT.fromOption[Future](order.source) if source == Source.Storefront
        } yield sendOnlineOrderMessages(resultType, receiptContext, existingOrder, events)).value

      for {
        _ <- sendOrderStatusMessagesR
        _ <- sendOnlineOrderMessagesR
      } yield messageHandler.sendOrderSyncedMsg(order)
    }
  }

  private def sendOrderStatusMessages(resultType: ResultType, order: OrderEntity)(implicit user: UserContext): Unit =
    resultType match {
      case ResultType.Created => messageHandler.sendOrderCreatedMsg(order)
      case ResultType.Updated => messageHandler.sendOrderUpdatedMsg(order)
    }

  private def sendOnlineOrderMessages(
      resultType: ResultType,
      receiptContext: ReceiptContext,
      existingOrder: Option[OrderEntity],
      events: List[RecoveredOrderUpsertion.Event],
    )(implicit
      merchant: MerchantContext,
    ) = {
    val updatedOrder = receiptContext.order

    // Send order received email (onlineOrderCreated) when an order acceptance status transitions from open -> pending
    val existingAcceptanceStatus = existingOrder.flatMap(_.onlineOrderAttribute).map(_.acceptanceStatus)
    val updatedAcceptanceStatus = updatedOrder.onlineOrderAttribute.map(_.acceptanceStatus)
    val isUpdatePending =
      Boolean.and(
        existingAcceptanceStatus != updatedAcceptanceStatus,
        updatedAcceptanceStatus.contains(AcceptanceStatus.Pending),
      )

    if (isUpdatePending)
      messageHandler.sendOnlineOrderCreatedMsg(receiptContext)

    val existingStatus = existingOrder.flatMap(_.status)
    val updatedStatus = updatedOrder.status

    val isCancelledAndNotOpen =
      Boolean.and(
        existingStatus != updatedStatus,
        updatedStatus.contains(OrderStatus.Canceled),
        !updatedAcceptanceStatus.contains(AcceptanceStatus.Open),
      )

    if (isCancelledAndNotOpen)
      messageHandler.sendOnlineOrderCanceledMsg(receiptContext)

    events.foreach {
      case RecoveredOrderUpsertion.Event.OrderMarkedAsReadyForPickup =>
        val isCustomerOnSite: Boolean = {
          val estimatedReadyAt: Option[ZonedDateTime] =
            existingOrder.flatMap(_.onlineOrderAttribute).flatMap(_.estimatedReadyAt)

          val acceptedAt: Option[ZonedDateTime] =
            existingOrder.flatMap(_.onlineOrderAttribute).flatMap(_.acceptedAt)

          (estimatedReadyAt, acceptedAt).mapN(_ == _).contains(true)
        }

        val isCustomerRemote =
          !isCustomerOnSite

        if (isCustomerRemote)
          messageHandler.sendOnlineOrderReadyForPickupMsg(receiptContext)

      case RecoveredOrderUpsertion.Event.OrderCompleted =>
    }
  }

  def ensureOrderNumberInChronologicalOrder(
      orderUpsertionsRef: Map[(String, UUID), OrderUpsertionEntity],
    )(implicit
      user: UserContext,
    ): Future[Boolean] = {
    import io.paytouch.core.ordering
    val idWithReceivedAt = orderUpsertionsRef.map { case ((_, id), upsertion) => id -> upsertion.receivedAt }.toSeq
    val sortedIdWithIdx = idWithReceivedAt.sortBy { case (id, date) => (date, id) }.zipWithIndex
    val idWithNumber = sortedIdWithIdx.map { case ((id, _), idx) => id -> s"${idx + 1}" }.toMap
    dao.updateOrderNumber(idWithNumber)
  }
}
