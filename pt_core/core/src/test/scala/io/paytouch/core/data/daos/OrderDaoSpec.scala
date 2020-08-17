package io.paytouch.core.data.daos

import java.util.UUID

import cats.implicits._

import io.paytouch.core.data.model._
import io.paytouch.core.data.model.upsertions.{ OrderItemUpsertion, OrderUpsertion }
import io.paytouch.core.utils.{ MultipleLocationFixtures, UtcTime, FixtureDaoFactory => Factory }

class OrderDaoSpec extends DaoSpec {
  lazy val orderDao = daos.orderDao

  abstract class OrderDaoSpecContext extends DaoSpecContext with MultipleLocationFixtures {
    val orderId = UUID.randomUUID
    val orderItemId = UUID.randomUUID

    val giftCardProduct = Factory.giftCardProduct(merchant).create
    val giftCard = Factory.giftCard(giftCardProduct).create

    val globalCustomer = Factory.globalCustomer().create
    val loyaltyProgram = Factory.loyaltyProgram(merchant).create
    val loyaltyReward = Factory.loyaltyReward(loyaltyProgram).create
    val loyaltyMembership = Factory.loyaltyMembership(globalCustomer, loyaltyProgram).create
    val orderUpdate = randomModelOrderUpdate().copy(
      id = Some(orderId),
      merchantId = Some(merchant.id),
      locationId = Some(london.id),
      version = Some(1),
      receivedAt = Some(UtcTime.now),
      receivedAtTz = Some(UtcTime.now),
    )

    @scala.annotation.nowarn("msg=Auto-application")
    val orderItemUpsertion =
      OrderItemUpsertion
        .empty(
          orderItem = random[OrderItemUpdate]
            .copy(
              id = orderItemId.some,
              merchantId = merchant.id.some,
              orderId = orderId.some,
            ),
        )
        .copy(
          discounts = Seq(
            random[OrderItemDiscountUpdate].copy(
              merchantId = merchant.id.some,
              orderItemId = orderItemId.some,
              `type` = genDiscountType.instance.some,
              amount = genBigDecimal.instance.some,
            ),
          ).some,
          modifierOptions = Seq(
            random[OrderItemModifierOptionUpdate].copy(
              merchantId = merchant.id.some,
              orderItemId = orderItemId.some,
              name = randomWord.some,
              `type` = genModifierSetType.instance.some,
              priceAmount = genBigDecimal.instance.some,
              quantity = genBigDecimal.instance.some,
            ),
          ).some,
          taxRates = Seq(
            random[OrderItemTaxRateUpdate].copy(
              merchantId = merchant.id.some,
              orderItemId = orderItemId.some,
              name = randomWord.some,
              value = genBigDecimal.instance.some,
            ),
          ).some,
          variantOptions = Seq(
            random[OrderItemVariantOptionUpdate].copy(
              merchantId = merchant.id.some,
              orderItemId = orderItemId.some,
              optionName = randomWord.some,
              optionTypeName = randomWord.some,
              position = genInt.instance.some,
            ),
          ).some,
          giftCardPassRecipientEmail = None,
        )

    val orderBundleUpdate = random[OrderBundleUpdate].copy(
      merchantId = Some(merchant.id),
      orderId = Some(orderId),
      bundleOrderItemId = Some(orderItemId),
    )

    val orderDiscountUpdate = random[OrderDiscountUpdate].copy(
      merchantId = Some(merchant.id),
      orderId = Some(orderId),
      `type` = Some(genDiscountType.instance),
      amount = Some(genBigDecimal.instance),
    )

    val orderTaxRateUpdate = random[OrderTaxRateUpdate].copy(
      merchantId = Some(merchant.id),
      orderId = Some(orderId),
      name = Some(randomWord),
      value = Some(genBigDecimal.instance),
      totalAmount = Some(genBigDecimal.instance),
    )

    val giftCardPassUpdate = random[GiftCardPassUpdate].copy(
      merchantId = Some(merchant.id),
      orderItemId = Some(orderItemId),
      giftCardId = Some(giftCard.id),
      lookupId = Some(genLookupId.instance),
      balanceAmount = Some(genBigDecimal.instance),
      originalAmount = Some(genBigDecimal.instance),
      onlineCode = Some(genOnlineCode.instance),
    )

    @scala.annotation.nowarn("msg=Auto-application")
    val rewardRedemptionUpdate =
      random[RewardRedemptionUpdate].copy(
        merchantId = Some(merchant.id),
        orderId = Some(orderId),
        loyaltyRewardId = Some(loyaltyReward.id),
        loyaltyRewardType = Some(genRewardType.instance),
        loyaltyMembershipId = Some(loyaltyMembership.id),
        points = Some(genInt.instance),
        status = Some(genRewardRedemptionStatus.instance),
      )

    val orderUpsertion = OrderUpsertion(
      order = orderUpdate,
      orderItems = Seq(orderItemUpsertion),
      creatorOrderUsers = None,
      assignedOrderUsers = None,
      paymentTransactions = Seq.empty[PaymentTransactionUpdate],
      paymentTransactionFees = Seq.empty[PaymentTransactionFeeUpdate],
      paymentTransactionOrderItems = Seq.empty[PaymentTransactionOrderItemUpdate],
      customerLocation = None,
      orderTaxRates = Some(Seq(orderTaxRateUpdate)),
      orderDiscounts = Some(Seq(orderDiscountUpdate)),
      giftCardPasses = Some(Seq(giftCardPassUpdate)),
      rewardRedemptions = Some(Seq(rewardRedemptionUpdate)),
      canDeleteOrderItems = false,
      events = List.empty,
      deliveryAddress = None,
      onlineOrderAttribute = None,
      orderBundles = Some(Seq(orderBundleUpdate)),
    )

    def assertFieldsAreExpected() = {
      orderDao.findById(orderId).await must beSome

      daos.orderItemDao.findByOrderId(orderId).await must haveLength(1)
      daos.orderBundleDao.findByOrderId(orderId).await must haveLength(1)
      daos.orderDiscountDao.findByOrderIds(Seq(orderId)).await must haveLength(1)
      daos.orderTaxRateDao.findAllByOrderIds(Seq(orderId)).await must haveLength(1)
      daos.rewardRedemptionDao.findPerOrderIds(Seq(orderId)).await must haveLength(1)

      daos.orderItemDiscountDao.findAllByOrderItemIds(Seq(orderItemId)).await must haveLength(1)
      daos.orderItemModifierOptionDao.findAllByOrderItemIds(Seq(orderItemId)).await must haveLength(1)
      daos.orderItemTaxRateDao.findAllByOrderItemIds(Seq(orderItemId)).await must haveLength(1)
      daos.orderItemVariantOptionDao.findAllByOrderItemIds(Seq(orderItemId)).await must haveLength(1)
      daos.giftCardPassDao.findByOrderItemId(orderItemId).await must beSome
    }
  }

  "OrderDao" in {
    "upsert" should {
      "with empty upsertion should not delete important data" in new OrderDaoSpecContext {
        orderDao.upsert(orderUpsertion).await
        assertFieldsAreExpected()

        @scala.annotation.nowarn("msg=Auto-application")
        val emptyOrderItemUpsertion =
          OrderItemUpsertion.empty(
            orderItem = random[OrderItemUpdate]
              .copy(
                id = orderItemId.some,
              ),
          )
        val emptyOrderUpsertion =
          OrderUpsertion
            .empty(OrderUpdate.empty(orderId))
            .copy(orderItems = Seq(emptyOrderItemUpsertion))
        orderDao.upsert(emptyOrderUpsertion).await
        assertFieldsAreExpected()
      }
    }
  }
}
