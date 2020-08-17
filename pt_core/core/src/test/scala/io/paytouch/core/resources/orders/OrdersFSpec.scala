package io.paytouch.core.resources.orders

import java.time.ZonedDateTime
import java.util.UUID

import scala.concurrent._

import io.paytouch.core._
import io.paytouch.core.data._
import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums._
import io.paytouch.core.entities._
import io.paytouch.core.json.JsonSupport
import io.paytouch.core.utils._

abstract class OrdersFSpec extends FSpec {
  abstract class OrderResourceFSpecContext extends FSpecContext with JsonSupport with MultipleLocationFixtures {
    val articleDao = daos.articleDao
    val giftCardPassDao = daos.giftCardPassDao
    val loyaltyRewardDao = daos.loyaltyRewardDao
    val modifierOptionDao = daos.modifierOptionDao
    val modifierSetDao = daos.modifierSetDao
    val orderBundleDao = daos.orderBundleDao
    val orderDao = daos.orderDao
    val orderItemDao = daos.orderItemDao
    val orderItemDiscountDao = daos.orderItemDiscountDao
    val orderItemModifierOptionDao = daos.orderItemModifierOptionDao
    val orderItemVariantOptionDao = daos.orderItemVariantOptionDao
    val orderTaxRateDao = daos.orderTaxRateDao
    val orderItemTaxRateDao = daos.orderItemTaxRateDao
    val orderUserDao = daos.orderUserDao
    val paymentTransactionOrderItemDao = daos.paymentTransactionOrderItemDao
    val onlineOrderAttributeDao = daos.onlineOrderAttributeDao

    def assertPaymentTransaction(
        entity: PaymentTransaction,
        record: PaymentTransactionRecord,
        paymentTransactionFees: Seq[PaymentTransactionFeeRecord],
      ) = {
      entity.id ==== record.id
      entity.orderId ==== record.orderId
      entity.customerId ==== record.customerId
      entity.`type` ==== record.`type`
      entity.refundedPaymentTransactionId === record.refundedPaymentTransactionId
      entity.paymentType ==== record.paymentType
      entity.paidAt ==== record.paidAt

      assertPaymentDetails(record.paymentDetails, entity.paymentDetails)

      val dbPaymentTransactionOrderItems = paymentTransactionOrderItemDao.findByPaymentTransactionId(record.id).await
      entity.orderItemIds.toSet ==== dbPaymentTransactionOrderItems.map(_.orderItemId).toSet

      entity.fees.map(_.id) ==== paymentTransactionFees.map(_.id)
    }

    def assertPaymentDetails(record: Option[PaymentDetails], entity: Option[PaymentDetails]) = {
      if (entity.flatMap(_.giftCardPassId).isDefined) {
        val giftCardPassId = entity.flatMap(_.giftCardPassId).get
        val giftCardPassRecord = giftCardPassDao.findById(giftCardPassId).await
        entity.flatMap(_.giftCardPassLookupId) ==== giftCardPassRecord.map(_.lookupId)
      }

      record.map(_.amount) ==== entity.map(_.amount)
      record.map(_.currency) ==== entity.map(_.currency)
      record.map(_.authCode) ==== entity.map(_.authCode)
      record.map(_.maskPan) ==== entity.map(_.maskPan)
      record.map(_.cardHash) ==== entity.map(_.cardHash)
      record.map(_.cardReference) ==== entity.map(_.cardReference)
      record.map(_.cardType) ==== entity.map(_.cardType)
      record.map(_.terminalName) ==== entity.map(_.terminalName)
      record.map(_.terminalId) ==== entity.map(_.terminalId)
      record.map(_.transactionResult) ==== entity.map(_.transactionResult)
      record.map(_.transactionStatus) ==== entity.map(_.transactionStatus)
      record.map(_.transactionReference) ==== entity.map(_.transactionReference)
      record.map(_.last4Digits) ==== entity.map(_.last4Digits)
      record.map(_.paidInAmount) ==== entity.map(_.paidInAmount)
      record.map(_.paidOutAmount) ==== entity.map(_.paidOutAmount)
      record.map(_.giftCardPassId) ==== entity.map(_.giftCardPassId)
      record.map(_.giftCardPassTransactionId) ==== entity.map(_.giftCardPassTransactionId)
      record.map(_.isStandalone) ==== entity.map(_.isStandalone)
      record.map(_.customerId) ==== entity.map(_.customerId)
      record.map(_.tipAmount) ==== entity.map(_.tipAmount)
      record.map(_.preauth) ==== entity.map(_.preauth)
    }

    def assertPaymentTransactions(
        entities: Seq[PaymentTransaction],
        records: Seq[PaymentTransactionRecord],
        paymentTransactionFees: Map[PaymentTransactionRecord, Seq[PaymentTransactionFeeRecord]],
      ) = {
      entities.map(_.id).toSet ==== records.map(_.id).toSet
      entities.map { entity =>
        val record = records.find(_.id == entity.id).get
        val fees = paymentTransactionFees.getOrElse(record, Seq.empty)
        assertPaymentTransaction(entity, record, fees)
      }
    }

    def assertResponse(
        record: OrderRecord,
        entity: entities.Order,
        location: Option[LocationRecord] = None,
        customer: Option[CustomerMerchantRecord] = None,
        paymentTransactions: Option[Seq[PaymentTransactionRecord]] = None,
        paymentTransactionFees: Map[PaymentTransactionRecord, Seq[PaymentTransactionFeeRecord]] = Map.empty,
        items: Option[Seq[OrderItemRecord]] = None,
        orderRoutingStatuses: Option[OrderRoutingStatusesByType] = None,
        orderStatusPerItem: Option[Map[OrderItemRecord, Option[OrderRoutingStatus]]] = None,
        statusTransitions: Option[Seq[model.StatusTransition]] = None,
        discounts: Seq[OrderDiscountRecord] = Seq.empty,
        giftCardPassInfoPerItem: Option[Map[OrderItemRecord, GiftCardPassInfo]] = None,
        rewards: Seq[RewardRedemptionRecord] = Seq.empty,
        loyaltyPoints: Option[LoyaltyPoints] = None,
        orderDeliveryAddress: Option[OrderDeliveryAddressRecord] = None,
        onlineOrderAttribute: Option[OnlineOrderAttributeRecord] = None,
        tipsAssignments: Option[Seq[TipsAssignmentRecord]] = None,
      ) = {
      record.id ==== entity.id
      record.deviceId ==== entity.deviceId
      record.userId ==== entity.creatorUserId
      record.userId ==== entity.creatorUser.map(_.id)
      record.number ==== entity.number
      record.tag ==== entity.tag
      record.source ==== entity.source
      record.`type` ==== entity.`type`
      record.paymentType ==== entity.paymentType
      record.seating ==== entity.seating
      record.totalAmount ==== entity.total.map(_.amount)
      record.subtotalAmount ==== entity.subtotal.map(_.amount)
      record.discountAmount ==== entity.discount.map(_.amount)
      record.taxAmount ==== entity.tax.map(_.amount)
      record.tipAmount ==== entity.tip.map(_.amount)
      record.ticketDiscountAmount ==== entity.ticketDiscount.map(_.amount)
      record.deliveryFeeAmount ==== entity.deliveryFee.map(_.amount)
      record.customerNotes ==== entity.customerNotes
      record.paymentStatus.map(_.toString) ==== entity.paymentStatus.map(_.toString)
      record.status.map(_.toString) ==== entity.status.map(_.toString)
      record.fulfillmentStatus.map(_.toString) ==== entity.fulfillmentStatus.map(_.toString)
      record.isInvoice ==== entity.isInvoice
      record.receivedAt ==== entity.receivedAt
      record.completedAt ==== entity.completedAt
      record.createdAt ==== entity.createdAt
      record.updatedAt ==== entity.updatedAt

      if (orderRoutingStatuses.isDefined) orderRoutingStatuses ==== Some(entity.orderRoutingStatuses)

      if (location.isDefined) entity.location.map(_.id) ==== location.map(_.id)
      entity.customer.map(_.id) ==== customer.map(_.id)

      if (paymentTransactions.isDefined) {
        entity.paymentTransactions must beSome
        assertPaymentTransactions(entity.paymentTransactions.get, paymentTransactions.get, paymentTransactionFees)
      }

      entity.paymentTransactions.map(_.map(_.id)).getOrElse(Seq.empty) should containTheSameElementsAs(
        paymentTransactions.map(_.map(_.id)).getOrElse(Seq.empty),
      )

      if (items.isDefined) {
        entity.items must beSome
        assertOrderItems(entity.items.get, items.get, orderStatusPerItem, giftCardPassInfoPerItem)
      }

      if (statusTransitions.isDefined)
        statusTransitions.get.foreach(st => entity.statusTransitions.exists(_.id == st.id) must beTrue)

      if (!entity.status.contains(OrderStatus.Canceled))
        entity.statusTransitions.map(_.status) should containTheSameElementsAs(
          OrderWorkflow.getByOrderType(record.`type`.get, Seq.empty),
        )

      assertMerchantNotes(record.merchantNotes, entity.merchantNotes)

      assertUsers(record.id, entity.assignedUsers)

      assertOrderTaxRates(record, entity)

      assertOrderDiscounts(entity.discountDetails, discounts)

      assertRewardRedemptions(entity.rewards, rewards)

      entity.loyaltyPoints ==== loyaltyPoints

      if (orderDeliveryAddress.isDefined) {
        entity.deliveryAddress must beSome
        assertDeliveryAddress(entity.deliveryAddress.get, orderDeliveryAddress.get)
      }

      if (onlineOrderAttribute.isDefined) {
        entity.onlineOrderAttribute must beSome
        assertOnlineOrderAttribute(entity.onlineOrderAttribute.get, onlineOrderAttribute.get)
      }

      assertOrderBundles(entity.id, entity.bundles)

      if (tipsAssignments.isDefined)
        assertTipsAssignments(entity.tipsAssignments, tipsAssignments.get)
    }

    def assertOrderBundles(orderId: UUID, bundles: Seq[OrderBundle]) = {
      val bundleRecords = orderBundleDao.findByOrderId(orderId).await
      bundleRecords.size ==== bundles.map(_.orderBundleSets.map(_.orderBundleOptions)).size
      bundles.flatMap { bundle =>
        val maybeBundleRecord = bundleRecords.find(_.id == bundle.id)
        maybeBundleRecord must beSome
        val bundleRecord = maybeBundleRecord.get

        bundle.id ==== bundleRecord.id
        bundle.bundleOrderItemId ==== bundleRecord.bundleOrderItemId

        bundle.orderBundleSets.size ==== bundleRecord.orderBundleSets.size
        bundle.orderBundleSets.flatMap { orderBundleSet =>
          val maybeOrderBundleSetRecord = bundleRecord.orderBundleSets.find(_.id == orderBundleSet.id)
          maybeOrderBundleSetRecord must beSome
          val orderBundleSetRecord = maybeOrderBundleSetRecord.get

          orderBundleSetRecord.bundleSetId ==== orderBundleSetRecord.bundleSetId
          orderBundleSetRecord.name ==== orderBundleSetRecord.name

          orderBundleSet.orderBundleOptions.size ==== orderBundleSetRecord.orderBundleOptions.size
          orderBundleSet.orderBundleOptions.map { orderBundleOption =>
            val maybeOrderBundleOptionRecord = orderBundleSet.orderBundleOptions.find(_.id == orderBundleOption.id)
            maybeOrderBundleOptionRecord must beSome
            val orderBundleOptionRecord = maybeOrderBundleOptionRecord.get

            orderBundleOptionRecord.bundleOptionId ==== orderBundleOptionRecord.bundleOptionId
            orderBundleOptionRecord.articleOrderItemId ==== orderBundleOptionRecord.articleOrderItemId
            orderBundleOptionRecord.priceAdjustment ==== orderBundleOptionRecord.priceAdjustment
          }
        }
      }
    }

    def assertOrderTaxRates(record: OrderRecord, entity: entities.Order) = {
      val orderTaxRateRecords = orderTaxRateDao.findAllByOrderIds(Seq(record.id)).await
      orderTaxRateRecords.foreach { orderTaxRateRecord =>
        val orderTaxRateEntity = entity.taxRates.find(_.taxRateId == orderTaxRateRecord.taxRateId).get
        orderTaxRateEntity.taxRateId ==== orderTaxRateRecord.taxRateId
        orderTaxRateEntity.name ==== orderTaxRateRecord.name
        orderTaxRateEntity.value ==== orderTaxRateRecord.value
        orderTaxRateEntity.totalAmount ==== orderTaxRateRecord.totalAmount
      }
    }

    def assertOrderItems(
        entities: Seq[OrderItem],
        records: Seq[OrderItemRecord],
        orderStatusPerItem: Option[Map[OrderItemRecord, Option[OrderRoutingStatus]]],
        giftCardPassInfoPerItem: Option[Map[OrderItemRecord, GiftCardPassInfo]] = None,
      ) =
      entities.map { entity =>
        val record = records.find(_.id == entity.id).get
        val giftCardPassInfo = giftCardPassInfoPerItem.flatMap(_.get(record))
        assertOrderItem(entity, record, giftCardPassInfo)

        if (orderStatusPerItem.isDefined) {
          val expectedOrderRoutingStatus = orderStatusPerItem.flatMap(_.getOrElse(record, None))
          entity.orderRoutingStatus ==== expectedOrderRoutingStatus
        }
      }

    def assertOrderItem(
        entity: OrderItem,
        record: OrderItemRecord,
        giftCardPassInfo: Option[GiftCardPassInfo] = None,
      ) = {
      entity.id ==== record.id
      entity.orderId ==== record.orderId
      entity.productId ==== record.productId
      entity.productName ==== record.productName
      entity.productDescription ==== record.productDescription
      entity.productType ==== record.productType
      entity.quantity ==== record.quantity
      entity.unit ==== record.unit
      entity.paymentStatus ==== record.paymentStatus
      entity.price ==== MonetaryAmount.extract(record.priceAmount, currency)
      entity.cost ==== MonetaryAmount.extract(record.costAmount, currency)
      entity.discount ==== MonetaryAmount.extract(record.discountAmount, currency)
      entity.tax ==== MonetaryAmount.extract(record.taxAmount, currency)
      entity.basePrice ==== MonetaryAmount.extract(record.basePriceAmount, currency)
      entity.calculatedPrice ==== MonetaryAmount.extract(record.calculatedPriceAmount, currency)
      entity.totalPrice ==== MonetaryAmount.extract(record.totalPriceAmount, currency)

      val product = record.productId match {
        case None            => Future.successful(None)
        case Some(productId) => articleDao.findDeletedById(productId)
      }
      product.await.map(_.`type`) ==== entity.productType

      assertOrderItemDiscounts(record.id, entity.discounts)
      assertOrderItemModifierOptions(record.id, entity.modifierOptions)
      assertOrderItemTaxRates(record.id, entity.taxRates)
      assertOrderItemVariantOptions(record.id, entity.variantOptions)
      assertOrderItemGiftCardPasses(entity, giftCardPassInfo)
    }

    def assertOrderItemDiscounts(id: UUID, discounts: Seq[OrderItemDiscount]) = {
      def assertOrderItemDiscount(entity: OrderItemDiscount, record: OrderItemDiscountRecord) = {
        entity.id ==== record.id
        entity.discountId ==== record.discountId
        entity.title ==== record.title
        entity.`type` ==== record.`type`
        entity.amount ==== record.amount
        entity.totalAmount ==== record.totalAmount
        entity.currency ==== (if (record.`type` == DiscountType.Percentage) None else Some(currency))
      }

      val orderItemDiscounts = orderItemDiscountDao.findAllByOrderItemIds(Seq(id)).await
      orderItemDiscounts.map(_.id).toSet ==== discounts.map(_.id).toSet
      discounts.map(entity => assertOrderItemDiscount(entity, orderItemDiscounts.find(_.id == entity.id).get))
    }

    def assertOrderItemModifierOptions(id: UUID, modifierOptions: Seq[OrderItemModifierOption]) = {
      def assertOrderItemModifierOption(entity: OrderItemModifierOption, record: OrderItemModifierOptionRecord) = {
        entity.id ==== record.id
        entity.modifierOptionId ==== record.modifierOptionId
        entity.name ==== record.name
        entity.`type` ==== record.`type`
        entity.price ==== MonetaryAmount(record.priceAmount, currency)
        entity.quantity ==== record.quantity
      }

      val orderItemModifierOptions = orderItemModifierOptionDao.findAllByOrderItemIds(Seq(id)).await
      orderItemModifierOptions.map(_.id).toSet ==== modifierOptions.map(_.id).toSet
      modifierOptions.map(entity =>
        assertOrderItemModifierOption(entity, orderItemModifierOptions.find(_.id == entity.id).get),
      )
    }

    def assertOrderItemTaxRates(id: UUID, taxRates: Seq[OrderItemTaxRate]) = {
      val orderItemTaxRateRecords = orderItemTaxRateDao.findAllByOrderItemIds(Seq(id)).await
      orderItemTaxRateRecords.foreach { orderItemTaxRateRecord =>
        val orderItemTaxRateEntity = taxRates.find(_.taxRateId == orderItemTaxRateRecord.taxRateId).get
        orderItemTaxRateEntity.taxRateId ==== orderItemTaxRateRecord.taxRateId
        orderItemTaxRateEntity.name ==== orderItemTaxRateRecord.name
        orderItemTaxRateEntity.value ==== orderItemTaxRateRecord.value
        orderItemTaxRateEntity.totalAmount ==== orderItemTaxRateRecord.totalAmount
        orderItemTaxRateEntity.applyToPrice ==== orderItemTaxRateRecord.applyToPrice
        orderItemTaxRateEntity.active ==== orderItemTaxRateRecord.active
      }
    }

    def assertOrderItemVariantOptions(id: UUID, variantOptions: Seq[OrderItemVariantOption]) = {
      def assertOrderItemVariantOption(entity: OrderItemVariantOption, record: OrderItemVariantOptionRecord) = {
        entity.id ==== record.id
        entity.variantOptionId ==== record.variantOptionId
        entity.optionName ==== record.optionName
        entity.optionTypeName ==== record.optionTypeName
      }

      val orderItemVariantOptions = orderItemVariantOptionDao.findAllByOrderItemIds(Seq(id)).await
      orderItemVariantOptions.map(_.id).toSet ==== variantOptions.map(_.id).toSet
      variantOptions.map(entity =>
        assertOrderItemVariantOption(entity, orderItemVariantOptions.find(_.id == entity.id).get),
      )
    }

    def assertOrderItemGiftCardPasses(entity: OrderItem, giftCardPassInfo: Option[GiftCardPassInfo]) =
      entity.giftCardPass ==== giftCardPassInfo

    def assertMerchantNotes(records: Seq[model.MerchantNote], multipleEntities: Seq[entities.MerchantNote]) = {
      records.map(_.id) should containTheSameElementsAs(multipleEntities.map(_.id))
      records.map(_.body) should containTheSameElementsAs(multipleEntities.map(_.body))
      records.map(_.createdAt) should containTheSameElementsAs(multipleEntities.map(_.createdAt))
    }

    def assertUsers(orderId: UUID, users: Seq[UserInfo]) = {
      val orderUsers = orderUserDao.findByOrderIds(Seq(orderId)).await
      users.map(_.id).toSet ==== orderUsers.map(_.userId).toSet
    }

    def assertOrderDiscounts(entities: Seq[OrderDiscount], records: Seq[OrderDiscountRecord]) = {
      entities.size ==== records.size
      entities.foreach { entity =>
        val record = records.find(_.id == entity.id).get
        entity.id ==== record.id
        entity.orderId ==== record.orderId
        entity.discountId ==== record.discountId
        entity.title ==== record.title
        entity.`type` ==== record.`type`
        entity.amount ==== record.amount
        entity.totalAmount ==== record.totalAmount
        entity.currency ==== (if (record.`type` == DiscountType.Percentage) None else Some(currency))
      }
    }

    def assertRewardRedemptions(entities: Seq[RewardRedemption], records: Seq[RewardRedemptionRecord]) = {
      entities.size ==== records.size
      entities.foreach { entity =>
        val record = records.find(_.id == entity.id).get
        entity.id ==== record.id
        entity.loyaltyRewardId ==== record.loyaltyRewardId
        entity.loyaltyMembershipId ==== record.loyaltyMembershipId
        entity.points ==== record.points
        entity.status ==== record.status
        entity.loyaltyRewardType ==== record.loyaltyRewardType
        entity.orderId ==== record.orderId
        entity.objectId ==== record.objectId
        entity.objectType ==== record.objectType
        entity.loyaltyMembership.isDefined must beFalse
      }
    }

    def assertDeliveryAddress(entity: DeliveryAddress, record: OrderDeliveryAddressRecord) = {
      entity.id ==== record.id
      entity.firstName ==== record.firstName
      entity.lastName ==== record.lastName
      entity.address.line1 ==== record.addressLine1
      entity.address.line2 ==== record.addressLine2
      entity.address.city ==== record.city
      entity.address.state ==== record.state
      entity.address.country ==== record.country
      entity.address.postalCode ==== record.postalCode
      entity.drivingDistanceInMeters ==== record.drivingDistanceInMeters
      entity.estimatedDrivingTimeInMins ==== record.estimatedDrivingTimeInMins
    }

    def assertOnlineOrderAttribute(entity: OnlineOrderAttribute, record: OnlineOrderAttributeRecord): Unit = {
      entity.id ==== record.id
      entity.acceptanceStatus ==== record.acceptanceStatus
      entity.rejectionReason ==== record.rejectionReason
      entity.prepareByTime ==== record.prepareByTime
      entity.estimatedPrepTimeInMins ==== record.estimatedPrepTimeInMins
      entity.acceptedAt ==== record.acceptedAt
      entity.rejectedAt ==== record.rejectedAt
      entity.estimatedReadyAt ==== record.estimatedReadyAt
      entity.estimatedDeliveredAt ==== record.estimatedDeliveredAt
    }

    def assertTipsAssignments(entities: Seq[TipsAssignment], records: Seq[TipsAssignmentRecord]) = {
      records.map(_.id) should containTheSameElementsAs(entities.map(_.id))

      records.map { record =>
        val maybeTipAssignment = entities.find(_.id == record.id)
        maybeTipAssignment must beSome

        val entity = maybeTipAssignment.get
        entity.locationId ==== record.locationId
        entity.userId ==== record.userId
        entity.orderId ==== record.orderId
        entity.amount.amount ==== record.amount
        entity.handledVia ==== record.handledVia
        entity.handledViaCashDrawerActivityId ==== record.handledViaCashDrawerActivityId
      }
    }

    def assertFullyExpandedResponse(orderRecord: OrderRecord, orderEntity: entities.Order) = {
      val loyaltyPoints = LoyaltyPoints.potential(0)
      assertResponse(orderRecord, orderEntity, loyaltyPoints = Some(loyaltyPoints))
    }
  }
}
