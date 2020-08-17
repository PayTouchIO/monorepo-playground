package io.paytouch.core.reports.resources.loyalty_orders

import io.paytouch.core.data.model.enums._
import io.paytouch.core.reports.resources.ReportsAggrFSpec
import io.paytouch.core.reports.views.LoyaltyOrdersView
import io.paytouch.core.reports.resources.orders.OrdersFSpecFixtures
import io.paytouch.core.reports.services.AdminReportService
import io.paytouch.core.utils.{ UtcTime, FixtureDaoFactory => Factory }

abstract class LoyaltyOrdersFSpec extends ReportsAggrFSpec[LoyaltyOrdersView] {

  def view = LoyaltyOrdersView

  val fixtures = new LoyaltyOrdersFSpecContext

  class LoyaltyOrdersFSpecContext extends ReportsAggrFSpecContext {
    val loyaltyProgram =
      Factory
        .loyaltyProgram(
          merchant,
        )
        .create

    val loyaltyCustomer1 = Factory.globalCustomer().create
    val loyaltyCustomer2 = Factory.globalCustomer().create
    val loyaltyCustomer3 = Factory.globalCustomer().create
    val nonLoyaltyCustomer = Factory.globalCustomer().create

    val membership1 = Factory
      .loyaltyMembership(loyaltyCustomer1, loyaltyProgram)
      .create
    val membership2 = Factory
      .loyaltyMembership(loyaltyCustomer2, loyaltyProgram)
      .create
    val membership3 = Factory
      .loyaltyMembership(loyaltyCustomer3, loyaltyProgram)
      .create

    val order1 = Factory
      .order(
        merchant,
        location = Some(rome),
        globalCustomer = Some(loyaltyCustomer1),
        totalAmount = Some(12),
        paymentStatus = Some(PaymentStatus.Paid),
        receivedAt = Some(nowBetweenFromAndFromInRomeTimezone),
        completedAt = Some(nowBetweenFromAndFromInRomeTimezone),
      )
      .create

    val transaction1 = Factory
      .paymentTransaction(
        order1,
        Seq.empty,
        paymentType = Some(TransactionPaymentType.Cash),
        `type` = Some(TransactionType.Payment),
        paidAt = Some(UtcTime.now),
      )
      .create

    Factory
      .loyaltyPointsHistory(
        loyaltyMembership = membership1,
        points = 12,
        `type` = LoyaltyPointsHistoryType.SpendTransaction,
        objectId = Some(transaction1.id),
      )
      .create

    val order2 = Factory
      .order(
        merchant,
        location = Some(rome),
        globalCustomer = Some(loyaltyCustomer1),
        totalAmount = Some(5),
        paymentStatus = Some(PaymentStatus.Paid),
        receivedAt = Some(nowBetweenFromAndFromInRomeTimezone),
        completedAt = Some(nowBetweenFromAndFromInRomeTimezone),
      )
      .create

    val transaction2 = Factory
      .paymentTransaction(
        order2,
        Seq.empty,
        paymentType = Some(TransactionPaymentType.Cash),
        `type` = Some(TransactionType.Payment),
        paidAt = Some(UtcTime.now),
      )
      .create

    Factory
      .loyaltyPointsHistory(
        loyaltyMembership = membership1,
        points = 5,
        `type` = LoyaltyPointsHistoryType.SpendTransaction,
        objectId = Some(transaction2.id),
      )
      .create

    val order3 = Factory
      .order(
        merchant,
        location = Some(rome),
        globalCustomer = Some(loyaltyCustomer1),
        totalAmount = Some(20),
        paymentStatus = Some(PaymentStatus.Refunded),
        receivedAt = Some(nowBetweenFromAndFromInRomeTimezone),
        completedAt = Some(nowBetweenFromAndFromInRomeTimezone),
      )
      .create

    val transaction3a = Factory
      .paymentTransaction(
        order3,
        Seq.empty,
        paymentType = Some(TransactionPaymentType.Cash),
        `type` = Some(TransactionType.Payment),
        paidAt = Some(UtcTime.now),
      )
      .create

    Factory
      .loyaltyPointsHistory(
        loyaltyMembership = membership1,
        points = 20,
        `type` = LoyaltyPointsHistoryType.SpendTransaction,
        objectId = Some(transaction3a.id),
      )
      .create

    val transaction3b = Factory
      .paymentTransaction(
        order3,
        Seq.empty,
        paymentType = Some(TransactionPaymentType.Cash),
        `type` = Some(TransactionType.Refund),
        paidAt = Some(UtcTime.now),
      )
      .create

    Factory
      .loyaltyPointsHistory(
        loyaltyMembership = membership1,
        points = -20,
        `type` = LoyaltyPointsHistoryType.SpendRefund,
        objectId = Some(transaction3b.id),
      )
      .create

    val order5 = Factory
      .order(
        merchant,
        location = Some(rome),
        globalCustomer = Some(nonLoyaltyCustomer),
        totalAmount = Some(7),
        paymentStatus = Some(PaymentStatus.Paid),
        receivedAt = Some(nowBetweenFromAndFromInRomeTimezone),
        completedAt = Some(nowBetweenFromAndFromInRomeTimezone),
      )
      .create

    val transaction5 = Factory
      .paymentTransaction(
        order5,
        Seq.empty,
        paymentType = Some(TransactionPaymentType.Cash),
        `type` = Some(TransactionType.Payment),
        paidAt = Some(UtcTime.now),
      )
      .create

    val order6 = Factory
      .order(
        merchant,
        location = Some(rome),
        globalCustomer = Some(nonLoyaltyCustomer),
        totalAmount = Some(15),
        paymentStatus = Some(PaymentStatus.Paid),
        receivedAt = Some(nowBetweenFromAndFromInRomeTimezone),
        completedAt = Some(nowBetweenFromAndFromInRomeTimezone),
      )
      .create

    val transaction6 = Factory
      .paymentTransaction(
        order6,
        Seq.empty,
        paymentType = Some(TransactionPaymentType.Cash),
        `type` = Some(TransactionType.Payment),
        paidAt = Some(UtcTime.now),
      )
      .create

    val order7 = Factory
      .order(
        merchant,
        `type` = Some(OrderType.DeliveryRestaurant),
        location = Some(rome),
        globalCustomer = Some(loyaltyCustomer2),
        totalAmount = Some(3),
        paymentStatus = Some(PaymentStatus.Paid),
        receivedAt = Some(nowBetweenFromAndFromInRomeTimezone),
        completedAt = Some(nowBetweenFromAndFromInRomeTimezone),
      )
      .create

    val transaction7 = Factory
      .paymentTransaction(
        order7,
        Seq.empty,
        paymentType = Some(TransactionPaymentType.Cash),
        `type` = Some(TransactionType.Payment),
        paidAt = Some(UtcTime.now),
      )
      .create

    Factory
      .loyaltyPointsHistory(
        loyaltyMembership = membership2,
        points = 3,
        `type` = LoyaltyPointsHistoryType.SpendTransaction,
        objectId = Some(transaction7.id),
      )
      .create

    val order8 = Factory
      .order(
        merchant,
        `type` = Some(OrderType.TakeOut),
        location = Some(rome),
        globalCustomer = Some(loyaltyCustomer3),
        totalAmount = Some(5),
        paymentStatus = Some(PaymentStatus.Paid),
        receivedAt = Some(nowBetweenFromAndFromInRomeTimezone),
        completedAt = Some(nowBetweenFromAndFromInRomeTimezone),
      )
      .create

    val transaction8 = Factory
      .paymentTransaction(
        order8,
        Seq.empty,
        paymentType = Some(TransactionPaymentType.Cash),
        `type` = Some(TransactionType.Payment),
        paidAt = Some(UtcTime.now),
      )
      .create

    Factory
      .loyaltyPointsHistory(
        loyaltyMembership = membership3,
        points = 5,
        `type` = LoyaltyPointsHistoryType.SpendTransaction,
        objectId = Some(transaction8.id),
      )
      .create

    new AdminReportService(db).triggerUpdateReports(filters = adminReportFilters).await
  }
}
