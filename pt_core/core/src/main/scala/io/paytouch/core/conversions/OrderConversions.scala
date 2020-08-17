package io.paytouch.core.conversions

import java.time.{ ZoneId, ZonedDateTime }
import java.util.UUID

import io.paytouch.core.{ OrderWorkflow, RichZoneDateTime }
import io.paytouch.core.calculations.OrderRoutingStatusCalculation
import io.paytouch.core.data.model.enums.{ KitchenType, OrderRoutingStatus, OrderStatus }
import io.paytouch.core.data.model.{
  CustomerNote,
  OrderRecord,
  TicketRecord,
  MerchantNote => MerchantNoteModel,
  StatusTransition => StatusTransitionModel,
}
import io.paytouch.core.entities.{
  OrderRoutingStatusesByType,
  MerchantNote => MerchantNoteEntity,
  Order => OrderEntity,
  StatusTransition => StatusTransitionEntity,
  _,
}

trait OrderConversions extends EntityConversion[OrderRecord, OrderEntity] with OrderRoutingStatusCalculation {
  def fromRecordToEntity(record: OrderRecord)(implicit user: UserContext): OrderEntity =
    fromRecordAndOptionsToEntity(
      record,
      creatorUser = None,
      customer = None,
      deliveryAddress = None,
      onlineOrderAttribute = None,
      discounts = Seq.empty,
      items = None,
      location = None,
      loyaltyPoints = None,
      paymentTransactions = None,
      rewardRedemptions = Seq.empty,
      statusTransitions = Seq.empty,
      taxRates = Seq.empty,
      orderRoutingStatuses = Map.empty,
      orderRoutingKitchenStatuses = Map.empty,
      users = Seq.empty,
      usersForMerchantNotes = Seq.empty,
      orderBundles = Seq.empty,
      kitchens = Map.empty,
      ticketInfos = None,
      tipsAssignments = Seq.empty,
    )

  def fromRecordsAndOptionsToEntities(
      records: Seq[OrderRecord],
      customersPerOrder: Map[OrderRecord, CustomerMerchant],
      deliveryAddressesPerOrder: Map[OrderRecord, DeliveryAddress],
      onlineOrderAttributesPerOrder: Map[OrderRecord, OnlineOrderAttribute],
      discountsPerOrder: Map[OrderRecord, Seq[OrderDiscount]],
      locationsPerOrder: Map[OrderRecord, Location],
      loyaltyPointsPerOrder: Option[Map[OrderRecord, LoyaltyPoints]],
      paymentTransactionsPerOrder: Option[Map[UUID, Seq[PaymentTransaction]]],
      rewardRedemptionsPerOrder: Map[OrderRecord, Seq[RewardRedemption]],
      itemsPerOrder: Option[Map[UUID, Seq[OrderItem]]],
      orderBundlesPerOrder: Map[OrderRecord, Seq[OrderBundle]],
      assignedUsersPerOrder: Map[OrderRecord, Seq[UserInfo]],
      relatedUsers: Seq[UserInfo],
      taxRatesPerOrder: Map[OrderRecord, Seq[OrderTaxRate]],
      ticketsPerOrder: Map[OrderRecord, Seq[TicketRecord]],
      ticketInfosPerOrder: Option[Map[OrderRecord, Seq[TicketInfo]]],
      kitchens: Map[UUID, Kitchen],
      tipsAssignmentsPerOrder: Map[OrderRecord, Seq[TipsAssignment]],
    )(implicit
      user: UserContext,
    ): Seq[OrderEntity] =
    records.map { record =>
      val location = locationsPerOrder.get(record)
      val customer = customersPerOrder.get(record)
      val deliveryAdrress = deliveryAddressesPerOrder.get(record)
      val orderBundles = orderBundlesPerOrder.getOrElse(record, Seq.empty)
      val onlineOrderAttribute = onlineOrderAttributesPerOrder.get(record)
      val discounts = discountsPerOrder.getOrElse(record, Seq.empty)
      val creatorUser = relatedUsers.find(u => record.userId.contains(u.id))
      val loyaltyPoints = loyaltyPointsPerOrder.flatMap(_.get(record))
      val paymentTransactions = paymentTransactionsPerOrder.map(_.getOrElse(record.id, Seq.empty))
      val rewardRedemptions = rewardRedemptionsPerOrder.getOrElse(record, Seq.empty)
      val items = itemsPerOrder.map(_.getOrElse(record.id, Seq.empty))
      val assignedUsers = assignedUsersPerOrder.getOrElse(record, Seq.empty)
      val orderWorkflow =
        record.`type`.map(tpe => OrderWorkflow.getByOrderType(tpe, kitchens.values.toSeq)).getOrElse(Seq.empty)
      val statusTransitions =
        mergeOrderWorkflowWithStatusTransitions(orderWorkflow, record.statusTransitions)
      val taxRates = taxRatesPerOrder.getOrElse(record, Seq.empty)
      val ticketInfo = ticketInfosPerOrder.flatMap(_.get(record))

      val tickets = ticketsPerOrder.getOrElse(record, Seq.empty)
      val orderRoutingStatuses = inferOrderRoutingStatusesByType(tickets, kitchens)
      val orderRoutingKitchenStatuses = inferOrderRoutingStatusesByKitchen(tickets, kitchens)
      val tipsAssignments = tipsAssignmentsPerOrder.getOrElse(record, Seq.empty)

      fromRecordAndOptionsToEntity(
        record,
        creatorUser,
        customer,
        deliveryAdrress,
        onlineOrderAttribute,
        location,
        loyaltyPoints,
        paymentTransactions,
        items,
        orderBundles,
        discounts,
        rewardRedemptions,
        statusTransitions,
        assignedUsers,
        relatedUsers,
        taxRates,
        ticketInfo,
        orderRoutingStatuses,
        orderRoutingKitchenStatuses,
        kitchens,
        tipsAssignments,
      )
    }

  def fromRecordAndOptionsToEntity(
      record: OrderRecord,
      creatorUser: Option[UserInfo],
      customer: Option[CustomerMerchant],
      deliveryAddress: Option[DeliveryAddress],
      onlineOrderAttribute: Option[OnlineOrderAttribute],
      location: Option[Location],
      loyaltyPoints: Option[LoyaltyPoints],
      paymentTransactions: Option[Seq[PaymentTransaction]],
      items: Option[Seq[OrderItem]],
      orderBundles: Seq[OrderBundle],
      discounts: Seq[OrderDiscount],
      rewardRedemptions: Seq[RewardRedemption],
      statusTransitions: Seq[StatusTransitionEntity],
      users: Seq[UserInfo],
      usersForMerchantNotes: Seq[UserInfo],
      taxRates: Seq[OrderTaxRate],
      ticketInfos: Option[Seq[TicketInfo]],
      orderRoutingStatuses: OrderRoutingStatusesByType,
      orderRoutingKitchenStatuses: OrderRoutingStatusesByKitchen,
      kitchens: Map[UUID, Kitchen],
      tipsAssignments: Seq[TipsAssignment],
    )(implicit
      user: UserContext,
    ): OrderEntity =
    OrderEntity(
      id = record.id,
      location = location,
      deviceId = record.deviceId,
      creatorUserId = record.userId,
      creatorUser = creatorUser,
      customer = customer,
      number = record.number,
      tag = record.tag,
      source = record.source,
      `type` = record.`type`,
      paymentType = record.paymentType,
      total = MonetaryAmount.extract(record.totalAmount),
      subtotal = MonetaryAmount.extract(record.subtotalAmount),
      discount = MonetaryAmount.extract(record.discountAmount),
      tax = MonetaryAmount.extract(record.taxAmount),
      tip = MonetaryAmount.extract(record.tipAmount),
      ticketDiscount = MonetaryAmount.extract(record.ticketDiscountAmount),
      deliveryFee = MonetaryAmount.extract(record.deliveryFeeAmount),
      taxRates = taxRates,
      customerNotes = fromCustomerNotesRecordsToEntities(record.customerNotes, location),
      merchantNotes = fromMerchantNoteRecordsToEntities(record.merchantNotes, usersForMerchantNotes, location),
      paymentStatus = record.paymentStatus,
      status = record.status,
      fulfillmentStatus = record.fulfillmentStatus,
      orderRoutingStatuses = orderRoutingStatuses,
      orderRoutingKitchenStatuses = orderRoutingKitchenStatuses,
      statusTransitions = statusTransitions,
      isInvoice = record.isInvoice,
      isFiscal = record.isFiscal,
      version = record.version,
      paymentTransactions = paymentTransactions,
      items = items,
      assignedUsers = users,
      discountDetails = discounts,
      rewards = rewardRedemptions,
      loyaltyPoints = loyaltyPoints,
      deliveryAddress = deliveryAddress,
      onlineOrderAttribute = onlineOrderAttribute,
      bundles = orderBundles,
      tipsAssignments = tipsAssignments,
      tickets = ticketInfos,
      seating = record.seating,
      deliveryProvider = record.deliveryProvider,
      deliveryProviderId = record.deliveryProviderId,
      deliveryProviderNumber = record.deliveryProviderNumber,
      receivedAt = record.receivedAt,
      completedAt = record.completedAt,
      createdAt = record.createdAt,
      updatedAt = record.updatedAt,
    )

  def mergeOrderWorkflowWithStatusTransitions(
      workflow: OrderWorkflow,
      statusTransitions: Seq[StatusTransitionModel],
    ): Seq[StatusTransitionEntity] = {
    val path = {
      val statuses = statusTransitions.map(_.status)
      if (statuses.contains(OrderStatus.Canceled))
        statuses
      else
        workflow
    }

    path.map { orderStatus =>
      val existingStatusTransition = statusTransitions.find(_.status == orderStatus)
      toStatusTransitionEntity(existingStatusTransition, orderStatus)
    }
  }

  private def toStatusTransitionEntity(
      existingTransaction: Option[StatusTransitionModel],
      orderStatus: OrderStatus,
    ): StatusTransitionEntity =
    StatusTransitionEntity(
      id = existingTransaction.map(_.id).getOrElse(UUID.randomUUID),
      status = orderStatus,
      createdAt = existingTransaction.map(_.createdAt),
    )

  def fromMerchantNoteRecordsToEntities(
      merchantNotes: Seq[MerchantNoteModel],
      users: Seq[UserInfo],
      location: Option[Location],
    ): Seq[MerchantNoteEntity] =
    merchantNotes.map { merchantNote =>
      val user = users.find(_.id == merchantNote.userId)
      fromMerchantNoteRecordToEntities(merchantNote, user, location)
    }

  def fromMerchantNoteRecordToEntities(
      merchantNote: MerchantNoteModel,
      user: Option[UserInfo],
      location: Option[Location],
    ): MerchantNoteEntity =
    MerchantNoteEntity(
      id = merchantNote.id,
      user = user,
      body = merchantNote.body,
      createdAt = merchantNote.createdAt.toLocationTimezoneWithFallback(location.map(_.timezone)),
    )

  def fromCustomerNotesRecordsToEntities(
      customerNotes: Seq[CustomerNote],
      location: Option[Location],
    ): Seq[CustomerNote] =
    customerNotes.map(fromCustomerNoteRecordsToEntities(_, location))

  def fromCustomerNoteRecordsToEntities(customerNote: CustomerNote, location: Option[Location]): CustomerNote =
    customerNote.copy(createdAt = customerNote.createdAt.toLocationTimezoneWithFallback(location.map(_.timezone)))

  private def inferOrderRoutingStatusesByType(
      tickets: Seq[TicketRecord],
      kitchens: Map[UUID, Kitchen],
    ): OrderRoutingStatusesByType = {
    val values = KitchenType.values.map { kitchenType =>
      val kitchensByType = kitchens.values.filter(_.`type` == kitchenType)
      val kitchensByTypeIds = kitchensByType.map(_.id).toSeq
      val ticketsInTheseKitchens = tickets.filter(ticket => kitchensByTypeIds.contains(ticket.routeToKitchenId))
      (kitchenType, inferOrderRoutingStatus(ticketsInTheseKitchens))
    }

    OrderRoutingStatusesByType(values: _*)
  }

  private def inferOrderRoutingStatusesByKitchen(
      tickets: Seq[TicketRecord],
      kitchens: Map[UUID, Kitchen],
    ): OrderRoutingStatusesByKitchen =
    tickets.groupBy(_.routeToKitchenId).map {
      case (kitchenId, _tickets) => (kitchenId, inferOrderRoutingStatus(_tickets))
    }
}
