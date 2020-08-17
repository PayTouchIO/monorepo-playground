package io.paytouch.core.reports.resources.giftcardpasses

import java.time.ZonedDateTime

import io.paytouch.core.data.daos.ConfiguredTestDatabase
import io.paytouch.core.data.model.{
  GiftCardPassRecord,
  GiftCardPassTransactionRecord,
  GlobalCustomerRecord,
  LocationRecord,
}
import io.paytouch.core.entities.PaymentDetails
import io.paytouch.core.reports.resources.ReportsDates
import io.paytouch.core.reports.services.AdminReportService
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }
import io.paytouch.utils.FutureHelpers

trait GiftCardPassesFSpecFixtures extends ReportsDates with ConfiguredTestDatabase with FutureHelpers {

  val giftCardProduct = Factory.giftCardProduct(merchant).create
  val giftCard = Factory.giftCard(giftCardProduct).create

  def buyPass(
      customer: GlobalCustomerRecord,
      location: LocationRecord,
      date: ZonedDateTime,
      value: BigDecimal,
      remainingValue: BigDecimal,
      isCustomAmount: Boolean,
    ): GiftCardPassRecord = {
    val order =
      Factory
        .order(
          merchant,
          location = Some(location),
          globalCustomer = Some(customer),
          receivedAt = Some(date),
          completedAt = Some(date),
        )
        .create
    val orderItem = Factory.orderItem(order, Some(giftCardProduct)).create
    Factory
      .giftCardPass(
        giftCard,
        orderItem,
        originalAmount = Some(value),
        balanceAmount = Some(remainingValue),
        overrideNow = Some(date),
        isCustomAmount = Some(isCustomAmount),
      )
      .create
  }

  def chargePass(
      pass: GiftCardPassRecord,
      customer: GlobalCustomerRecord,
      location: LocationRecord,
      charge: BigDecimal,
      date: ZonedDateTime,
    ): GiftCardPassTransactionRecord = {
    val transaction =
      Factory.giftCardPassTransaction(pass, totalAmount = Some(-charge), overrideNow = Some(date)).create
    val order = Factory
      .order(
        merchant,
        location = Some(location),
        globalCustomer = Some(customer),
        receivedAt = Some(date),
        completedAt = Some(date),
      )
      .create
    Factory
      .paymentTransaction(
        order,
        paymentDetails = Some(
          PaymentDetails(
            amount = Some(-charge),
            paidInAmount = Some(-charge),
            giftCardPassTransactionId = Some(transaction.id),
          ),
        ),
      )
      .create
    transaction
  }

  val week1 = now.plusWeeks(1)
  val week2 = now.plusWeeks(2)
  val week3 = now.plusWeeks(3)

  val willow1 = Factory.location(merchant, name = Some("Willow 1"), zoneId = Some("America/New_York")).create
  val willow2 = Factory.location(merchant, name = Some("Willow 2"), zoneId = Some("America/New_York")).create
  val willow3 = Factory.location(merchant, name = Some("Willow 3"), zoneId = Some("America/New_York")).create

  Factory.userLocation(user, willow1).create
  Factory.userLocation(user, willow2).create
  Factory.userLocation(user, willow3).create

  val daniela = Factory.globalCustomer(firstName = Some("Daniela"), lastName = Some("Sfregola")).create
  val francesco = Factory.globalCustomer(firstName = Some("Francesco"), lastName = Some("Levorato")).create
  val marco = Factory.globalCustomer(firstName = Some("Marco"), lastName = Some("Campana")).create
  val david = Factory.globalCustomer(firstName = Some("David"), lastName = Some("Bozin")).create

  // gift card purchases:

  val passA = buyPass(daniela, willow1, week1, 50, 20, false)
  val passB = buyPass(francesco, willow2, week1, 20, 0, true)
  val passC = buyPass(marco, willow1, week2, 40, 35, true)
  val passD = buyPass(david, willow3, week3, 100, 100, false)

  // gift card usages:

  chargePass(passA, marco, willow3, 25, week1)
  chargePass(passA, marco, willow1, -25, week2)
  chargePass(passB, marco, willow2, 20, week2)
  chargePass(passC, daniela, willow1, 5, week3)
  chargePass(passA, david, willow3, 30, week3)

  new AdminReportService(db).triggerUpdateReports(filters = adminReportFilters).await
}
