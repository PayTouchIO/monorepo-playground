package io.paytouch.core.validators

import java.time.ZonedDateTime
import java.util.UUID

import scala.concurrent._

import cats.implicits._

import io.paytouch._
import io.paytouch.implicits._

import io.paytouch.core.data.daos.{ Daos, OrderDao }
import io.paytouch.core.data.model.{ LocationRecord, OrderRecord, UserRecord }
import io.paytouch.core.data.model.enums._
import io.paytouch.core.entities._
import io.paytouch.core.errors._
import io.paytouch.core.RichZoneDateTime
import io.paytouch.core.utils.{ Multiple, PaytouchLogger }
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.features.DefaultRecoveryValidator

class OrderRecoveryValidator(
    implicit
    val ec: ExecutionContext,
    val daos: Daos,
    val logger: PaytouchLogger,
  ) extends DefaultRecoveryValidator[OrderRecord] {
  type Record = OrderRecord
  type Dao = OrderDao

  protected val dao = daos.orderDao
  val validationErrorF = InvalidOrderIds(_)
  val accessErrorF = NonAccessibleOrderIds(_)

  val customerValidator = new CustomerMerchantValidator
  val itemDiscountValidator = new OrderDiscountRecoveryValidator
  val locationValidator = new LocationValidatorIncludingDeleted
  val orderItemValidator = new OrderItemRecoveryValidator
  val orderTaxRatesValidator = new OrderTaxRateRecoveryValidator
  val paymentTransactionValidator = new PaymentTransactionRecoveryValidator
  val rewardRedemptionValidator = new RewardRedemptionValidator
  val userValidator = new UserValidatorIncludingDeleted
  val deliveryAddressValidator = new DeliveryAddressRecoveryValidator
  val onlineOrderAttributeValidator = new OnlineOrderAttributeRecoveryValidator
  val orderBunValidator = new OrderBundleValidator
  val orderInventoryValidator = new OrderInventoryValidator

  override def recordsFinder(ids: Seq[UUID])(implicit user: UserContext): Future[Seq[OrderRecord]] =
    daos.orderDao.findOpenByIds(ids)

  def validateUpsertion(
      id: UUID,
      upsertion: OrderUpsertion,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[OrderUpsertion]] =
    for {
      validExistingOrder <- validateOneById(id)
      validOrderId = validExistingOrder.map(_ => id)
      validLocation <- locationValidator.accessOneById(upsertion.locationId)
      customerId <- customerValidator.accessOneByOptId(upsertion.customerId)
      userIds = upsertion.creatorUserId.toSeq ++ upsertion.assignedUserIds.getOrElse(Seq.empty)
      users <- userValidator.filterValidByIds(userIds)
      validDiscounts <- itemDiscountValidator.validateUpsertions(upsertion.discounts)
      validOrderItems <- orderItemValidator.validateUpsertions(id, upsertion.items)
      validOrderTaxRates <- orderTaxRatesValidator.validateUpsertions(upsertion.taxRates)
      validPaymentTransactions <- paymentTransactionValidator.validateUpsertions(id, upsertion.paymentTransactions)
      validRewardRedemptions <- rewardRedemptionValidator.validateUpsertions(id, upsertion)
      validDeliveryAddress <- deliveryAddressValidator.validateUpsertion(upsertion.deliveryAddress)
      validOnlineOrderAttribute <- onlineOrderAttributeValidator.validateUpsertion(id, upsertion)
      validBundles <- orderBunValidator.validateUpsertions(id, upsertion.bundles, upsertion.items.map(_.id))
      validInventory <- orderInventoryValidator.validateUpsertions(id, upsertion, validExistingOrder.toOption.flatten)
    } yield Multiple.combine(
      validLocation,
      validExistingOrder,
      validDiscounts,
      validOrderItems,
      validOrderTaxRates,
      validPaymentTransactions,
      validRewardRedemptions,
      validDeliveryAddress,
      validOnlineOrderAttribute,
      validBundles,
      validInventory,
    ) {
      case _ => upsertion
    }

  def recoverUpsertion(
      id: UUID,
      upsertion: OrderUpsertion,
    )(implicit
      user: UserContext,
    ): Future[RecoveredOrderUpsertion] =
    for {
      validExistingOrder <- validateOneById(id)
      validExistingOrderId = validExistingOrder.as(id)
      location <- locationValidator.accessOneById(upsertion.locationId).mapNested(Option.apply)
      customerId <- customerValidator.accessOneByOptId(upsertion.customerId)
      userIds = upsertion.creatorUserId.toSeq ++ upsertion.assignedUserIds.getOrElse(Seq.empty)
      users <- userValidator.filterValidByIds(userIds)
      recoveredDiscounts <- itemDiscountValidator.recoverUpsertions(upsertion.discounts)
      recoveredOrderItems <- orderItemValidator.recoverUpsertions(id, upsertion.items)
      recoveredOrderTaxRates <- orderTaxRatesValidator.recoverUpsertions(upsertion.taxRates)
      recoveredPaymentTransactions <- paymentTransactionValidator.recoverUpsertions(id, upsertion.paymentTransactions)
      recoveredRewardRedemptions <- rewardRedemptionValidator.recoverUpsertions(id, upsertion)
      recoveredDeliveryAddress <- deliveryAddressValidator.recoverUpsertion(upsertion.deliveryAddress)
      recoveredOnlineOrderAttribute <- onlineOrderAttributeValidator.recoverUpsertion(id, upsertion)
      recoveredBundles <- orderBunValidator.recoverUpsertions(id, upsertion.bundles, recoveredOrderItems)
      recoveredOrderItemsWithBundleCost <- orderBunValidator.recoverBundleItemCost(
        recoveredBundles,
        recoveredOrderItems,
      )
    } yield {
      val contextDescription = s"While validating order upsertion of order $id"
      val recoveredExistingOrGeneratedOrderId =
        logger.loggedRecoverUUID(validExistingOrderId)(contextDescription, upsertion)
      val recoveredLocation = logger.loggedRecover(location)(contextDescription, upsertion)
      val recoveredCustomerId = logger.loggedRecover(customerId)(contextDescription, upsertion).map(_.id)
      val recoveredCreatorUserId =
        logger.loggedRecover(recoverCreatorUserId(users, upsertion.creatorUserId))(contextDescription, upsertion)
      val recoveredAssignedUserIds = {
        val existingIds = upsertion.assignedUserIds.map(assignUserIds => users.map(_.id) intersect assignUserIds)
        logger.loggedGenericRecover(recoverAssignedUserIds(users, upsertion.assignedUserIds), existingIds)(
          contextDescription,
          upsertion,
        )
      }
      val sourceWithDefault: Source = {
        val maybeExistingOrder: Option[OrderRecord] = validExistingOrder.toOption.flatten
        val maybeExistingOrderSource: Option[Source] = maybeExistingOrder.flatMap(_.source)

        upsertion.source.orElse(maybeExistingOrderSource).getOrElse(Source.Register)
      }

      val events: List[RecoveredOrderUpsertion.Event] =
        validExistingOrder
          .map { existingOrder =>
            val isTakeOutCompleted =
              Boolean.and(
                upsertion.`type` == OrderType.TakeOut,
                upsertion.status == OrderStatus.Completed,
                // using forall so that the expression evaluates to true even for nonexistent orders
                existingOrder.forall(!_.`status`.contains(OrderStatus.Completed)),
              )

            if (isTakeOutCompleted)
              List(
                RecoveredOrderUpsertion.Event.OrderMarkedAsReadyForPickup,
                RecoveredOrderUpsertion.Event.OrderCompleted,
              )
            else
              List.empty
          }
          .getOrElse(List.empty)

      toRecoveredOrderUpsertion(
        recoveredExistingOrGeneratedOrderId,
        upsertion,
        sourceWithDefault,
        recoveredLocation,
        recoveredCustomerId,
        recoveredCreatorUserId,
        recoveredAssignedUserIds,
        recoveredOrderItemsWithBundleCost,
        recoveredDiscounts,
        recoveredOrderTaxRates,
        recoveredPaymentTransactions,
        recoveredRewardRedemptions,
        recoverCanDeleteOrderItems(id, upsertion, validExistingOrder.getOrElse(None)),
        recoveredDeliveryAddress,
        recoveredOnlineOrderAttribute,
        recoveredBundles,
        events,
      )
    }

  private def recoverCreatorUserId(users: Seq[UserRecord], creatorUserId: Option[UUID]): ErrorsOr[Option[UUID]] =
    creatorUserId match {
      case Some(userId) if users.exists(_.id == userId) => Multiple.successOpt(userId)
      case Some(userId)                                 => Multiple.failure(InvalidUserIds(Seq(userId)))
      case _                                            => Multiple.empty
    }

  private def recoverAssignedUserIds(
      users: Seq[UserRecord],
      assignedUserIds: Option[Seq[UUID]],
    ): ErrorsOr[Option[Seq[UUID]]] =
    assignedUserIds match {
      case None => Multiple.empty
      case Some(userIds) =>
        val existingIds = users.map(_.id) intersect userIds
        if (userIds.toSet == existingIds.toSet) Multiple.successOpt(userIds)
        else Multiple.failure(InvalidUserIds(userIds diff existingIds))
    }

  private def recoverCanDeleteOrderItems(
      id: UUID,
      upsertion: OrderUpsertion,
      existingOrder: Option[OrderRecord],
    ): Boolean = {
    val pending = {
      val currentPaymentStatus = upsertion.paymentStatus
      val previousPaymentStatus = existingOrder.flatMap(_.paymentStatus)
      Seq(Some(currentPaymentStatus), previousPaymentStatus).flatten.contains(PaymentStatus.Pending)
    }

    if (!pending) daos.orderItemDao.findByOrderId(id).map { existingItems =>
      val existingIds = existingItems.map(_.id).toSet
      val upsertionIds = upsertion.items.map(_.id).toSet
      val diffIds = existingIds.diff(upsertionIds)
      if (diffIds.nonEmpty) {
        val description = "trying to delete items from a non-pending order"
        logger.recoverLog(
          s"$description: order id $id. Recovering with non cancelling order items with ids ${diffIds.mkString(", ")}",
          upsertion,
        )
      }
    }

    pending
  }

  private def toRecoveredOrderUpsertion(
      orderId: UUID,
      upsertion: OrderUpsertion,
      sourceWithDefault: Source,
      location: Option[LocationRecord],
      customerId: Option[UUID],
      creatorUserId: Option[UUID],
      assignedUserIds: Option[Seq[UUID]],
      orderItemUpsertions: Seq[RecoveredOrderItemUpsertion],
      discounts: Seq[RecoveredItemDiscountUpsertion],
      orderTaxRateUpsertions: Seq[RecoveredOrderTaxRateUpsertion],
      paymentTransactionsUpsertions: Seq[RecoveredPaymentTransactionUpsertion],
      rewardRedemptionUpsertions: Seq[RecoveredRewardRedemptionUpsertion],
      canDeleteOrderItems: Boolean,
      deliveryAddress: Option[RecoveredDeliveryAddressUpsertion],
      onlineOrderAttribute: Option[RecoveredOnlineOrderAttributeUpsertion],
      bundles: Seq[RecoveredOrderBundle],
      events: List[RecoveredOrderUpsertion.Event],
    ): RecoveredOrderUpsertion =
    RecoveredOrderUpsertion(
      orderId = orderId,
      locationId = location.map(_.id),
      deviceId = upsertion.deviceId, // TODO - this should be validated!!! - note in the db is not a FK yet
      customerId = customerId,
      creatorUserId = creatorUserId,
      tag = upsertion.tag,
      `type` = upsertion.`type`,
      paymentType = upsertion.paymentType,
      totalAmount = upsertion.totalAmount,
      subtotalAmount = upsertion.subtotalAmount,
      discountAmount = upsertion.discountAmount,
      taxAmount = upsertion.taxAmount,
      tipAmount = upsertion.tipAmount,
      ticketDiscountAmount = upsertion.ticketDiscountAmount,
      deliveryFeeAmount = upsertion.deliveryFeeAmount,
      merchantNotes = upsertion.merchantNotes,
      paymentStatus = upsertion.paymentStatus,
      source = sourceWithDefault,
      status = upsertion.status,
      fulfillmentStatus = upsertion.fulfillmentStatus,
      isInvoice = upsertion.isInvoice,
      isFiscal = upsertion.isFiscal,
      paymentTransactions = paymentTransactionsUpsertions,
      items = orderItemUpsertions,
      discounts = discounts,
      assignedUserIds = assignedUserIds,
      taxRates = orderTaxRateUpsertions,
      rewardRedemptions = rewardRedemptionUpsertions,
      receivedAt = upsertion.receivedAt,
      receivedAtTz = upsertion.receivedAt.toLocationTimezoneWithFallback(location.map(_.timezone)),
      completedAt = upsertion.completedAt,
      completedAtTz = upsertion.completedAt.map(_.toLocationTimezoneWithFallback(location.map(_.timezone))),
      canDeleteOrderItems = canDeleteOrderItems,
      deliveryAddress = deliveryAddress,
      onlineOrderAttribute = onlineOrderAttribute,
      version = upsertion.version,
      seating = upsertion.seating,
      bundles = bundles,
      deliveryProvider = upsertion.deliveryProvider,
      deliveryProviderId = upsertion.deliveryProviderId,
      deliveryProviderNumber = upsertion.deliveryProviderNumber,
      events = events,
    )
}

final case class RecoveredOrderUpsertion(
    orderId: UUID,
    locationId: Option[UUID],
    deviceId: Option[UUID],
    creatorUserId: Option[UUID],
    customerId: Option[UUID],
    tag: ResettableString,
    `type`: OrderType,
    paymentType: Option[OrderPaymentType],
    totalAmount: BigDecimal,
    subtotalAmount: BigDecimal,
    discountAmount: Option[BigDecimal],
    taxAmount: BigDecimal,
    tipAmount: Option[BigDecimal],
    ticketDiscountAmount: Option[BigDecimal],
    deliveryFeeAmount: Option[BigDecimal],
    merchantNotes: Seq[MerchantNoteUpsertion],
    paymentStatus: PaymentStatus,
    source: Source,
    status: OrderStatus,
    fulfillmentStatus: Option[FulfillmentStatus],
    isInvoice: Boolean,
    isFiscal: Option[Boolean],
    paymentTransactions: Seq[RecoveredPaymentTransactionUpsertion],
    items: Seq[RecoveredOrderItemUpsertion],
    discounts: Seq[RecoveredItemDiscountUpsertion],
    assignedUserIds: Option[Seq[UUID]],
    taxRates: Seq[RecoveredOrderTaxRateUpsertion],
    rewardRedemptions: Seq[RecoveredRewardRedemptionUpsertion],
    receivedAt: ZonedDateTime,
    receivedAtTz: ZonedDateTime,
    completedAt: Option[ZonedDateTime],
    completedAtTz: Option[ZonedDateTime],
    canDeleteOrderItems: Boolean,
    deliveryAddress: Option[RecoveredDeliveryAddressUpsertion],
    onlineOrderAttribute: Option[RecoveredOnlineOrderAttributeUpsertion],
    version: Int,
    seating: ResettableSeating,
    bundles: Seq[RecoveredOrderBundle],
    deliveryProvider: Option[DeliveryProvider],
    deliveryProviderId: Option[String],
    deliveryProviderNumber: Option[String],
    events: List[RecoveredOrderUpsertion.Event],
  )

object RecoveredOrderUpsertion {
  sealed abstract class Event extends SerializableProduct
  object Event {
    case object OrderMarkedAsReadyForPickup extends Event
    case object OrderCompleted extends Event
  }
}
