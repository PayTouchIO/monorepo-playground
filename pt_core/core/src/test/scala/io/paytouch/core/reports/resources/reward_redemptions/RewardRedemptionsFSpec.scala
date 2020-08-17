package io.paytouch.core.reports.resources.reward_redemptions

import io.paytouch.core.entities.enums.RewardType
import io.paytouch.core.data.model.enums._
import io.paytouch.core.reports.resources.ReportsAggrFSpec
import io.paytouch.core.reports.views.RewardRedemptionsView
import io.paytouch.core.reports.resources.orders.OrdersFSpecFixtures
import io.paytouch.core.reports.services.AdminReportService
import io.paytouch.core.utils.{ UtcTime, FixtureDaoFactory => Factory }

abstract class RewardRedemptionsFSpec extends ReportsAggrFSpec[RewardRedemptionsView] {

  def view = RewardRedemptionsView

  val fixtures = new RewardRedemptionsFSpecContext

  class RewardRedemptionsFSpecContext extends ReportsAggrFSpecContext {
    val loyaltyProgram =
      Factory
        .loyaltyProgram(
          merchant,
        )
        .create

    val loyaltyReward1 = Factory
      .loyaltyReward(
        loyaltyProgram = loyaltyProgram,
        rewardType = Some(RewardType.FreeProduct),
      )
      .create

    val loyaltyReward2 = Factory
      .loyaltyReward(
        loyaltyProgram = loyaltyProgram,
        rewardType = Some(RewardType.DiscountFixedAmount),
      )
      .create

    val loyaltyCustomer1 = Factory.globalCustomer().create

    val membership1 = Factory
      .loyaltyMembership(loyaltyCustomer1, loyaltyProgram)
      .create

    val order1 = Factory
      .order(
        merchant,
        location = Some(rome),
        globalCustomer = Some(loyaltyCustomer1),
        totalAmount = Some(20),
        paymentStatus = Some(PaymentStatus.Paid),
        completedAt = Some(nowBetweenFromAndFromInRomeTimezone),
      )
      .create

    Factory
      .loyaltyPointsHistory(
        loyaltyMembership = membership1,
        points = 20,
        `type` = LoyaltyPointsHistoryType.SpendTransaction,
        objectId = Some(order1.id),
      )
      .create

    val order2 = Factory
      .order(
        merchant,
        location = Some(rome),
        globalCustomer = Some(loyaltyCustomer1),
        totalAmount = Some(12),
        paymentStatus = Some(PaymentStatus.Paid),
        completedAt = Some(nowBetweenFromAndFromInRomeTimezone),
      )
      .create

    val order2Item1 =
      Factory
        .orderItem(
          order2,
          quantity = Some(1),
          discountAmount = Some(0),
          taxAmount = Some(0),
          priceAmount = Some(10),
          totalPriceAmount = Some(10),
          calculatedPriceAmount = Some(10),
          paymentStatus = Some(PaymentStatus.Paid),
        )
        .create

    Factory
      .rewardRedemption(
        loyaltyMembership = membership1,
        loyaltyReward = loyaltyReward1,
        orderId = Some(order2.id),
        points = Some(8),
        objectId = Some(order2Item1.id),
        objectType = Some(RewardRedemptionType.OrderItem),
        status = Some(RewardRedemptionStatus.Redeemed),
        overrideNow = Some(now),
      )
      .create

    Factory
      .loyaltyPointsHistory(
        loyaltyMembership = membership1,
        points = -8,
        `type` = LoyaltyPointsHistoryType.RewardRedemption,
        objectId = Some(order2.id),
      )
      .create

    val order3 = Factory
      .order(
        merchant,
        location = Some(rome),
        globalCustomer = Some(loyaltyCustomer1),
        totalAmount = Some(5),
        paymentStatus = Some(PaymentStatus.Paid),
        completedAt = Some(nowBetweenFromAndFromInRomeTimezone),
      )
      .create

    val discount = Factory.discount(merchant, amount = Some(10)).create
    val order3Discount1 = Factory.orderDiscount(order3, discount).create

    Factory
      .rewardRedemption(
        loyaltyMembership = membership1,
        loyaltyReward = loyaltyReward2,
        orderId = Some(order3.id),
        points = Some(4),
        objectId = Some(order3Discount1.id),
        objectType = Some(RewardRedemptionType.OrderDiscount),
        status = Some(RewardRedemptionStatus.Redeemed),
        overrideNow = Some(now),
      )
      .create

    Factory
      .loyaltyPointsHistory(
        loyaltyMembership = membership1,
        points = -4,
        `type` = LoyaltyPointsHistoryType.RewardRedemption,
        objectId = Some(order3.id),
      )
      .create

    new AdminReportService(db).triggerUpdateReports(filters = adminReportFilters).await
  }
}
