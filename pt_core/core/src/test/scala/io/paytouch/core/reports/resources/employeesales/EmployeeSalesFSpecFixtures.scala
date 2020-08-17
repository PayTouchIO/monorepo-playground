package io.paytouch.core.reports.resources.employeesales

import io.paytouch.core.data.daos.ConfiguredTestDatabase
import io.paytouch.core.data.model.enums.TransactionPaymentType._
import io.paytouch.core.entities.MonetaryAmount
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.reports.entities.SalesAggregate
import io.paytouch.core.reports.resources.orders.OrdersFSpecFixtures
import io.paytouch.core.reports.services.AdminReportService
import io.paytouch.utils.FutureHelpers

trait EmployeeSalesFSpecFixtures extends OrdersFSpecFixtures with ConfiguredTestDatabase with FutureHelpers {

  val emptyAggregate = SalesAggregate(
    count = 0,
    costs = Some(0.$$$),
    discounts = Some(0.$$$),
    giftCardSales = Some(0.$$$),
    grossProfits = Some(0.$$$),
    grossSales = Some(0.$$$),
    netSales = Some(0.$$$),
    nonTaxable = Some(0.$$$),
    refunds = Some(0.$$$),
    taxable = Some(0.$$$),
    taxes = Some(0.$$$),
    tips = Some(0.$$$),
  )

  val carloEmptyAggregate = SalesAggregate(count = 1)

  val carloFullAggregate = carloEmptyAggregate.copy(
    costs = Some(8.$$$),
    discounts = Some(8.$$$),
    giftCardSales = Some(0.$$$),
    grossProfits = Some(-2.3.$$$),
    grossSales = Some(10.$$$),
    netSales = Some(5.70.$$$),
    nonTaxable = Some(0.$$$),
    refunds = Some(41.$$$),
    taxable = Some(8.7.$$$),
    taxes = Some(1.3.$$$),
    tips = Some(3.$$$),
  )

  val gabrieleEmptyAggregate = SalesAggregate(count = 1)

  val gabrieleFullAggregate = gabrieleEmptyAggregate.copy(
    costs = Some(4.5.$$$),
    discounts = Some(3.8.$$$),
    giftCardSales = Some(0.$$$),
    grossProfits = Some(-8.2.$$$),
    grossSales = Some(5.$$$),
    netSales = Some(-3.7.$$$),
    nonTaxable = Some(0.$$$),
    refunds = Some(0.$$$),
    taxable = Some(1.3.$$$),
    taxes = Some(3.7.$$$),
    tips = Some(5.$$$),
  )

  val johnEmptyAggregate = SalesAggregate(count = 0)

  val johnFullAggregate = johnEmptyAggregate.copy(
    costs = Some(0.$$$),
    discounts = Some(0.$$$),
    giftCardSales = Some(0.$$$),
    grossProfits = Some(0.$$$),
    grossSales = Some(0.$$$),
    netSales = Some(0.$$$),
    nonTaxable = Some(0.$$$),
    refunds = Some(0.$$$),
    taxable = Some(0.$$$),
    taxes = Some(0.$$$),
    tips = Some(0.$$$),
  )

  new AdminReportService(db).triggerUpdateReports(filters = adminReportFilters).await
}
