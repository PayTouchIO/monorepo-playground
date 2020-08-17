package io.paytouch.core.reports.resources.locationsales

import io.paytouch.core.data.daos.ConfiguredTestDatabase
import io.paytouch.core.data.model.enums.TransactionPaymentType._
import io.paytouch.core.entities.MonetaryAmount
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.reports.entities.SalesAggregate
import io.paytouch.core.reports.resources.orders.OrdersFSpecFixtures
import io.paytouch.core.reports.services.AdminReportService
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }
import io.paytouch.utils.FutureHelpers

trait LocationSalesFSpecFixtures extends OrdersFSpecFixtures with ConfiguredTestDatabase with FutureHelpers {

  val londonEmptyAggregate = SalesAggregate(count = 1)

  val londonFullAggregate = londonEmptyAggregate.copy(
    costs = Some(4.50.$$$),
    discounts = Some(3.80.$$$),
    giftCardSales = Some(0.$$$),
    grossProfits = Some(-8.20.$$$),
    grossSales = Some(5.$$$),
    netSales = Some(-3.70.$$$),
    nonTaxable = Some(0.$$$),
    refunds = Some(0.$$$),
    taxable = Some(1.3.$$$),
    taxes = Some(3.70.$$$),
    tips = Some(5.00.$$$),
    tenderTypes = Some(
      Map(
        CreditCard -> MonetaryAmount(0, currency),
        DebitCard -> MonetaryAmount(5, currency),
        GiftCard -> MonetaryAmount(0, currency),
        Cash -> MonetaryAmount(0, currency),
      ),
    ),
  )

  val romeEmptyAggregate = SalesAggregate(count = 3)

  val romeFullAggregate = romeEmptyAggregate.copy(
    count = 3,
    costs = Some(8.70.$$$),
    discounts = Some(10.3.$$$),
    giftCardSales = Some(0.$$$),
    grossProfits = Some(-3.$$$),
    grossSales = Some(11.0.$$$),
    netSales = Some(5.7.$$$),
    nonTaxable = Some(1.$$$),
    refunds = Some(43.$$$),
    taxable = Some(8.7.$$$),
    taxes = Some(1.3.$$$),
    tips = Some(4.$$$),
    tenderTypes = Some(
      Map(
        CreditCard -> MonetaryAmount(-39, currency),
        DebitCard -> MonetaryAmount(0, currency),
        GiftCard -> MonetaryAmount(1, currency),
        Cash -> MonetaryAmount(1, currency),
      ),
    ),
  )

  new AdminReportService(db).triggerUpdateReports(filters = adminReportFilters).await
}
