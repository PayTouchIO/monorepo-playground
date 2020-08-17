package io.paytouch.core.resources.orders

import java.time.ZoneId
import java.util.UUID

import cats.implicits._

import com.typesafe.scalalogging.LazyLogging

import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums._
import io.paytouch.core.entities.{ Order => OrderEntity, _ }
import io.paytouch.core.entities.enums.CustomerSource
import io.paytouch.core.resources.customers.CustomersFSpec
import io.paytouch.core.RichZoneDateTime
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, _ }

abstract class OrdersSyncAssertsFSpec extends OrdersFSpec with CustomersFSpec with LazyLogging {
  abstract class OrdersSyncFSpecContext
      extends OrderResourceFSpecContext
         with CustomerAssertions
         with LocalDateAssertions {
    val orderDiscountDao = daos.orderDiscountDao
    val paymentTransactionDao = daos.paymentTransactionDao
    val paymentTransactionFeeDao = daos.paymentTransactionFeeDao
    val customerLocationDao = daos.customerLocationDao
    val ticketDao = daos.ticketDao
    val stockDao = daos.stockDao
    val locationDao = daos.locationDao
    val productQuantityHistoryDao = daos.productQuantityHistoryDao
    val rewardRedemptionDao = daos.rewardRedemptionDao

    def assertAssignedUserIds(
        orderId: UUID,
        userIds: Seq[UUID],
        orderEntity: OrderEntity,
      ) =
      orderEntity.assignedUsers.map(_.id) should containTheSameElementsAs(userIds)

    def assertUpsertion(
        orderEntity: OrderEntity,
        upsertion: OrderUpsertion,
        customerId: Option[UUID] = None,
        number: Option[String] = None,
        withEmptyLocationId: Boolean = false,
        withEmptyUserId: Boolean = false,
      ) = {
      val orderId = orderEntity.id
      val order = orderDao.findOpenById(orderId).await.get
      val maybeLocation = order.locationId.flatMap(locationDao.findDeletedById(_).await)
      order.merchantId ==== merchant.id
      if (withEmptyLocationId) order.locationId ==== None else order.locationId ==== Some(upsertion.locationId)
      if (upsertion.deviceId.isDefined) order.deviceId ==== upsertion.deviceId
      if (withEmptyUserId) order.userId ==== None else order.userId ==== upsertion.creatorUserId
      if (number.isDefined) order.number ==== number
      order.source ==== upsertion.source
      order.`type` ==== Some(upsertion.`type`)
      if (upsertion.paymentType.isDefined) order.paymentType ==== upsertion.paymentType
      order.totalAmount ==== Some(upsertion.totalAmount)
      order.subtotalAmount ==== Some(upsertion.subtotalAmount)
      order.taxAmount ==== Some(upsertion.taxAmount)
      if (customerId.isDefined) {
        order.customerId ==== customerId
        orderEntity.customer must beSome
        orderEntity.customer.flatMap(_.avgTips) must beSome
        orderEntity.customer.flatMap(_.loyaltyMemberships) must beSome
        orderEntity.customer.flatMap(_.totalSpend) must beSome
        orderEntity.customer.flatMap(_.totalVisits) must beSome
      }
      if (upsertion.discountAmount.isDefined) order.discountAmount ==== upsertion.discountAmount
      if (upsertion.tipAmount.isDefined) order.tipAmount ==== upsertion.tipAmount
      if (upsertion.ticketDiscountAmount.isDefined) order.ticketDiscountAmount ==== upsertion.ticketDiscountAmount
      if (upsertion.deliveryFeeAmount.isDefined) order.deliveryFeeAmount ==== upsertion.deliveryFeeAmount
      order.merchantNotes.map(_.id) should containTheSameElementsAs(upsertion.merchantNotes.map(_.id))
      order.paymentStatus ==== Some(upsertion.paymentStatus)
      if (upsertion.fulfillmentStatus.isDefined) order.fulfillmentStatus ==== upsertion.fulfillmentStatus
      if (upsertion.assignedUserIds.isDefined)
        assertAssignedUserIds(orderId, upsertion.assignedUserIds.get, orderEntity)

      order.isInvoice ==== upsertion.isInvoice
      order.receivedAt ==== upsertion.receivedAt.asZulu
      order.receivedAtTz ==== order.receivedAt.toLocationTimezoneWithFallback(maybeLocation.map(_.timezone)).asZulu
      order.completedAt ==== upsertion.completedAt.map(_.asZulu)
      order.completedAtTz ==== upsertion
        .completedAt
        .map(_.toLocationTimezoneWithFallback(maybeLocation.map(_.timezone)).asZulu)

      upsertion.items.foreach(assertOrderItemUpsertion(orderId, _))

      assertPaymentTransactionsUpsertion(orderId, customerId, upsertion.paymentTransactions)

      assertOrderDiscountUpsertions(orderId, upsertion.discounts)

      assertLookupIdOnPaymentTransactions(orderEntity)

      assertRewardRedemptions(orderId, upsertion.rewards)

      if (upsertion.deliveryAddress.isDefined)
        assertDeliveryAddress(upsertion.deliveryAddress.get, orderEntity.deliveryAddress.get)

      if (upsertion.onlineOrderAttribute.isDefined)
        assertOnlineOrderAttribute(upsertion, orderEntity)

      val upsertionBundlesAsEntities = upsertion.bundles.map { orderBundle =>
        OrderBundle(
          id = orderBundle.id,
          bundleOrderItemId = orderBundle.bundleOrderItemId,
          orderBundleSets = orderBundle.orderBundleSets.map { orderBundleSet =>
            OrderBundleSet(
              id = orderBundleSet.id,
              bundleSetId = Some(orderBundleSet.bundleSetId),
              name = orderBundleSet.name,
              position = orderBundleSet.position,
              orderBundleOptions = orderBundleSet.orderBundleOptions.map { orderBundleOption =>
                OrderBundleOption(
                  id = orderBundleOption.id,
                  bundleOptionId = Some(orderBundleOption.bundleOptionId),
                  articleOrderItemId = Some(orderBundleOption.articleOrderItemId),
                  position = orderBundleOption.position,
                  priceAdjustment = orderBundleOption.priceAdjustment,
                )
              },
            )
          },
        )
      }
      assertOrderBundles(orderId, upsertionBundlesAsEntities)

      order.deliveryProvider ==== upsertion.deliveryProvider
      order.deliveryProviderId ==== upsertion.deliveryProviderId
      order.deliveryProviderNumber ==== upsertion.deliveryProviderNumber
    }

    def assertSeating(order: OrderEntity, maybeSeating: Option[Seating]) =
      if (maybeSeating.isDefined) {
        order.seating must beSome
        val upsertionSeating = maybeSeating.get
        val orderSeating = order.seating.get

        orderSeating.id ==== upsertionSeating.id
        orderSeating.tableId ==== upsertionSeating.tableId
        orderSeating.tableName ==== upsertionSeating.tableName
        orderSeating.tableShortName ==== upsertionSeating.tableShortName
        orderSeating.tableFloorPlanName ==== upsertionSeating.tableFloorPlanName
        orderSeating.tableSectionName ==== upsertionSeating.tableSectionName
        orderSeating.reservationId ==== upsertionSeating.reservationId
        orderSeating.guestName ==== upsertionSeating.guestName
        orderSeating.guestsCount ==== upsertionSeating.guestsCount
        orderSeating.createdAt ==== upsertionSeating.createdAt.asZulu
        orderSeating.updatedAt ==== upsertionSeating.updatedAt.asZulu
      }
      else
        order.seating ==== None

    def assertLookupIdOnPaymentTransactions(orderEntity: OrderEntity) = {
      val paymentDetails = orderEntity.paymentTransactions.getOrElse(Seq.empty).flatMap(_.paymentDetails)
      paymentDetails.map { details =>
        details.giftCardPassId.map { id =>
          val passRecord = giftCardPassDao.findById(id).await
          details.giftCardPassLookupId ==== passRecord.map(_.lookupId)
        }
      }
    }

    def assertOrderTaxRateUpsertions(orderId: UUID, taxRateUpsertions: Seq[OrderTaxRateUpsertion]) = {
      val orderTaxRates = orderTaxRateDao.findAllByOrderIds(Seq(orderId)).await
      taxRateUpsertions.map { orderTaxRateUpsertion =>
        val maybeOrderTaxRate =
          orderTaxRates.find(_.taxRateId.contains(orderTaxRateUpsertion.taxRateId))
        maybeOrderTaxRate must beSome
        val orderTaxRate = maybeOrderTaxRate.get
        orderTaxRateUpsertion.name ==== orderTaxRate.name
        orderTaxRateUpsertion.value ==== orderTaxRate.value
        orderTaxRateUpsertion.totalAmount ==== orderTaxRate.totalAmount
      }
    }

    def assertOrderItemUpsertion(orderId: UUID, upsertion: OrderItemUpsertion) = {
      val record = orderItemDao.findById(upsertion.id).await.get
      record.orderId ==== orderId
      if (upsertion.productId.isDefined) upsertion.productId ==== record.productId
      if (upsertion.productName.isDefined) upsertion.productName ==== record.productName
      if (upsertion.productDescription.isDefined) upsertion.productDescription ==== record.productDescription
      if (upsertion.productType.isDefined) upsertion.productType ==== record.productType
      if (upsertion.quantity.isDefined) upsertion.quantity ==== record.quantity
      if (upsertion.paymentStatus.isDefined) upsertion.paymentStatus ==== record.paymentStatus

      if (upsertion.priceAmount.isDefined) upsertion.priceAmount ==== record.priceAmount
      if (upsertion.costAmount.isDefined && upsertion.costAmount.get > 0) upsertion.costAmount ==== record.costAmount
      if (upsertion.discountAmount.isDefined) upsertion.discountAmount ==== record.discountAmount
      if (upsertion.taxAmount.isDefined) upsertion.taxAmount ==== record.taxAmount
      if (upsertion.basePriceAmount.isDefined) upsertion.basePriceAmount ==== record.basePriceAmount
      if (upsertion.totalPriceAmount.isDefined) upsertion.totalPriceAmount ==== record.totalPriceAmount
      if (upsertion.notes.isDefined) upsertion.notes ==== record.notes

      if (upsertion.calculatedPriceAmount.isDefined) upsertion.calculatedPriceAmount ==== record.calculatedPriceAmount

      assertOrderItemDiscountUpsertions(upsertion.id, upsertion.discounts)
      assertOrderItemModifierOptionUpsertions(upsertion.id, upsertion.modifierOptions)
      assertOrderItemTaxRateUpsertions(upsertion.id, upsertion.taxRates)
      assertOrderItemVariantOptionUpsertions(upsertion.id, upsertion.variantOptions)
    }

    def assertOrderItemDiscountUpsertions(orderItemId: UUID, upsertions: Seq[ItemDiscountUpsertion]) = {
      val orderItemDiscounts = orderItemDiscountDao.findAllByOrderItemIds(Seq(orderItemId)).await
      upsertions.map { upsertion =>
        val maybeOrderItemDiscount = orderItemDiscounts.find(_.discountId == upsertion.discountId)
        maybeOrderItemDiscount must beSome
        val orderItemDiscount = maybeOrderItemDiscount.get
        upsertion.title ==== orderItemDiscount.title
        upsertion.`type` ==== orderItemDiscount.`type`
        upsertion.amount ==== orderItemDiscount.amount
        if (upsertion.totalAmount.isDefined) upsertion.totalAmount ==== orderItemDiscount.totalAmount
      }
    }

    def assertOrderItemModifierOptionUpsertions(
        orderItemId: UUID,
        upsertions: Seq[OrderItemModifierOptionUpsertion],
      ) = {
      val orderItemModifierOptions = orderItemModifierOptionDao.findAllByOrderItemIds(Seq(orderItemId)).await
      val modifierOptionIds = orderItemModifierOptions.flatMap(_.modifierOptionId)
      val modifierOptions = modifierOptionDao.findByIds(modifierOptionIds).await
      val modifierSets = modifierSetDao.findByIds(modifierOptions.map(_.modifierSetId)).await
      upsertions.map { upsertion =>
        if (upsertion.modifierOptionId.isDefined) {
          val modifierOptionId = upsertion.modifierOptionId.get
          val maybeOrderItemModifierOption =
            orderItemModifierOptions.find(_.modifierOptionId.contains(modifierOptionId))
          maybeOrderItemModifierOption must beSome
          val orderItemModifierOption = maybeOrderItemModifierOption.get
          upsertion.name ==== orderItemModifierOption.name
          upsertion.`type` ==== orderItemModifierOption.`type`
          upsertion.price ==== orderItemModifierOption.priceAmount
          upsertion.quantity ==== orderItemModifierOption.quantity

          val expectedModifierSetName = for {
            modifierOption <- modifierOptions.find(_.id == modifierOptionId)
            modifierSet <- modifierSets.find(_.id == modifierOption.modifierSetId)
          } yield modifierSet.name
          orderItemModifierOption.modifierSetName ==== expectedModifierSetName
        }
      }
    }

    def assertOrderItemTaxRateUpsertions(orderItemId: UUID, upsertions: Seq[OrderItemTaxRateUpsertion]) = {
      val orderItemTaxRates = orderItemTaxRateDao.findAllByOrderItemIds(Seq(orderItemId)).await
      upsertions.map { upsertion =>
        if (upsertion.taxRateId.isDefined) {
          val taxRateId = upsertion.taxRateId.get
          val maybeOrderItemTaxRate = orderItemTaxRates.find(_.taxRateId.contains(taxRateId))
          maybeOrderItemTaxRate must beSome
          val orderItemTaxRate = maybeOrderItemTaxRate.get
          upsertion.name ==== orderItemTaxRate.name
          upsertion.value ==== orderItemTaxRate.value
          if (upsertion.totalAmount.isDefined) upsertion.totalAmount ==== orderItemTaxRate.totalAmount
          upsertion.applyToPrice ==== orderItemTaxRate.applyToPrice
          upsertion.active ==== orderItemTaxRate.active
        }
      }
    }

    def assertOrderItemVariantOptionUpsertions(orderItemId: UUID, upsertions: Seq[OrderItemVariantOptionUpsertion]) = {
      val orderItemVariantOptions = orderItemVariantOptionDao.findAllByOrderItemIds(Seq(orderItemId)).await
      upsertions.map { upsertion =>
        if (upsertion.variantOptionId.isDefined) {
          val variantOptionId = upsertion.variantOptionId.get
          val maybeOrderItemVariantOption = orderItemVariantOptions.find(_.variantOptionId.contains(variantOptionId))
          maybeOrderItemVariantOption must beSome
          val orderItemVariantOption = maybeOrderItemVariantOption.get
          upsertion.optionName ==== Some(orderItemVariantOption.optionName)
          upsertion.optionTypeName ==== Some(orderItemVariantOption.optionTypeName)
        }
      }
    }

    def assertPaymentTransactionsUpsertion(
        orderId: UUID,
        customerId: Option[UUID],
        paymentTransactions: Seq[PaymentTransactionUpsertion],
      ) = {
      val paymentTransactionIds = paymentTransactions.map(_.id)
      val dbPaymentTransactions = paymentTransactionDao.findByIds(paymentTransactionIds).await
      dbPaymentTransactions.map(_.id) should containTheSameElementsAs(paymentTransactionIds)

      dbPaymentTransactions.foreach { dbPaymentTransaction =>
        val paymentTransaction = paymentTransactions.find(_.id == dbPaymentTransaction.id).get
        dbPaymentTransaction.orderId ==== orderId
        dbPaymentTransaction.customerId ==== customerId
        dbPaymentTransaction.`type` ==== paymentTransaction.`type`
        dbPaymentTransaction.paymentType ==== paymentTransaction.paymentType
        dbPaymentTransaction.paymentDetails ==== paymentTransaction.paymentDetails
        paymentTransaction.paymentProcessorV2.map(_ ==== dbPaymentTransaction.paymentProcessor)

        val dbPaymentTransactionOrderItems =
          paymentTransactionOrderItemDao.findByPaymentTransactionId(paymentTransaction.id).await
        paymentTransaction.orderItemIds.toSet ==== dbPaymentTransactionOrderItems.map(_.orderItemId).toSet

        assertPaymentTransactionFees(dbPaymentTransaction.id, paymentTransaction.fees)
      }
    }

    def assertPaymentTransactionFees(paymentTransactionId: UUID, feeUpsertions: Seq[PaymentTransactionFeeUpsertion]) = {
      val dbFees = paymentTransactionFeeDao.findByPaymentTransactionIds(Seq(paymentTransactionId)).await

      dbFees.length ==== feeUpsertions.length
      dbFees.foreach { dbFee =>
        val feeUpsertion = feeUpsertions.find(_.id == dbFee.id).get
        dbFee.name ==== feeUpsertion.name
        dbFee.`type` ==== feeUpsertion.`type`
        dbFee.amount ==== feeUpsertion.amount
      }
    }

    def assertCustomerLocationUpsertion(
        customerId: UUID,
        locationId: UUID,
        totalVisits: Int,
        totalSpend: BigDecimal,
      ) = {

      def assertCustomerLocation(customerLocation: CustomerLocationRecord) = {
        customerLocation.totalVisits ==== totalVisits
        customerLocation.totalSpendAmount ==== totalSpend
      }

      val customerLocation = customerLocationDao.findOneByCustomerIdAndLocationId(customerId, locationId).await.get
      assertCustomerLocation(customerLocation)
    }

    def assertCustomerEarnedLoyaltyPoints(
        customerId: UUID,
        loyaltyProgram: LoyaltyProgramRecord,
        expectedPoints: Int,
      ) = {
      val program =
        loyaltyMembershipDao.findByCustomerIdAndLoyaltyProgramId(merchant.id, customerId, loyaltyProgram.id).await
      program should beSome
      program.get.points ==== expectedPoints
    }

    def assertOrderDiscountUpsertions(orderId: UUID, upsertions: Seq[ItemDiscountUpsertion]) = {
      val orderDiscounts = orderDiscountDao.findByOrderIds(Seq(orderId)).await
      upsertions.map { upsertion =>
        val maybeOrderDiscount = orderDiscounts.find(_.discountId == upsertion.discountId)
        maybeOrderDiscount must beSome
        val orderDiscount = maybeOrderDiscount.get
        upsertion.title ==== orderDiscount.title
        upsertion.`type` ==== orderDiscount.`type`
        upsertion.amount ==== orderDiscount.amount
        if (upsertion.totalAmount.isDefined) upsertion.totalAmount ==== orderDiscount.totalAmount
      }
    }

    def assertGiftCardPassCreation(
        orderItemUpsertion: OrderItemUpsertion,
        giftCard: GiftCardRecord,
        isCustomAmount: Boolean,
        recipientEmail: Option[String] = None,
      ) = {
      val maybePass = giftCardPassDao.findByOrderItemId(orderItemUpsertion.id).await
      maybePass must beSome
      val pass = maybePass.get
      pass.merchantId ==== merchant.id
      pass.giftCardId ==== giftCard.id
      orderItemUpsertion.id ==== pass.orderItemId
      orderItemUpsertion.priceAmount ==== pass.originalAmount.some
      orderItemUpsertion.priceAmount ==== pass.balanceAmount.some
      pass.iosPassPublicUrl ==== None
      pass.androidPassPublicUrl ==== None
      pass.passInstalledAt ==== None

      pass
        .recipientEmail
        .map(_ ==== recipientEmail.orElse(orderItemUpsertion.giftCardPassRecipientEmail).get)

      pass.isCustomAmount ==== isCustomAmount
    }

    def assertNoPassAttached(orderItemId: UUID) = giftCardPassDao.findByOrderItemId(orderItemId).await must beNone
    def assertPassAttached(orderItemId: UUID) = giftCardPassDao.findByOrderItemId(orderItemId).await must beSome

    def assertRewardRedemptions(orderId: UUID, rewards: Seq[RewardRedemptionSync]) = {
      val rewardRedemptionIds = rewards.map(_.rewardRedemptionId)
      val rewardRedemptionRecords = rewardRedemptionDao.findByIds(rewardRedemptionIds).await
      rewardRedemptionRecords.lengthCompare(rewards.size)
      rewardRedemptionRecords.map { rewardRedemptionRecord =>
        rewards
          .find(_.rewardRedemptionId == rewardRedemptionRecord.id)
          .map(assertRewardRedemption(orderId, _, rewardRedemptionRecord))
      }
    }

    def assertRewardRedemption(
        orderId: UUID,
        rewardRedemptionSync: RewardRedemptionSync,
        rewardRedemptionRecord: RewardRedemptionRecord,
      ) = {
      rewardRedemptionRecord.status ==== RewardRedemptionStatus.Redeemed
      rewardRedemptionRecord.orderId ==== Some(orderId)
      rewardRedemptionRecord.objectId ==== Some(rewardRedemptionSync.objectId)
      rewardRedemptionRecord.objectType ==== Some(rewardRedemptionSync.objectType)
    }

    def assertDeliveryAddress(upsertion: DeliveryAddressUpsertion, entity: DeliveryAddress) = {
      upsertion.id ==== entity.id
      upsertion.firstName ==== entity.firstName
      upsertion.lastName ==== entity.lastName
      if (upsertion.drivingDistanceInMeters.isDefined)
        upsertion.drivingDistanceInMeters ==== entity.drivingDistanceInMeters
      if (upsertion.estimatedDrivingTimeInMins.isDefined)
        upsertion.estimatedDrivingTimeInMins ==== entity.estimatedDrivingTimeInMins

      if (upsertion.address.line1.isDefined) upsertion.address.line1 ==== entity.address.line1
      if (upsertion.address.line2.isDefined) upsertion.address.line2 ==== entity.address.line2
      if (upsertion.address.city.isDefined) upsertion.address.city ==== entity.address.city
      if (upsertion.address.state.isDefined) upsertion.address.state ==== entity.address.state
      if (upsertion.address.country.isDefined) upsertion.address.country ==== entity.address.country
      if (upsertion.address.postalCode.isDefined) upsertion.address.postalCode ==== entity.address.postalCode
    }

    def assertOnlineOrderAttribute(orderUpsertion: OrderUpsertion, orderEntity: OrderEntity) = {
      val upsertion = orderUpsertion.onlineOrderAttribute.get
      val entity = orderEntity.onlineOrderAttribute.get

      upsertion.id ==== entity.id
      if (upsertion.prepareByTime.isDefined) {
        upsertion.prepareByTime.toOption.get must beApproxTheSame(entity.prepareByTime.get)
        entity.prepareByDateTime must beSome
      }
      if (upsertion.estimatedPrepTimeInMins.isDefined)
        upsertion.estimatedPrepTimeInMins ==== entity.estimatedPrepTimeInMins

      if (orderEntity.deliveryProvider.isEmpty) {
        orderEntity.customer must beSome
        val customer = orderEntity.customer.get
        customer.firstName ==== upsertion.firstName
        customer.lastName ==== upsertion.lastName
        customer.phoneNumber ==== upsertion.phoneNumber
        customer.address.line1 ==== orderUpsertion.deliveryAddress.flatMap(_.address.line1)
        customer.address.line2 ==== orderUpsertion.deliveryAddress.flatMap(_.address.line2)
        customer.address.city ==== orderUpsertion.deliveryAddress.flatMap(_.address.city)
        customer.address.state ==== orderUpsertion.deliveryAddress.flatMap(_.address.state)
        customer.address.postalCode ==== orderUpsertion.deliveryAddress.flatMap(_.address.postalCode)
        customer.address.country ==== orderUpsertion.deliveryAddress.flatMap(_.address.country)
      }

      if (upsertion.acceptanceStatus.isDefined)
        entity.acceptanceStatus ==== upsertion.acceptanceStatus.get
      else
        entity.acceptanceStatus ==== AcceptanceStatus.Pending
    }

    def assertCustomer(
        merchant: MerchantRecord,
        order: OrderEntity,
        firstName: Option[String] = None,
        lastName: Option[String] = None,
        email: Option[String] = None,
        phoneNumber: Option[String] = None,
        address: Option[AddressSync] = None,
        source: Option[CustomerSource] = None,
      ): Unit = {
      order.customer must beSome
      val customerEntity = order.customer.get

      val record = daos.customerMerchantDao.findByMerchantIdAndCustomerId(merchant.id, customerEntity.id).await.get

      firstName.map(record.firstName === Some(_))
      lastName.map(record.lastName === Some(_))
      email.map(record.email === Some(_))
      phoneNumber.map(record.phoneNumber === Some(_))
      address.map { address =>
        record.addressLine1 === address.line1
        record.addressLine2 === address.line2
        record.city === address.city
        record.state === address.state
        record.stateCode === address.stateCode
        record.countryCode === address.countryCode
        record.postalCode === address.postalCode
      }
      source.map(record.source === _)
    }

  }

  trait Fixtures extends MultipleLocationFixtures {
    lazy val product = Factory.simpleProduct(merchant).create
    lazy val cashier = user

    lazy val globalCustomer = Factory.globalCustomer(merchant = Some(merchant)).create

    lazy val baseOrderUpsertion = randomOrderUpsertion().copy(
      creatorUserId = Some(cashier.id),
      customerId = Some(globalCustomer.id),
      locationId = london.id,
      items = Seq.empty,
      assignedUserIds = None,
      paymentTransactions = Seq.empty,
      source = Some(Source.Register),
      taxRates = Seq.empty,
      discounts = Seq.empty,
      rewards = Seq.empty,
      deliveryAddress = None,
      onlineOrderAttribute = None,
      bundles = Seq.empty,
      paymentStatus = PaymentStatus.Pending,
    )
  }
}
