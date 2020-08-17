package io.paytouch.core.reports.resources.ordertaxrates

import io.paytouch.core.data.daos.ConfiguredTestDatabase
import io.paytouch.core.data.model.enums.PaymentStatus
import io.paytouch.core.reports.filters.AdminReportFilters
import io.paytouch.core.reports.resources.ReportsDates
import io.paytouch.core.reports.services.AdminReportService
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }
import io.paytouch.utils.FutureHelpers

trait OrderTaxRatesFSpecFixtures extends ReportsDates with ConfiguredTestDatabase with FutureHelpers {
  val taxRate1 = Factory.taxRate(merchant, name = Some("Tax Rate 1")).create
  val taxRate2 = Factory.taxRate(merchant, name = Some("Tax Rate 2")).create
  val taxRate3 = Factory.taxRate(merchant, name = Some("Tax Rate 3")).create

  val order1Completed = Factory
    .order(
      merchant,
      location = Some(rome),
      receivedAt = Some(now),
      completedAt = Some(now.plusHours(1)),
      isInvoice = Some(false),
      paymentStatus = Some(PaymentStatus.PartiallyRefunded),
    )
    .create
  Factory.orderTaxRate(order1Completed, taxRate1, totalAmount = Some(5)).create
  Factory.orderTaxRate(order1Completed, taxRate2, totalAmount = Some(10)).create
  Factory.orderTaxRate(order1Completed, taxRate3, totalAmount = Some(15)).create

  val order2Completed = Factory
    .order(
      merchant,
      location = Some(london),
      receivedAt = Some(now.plusDays(10)),
      completedAt = Some(now.plusDays(10).plusHours(1)),
      isInvoice = Some(false),
      paymentStatus = Some(PaymentStatus.PartiallyRefunded),
    )
    .create
  Factory.orderTaxRate(order2Completed, taxRate1, totalAmount = Some(16)).create
  Factory.orderTaxRate(order2Completed, taxRate2, totalAmount = Some(32)).create

  val order3Voided = Factory
    .order(
      merchant,
      location = Some(rome),
      receivedAt = Some(now),
      completedAt = Some(now.plusHours(1)),
      isInvoice = Some(false),
      paymentStatus = Some(PaymentStatus.Voided),
    )
    .create
  Factory.orderTaxRate(order3Voided, taxRate1, totalAmount = Some(16)).create
  Factory.orderTaxRate(order3Voided, taxRate2, totalAmount = Some(32)).create

  val orderInvoice = Factory
    .order(
      merchant,
      location = Some(rome),
      receivedAt = Some(now),
      completedAt = Some(now.plusHours(1)),
      isInvoice = Some(true),
      paymentStatus = Some(PaymentStatus.Paid),
    )
    .create
  Factory.orderTaxRate(orderInvoice, taxRate1, totalAmount = Some(16)).create
  Factory.orderTaxRate(orderInvoice, taxRate2, totalAmount = Some(32)).create

  new AdminReportService(db).triggerUpdateReports(filters = adminReportFilters).await
}
