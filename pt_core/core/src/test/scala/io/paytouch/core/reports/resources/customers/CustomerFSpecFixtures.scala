package io.paytouch.core.reports.resources.customers

import io.paytouch.core.data.daos.ConfiguredTestDatabase
import io.paytouch.core.data.model.enums.{ OrderPaymentType, OrderType, PaymentStatus, Source }
import io.paytouch.core.reports.filters.AdminReportFilters
import io.paytouch.core.reports.resources.ReportsDates
import io.paytouch.core.reports.services.AdminReportService
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }
import io.paytouch.utils.FutureHelpers

trait CustomerFSpecFixtures extends ReportsDates with ConfiguredTestDatabase with FutureHelpers {
  val simpleProductToMakeJoinsWork = Factory.simpleProduct(merchant).create

  val loyaltyProgram = Factory.loyaltyProgram(merchant, locations, active = Some(true)).create

  val anonymousOrder = Factory
    .order(
      merchant,
      customer = None,
      location = Some(rome),
      source = Some(Source.Register),
      `type` = Some(OrderType.DineIn),
      paymentType = Some(OrderPaymentType.CreditCard),
      totalAmount = Some(10),
      subtotalAmount = Some(8),
      discountAmount = Some(0),
      receivedAt = Some(now.minusYears(1)),
      completedAt = Some(now.minusYears(1).plusHours(1)),
      isInvoice = Some(false),
      paymentStatus = Some(PaymentStatus.PartiallyRefunded),
    )
    .create
  Factory
    .orderItem(
      anonymousOrder,
      totalPriceAmount = Some(10),
      paymentStatus = Some(PaymentStatus.Paid),
      quantity = Some(2),
      costAmount = Some(3),
    )
    .create

  val globalDaniela = Factory.globalCustomer(firstName = Some("Daniela"), lastName = Some("Sfregola")).create
  val daniela = Factory.customerMerchant(merchant, globalDaniela).create
  Factory.customerLocation(globalDaniela, rome, Some(3), Some(10)).create
  Factory
    .loyaltyMembership(globalDaniela, loyaltyProgram, customerOptInAt = Some(now.minusYears(8)))
    .create

  val danielaOldOrder = Factory
    .order(
      merchant,
      customer = Some(daniela),
      location = Some(rome),
      source = Some(Source.Register),
      `type` = Some(OrderType.DineIn),
      paymentType = Some(OrderPaymentType.CreditCard),
      totalAmount = Some(10),
      subtotalAmount = Some(8),
      discountAmount = Some(0),
      receivedAt = Some(now.minusYears(1)),
      completedAt = Some(now.minusYears(1).plusHours(1)),
      isInvoice = Some(false),
      paymentStatus = Some(PaymentStatus.PartiallyRefunded),
    )
    .create
  Factory
    .orderItem(
      danielaOldOrder,
      totalPriceAmount = Some(10),
      paymentStatus = Some(PaymentStatus.Paid),
      quantity = Some(2),
      costAmount = Some(3),
    )
    .create

  val danielaOrder1 = Factory
    .order(
      merchant,
      customer = Some(daniela),
      location = Some(rome),
      source = Some(Source.Register),
      `type` = Some(OrderType.DineIn),
      paymentType = Some(OrderPaymentType.CreditCard),
      totalAmount = Some(10),
      subtotalAmount = Some(8),
      discountAmount = Some(0),
      receivedAt = Some(now),
      completedAt = Some(now.plusHours(1)),
      isInvoice = Some(false),
      paymentStatus = Some(PaymentStatus.PartiallyRefunded),
    )
    .create
  Factory
    .orderItem(
      danielaOrder1,
      totalPriceAmount = Some(10),
      paymentStatus = Some(PaymentStatus.Paid),
      quantity = Some(2),
      costAmount = Some(3),
    )
    .create

  val danielaOrder2 = Factory
    .order(
      merchant,
      location = Some(rome),
      customer = Some(daniela),
      source = Some(Source.Storefront),
      `type` = Some(OrderType.DineIn),
      paymentType = Some(OrderPaymentType.DebitCard),
      totalAmount = Some(5),
      subtotalAmount = Some(4),
      discountAmount = Some(0.5),
      receivedAt = Some(now.plusDays(7)),
      completedAt = Some(now.plusDays(7).plusMinutes(5)),
      isInvoice = Some(false),
      paymentStatus = Some(PaymentStatus.Paid),
    )
    .create
  Factory
    .orderItem(
      danielaOrder2,
      totalPriceAmount = Some(3),
      quantity = Some(7),
      paymentStatus = Some(PaymentStatus.Paid),
      costAmount = Some(1),
    )
    .create
  Factory
    .orderItem(
      danielaOrder2,
      totalPriceAmount = Some(2),
      quantity = Some(1000),
      paymentStatus = Some(PaymentStatus.Paid),
      costAmount = Some(0),
    )
    .create

  val danielaOrder3 = Factory
    .order(
      merchant,
      customer = Some(daniela),
      location = Some(rome),
      source = Some(Source.Storefront),
      `type` = Some(OrderType.InStore),
      paymentType = Some(OrderPaymentType.Cash),
      totalAmount = Some(1),
      subtotalAmount = Some(0.8),
      discountAmount = Some(0),
      receivedAt = Some(now.plusDays(14)),
      completedAt = Some(now.plusDays(14).plusMinutes(30)),
      isInvoice = Some(false),
      paymentStatus = Some(PaymentStatus.PartiallyPaid),
    )
    .create
  Factory
    .orderItem(
      danielaOrder3,
      totalPriceAmount = Some(1),
      quantity = Some(2),
      paymentStatus = Some(PaymentStatus.Paid),
      costAmount = Some(0.1),
    )
    .create

  val danielaOrder4 = Factory
    .order(
      merchant,
      customer = Some(daniela),
      location = Some(rome),
      source = Some(Source.Storefront),
      `type` = Some(OrderType.InStore),
      paymentType = Some(OrderPaymentType.Cash),
      totalAmount = Some(3),
      subtotalAmount = Some(2.8),
      discountAmount = Some(1),
      receivedAt = Some(now.plusMonths(5)),
      completedAt = Some(now.plusMonths(4).plusMinutes(30)),
      isInvoice = Some(false),
      paymentStatus = Some(PaymentStatus.PartiallyPaid),
    )
    .create
  Factory
    .orderItem(
      danielaOrder4,
      totalPriceAmount = Some(3),
      quantity = Some(11),
      paymentStatus = Some(PaymentStatus.Paid),
      costAmount = Some(0.1),
    )
    .create

  val danielaInvoice = Factory
    .order(
      merchant,
      customer = Some(daniela),
      location = Some(rome),
      source = Some(Source.Storefront),
      `type` = Some(OrderType.InStore),
      paymentType = Some(OrderPaymentType.Cash),
      totalAmount = Some(1),
      subtotalAmount = Some(0.8),
      discountAmount = Some(0),
      receivedAt = Some(now),
      completedAt = Some(now.plusMinutes(30)),
      isInvoice = Some(true),
      paymentStatus = Some(PaymentStatus.Paid),
    )
    .create

  val danielaNonPaidOrder = Factory
    .order(
      merchant,
      customer = Some(daniela),
      location = Some(rome),
      source = Some(Source.Storefront),
      `type` = Some(OrderType.InStore),
      paymentType = Some(OrderPaymentType.Cash),
      totalAmount = Some(1),
      subtotalAmount = Some(0.8),
      discountAmount = Some(0),
      receivedAt = Some(now),
      completedAt = Some(now.plusMinutes(30)),
      isInvoice = Some(false),
      paymentStatus = Some(PaymentStatus.Pending),
    )
    .create

  val globalFrancesco = Factory.globalCustomer(firstName = Some("Francesco"), lastName = Some("Levorato")).create
  val francesco = Factory.customerMerchant(merchant, globalFrancesco).create
  Factory.customerLocation(globalFrancesco, rome, Some(3), Some(10)).create
  Factory
    .loyaltyMembership(globalFrancesco, loyaltyProgram, customerOptInAt = Some(now.plusDays(17)))
    .create

  val francescoOrder1 = Factory
    .order(
      merchant,
      customer = Some(francesco),
      location = Some(rome),
      source = Some(Source.Register),
      `type` = Some(OrderType.DineIn),
      paymentType = Some(OrderPaymentType.CreditCard),
      totalAmount = Some(11),
      subtotalAmount = Some(8),
      discountAmount = Some(0),
      receivedAt = Some(now),
      completedAt = Some(now.plusHours(1)),
      isInvoice = Some(false),
      paymentStatus = Some(PaymentStatus.PartiallyRefunded),
    )
    .create
  Factory
    .orderItem(
      francescoOrder1,
      totalPriceAmount = Some(11),
      quantity = Some(2),
      costAmount = Some(3),
      paymentStatus = Some(PaymentStatus.Paid),
    )
    .create

  val francescoOrder2 = Factory
    .order(
      merchant,
      location = Some(rome),
      customer = Some(francesco),
      source = Some(Source.Storefront),
      `type` = Some(OrderType.DineIn),
      paymentType = Some(OrderPaymentType.DebitCard),
      totalAmount = Some(6),
      subtotalAmount = Some(5),
      discountAmount = Some(1.5),
      receivedAt = Some(now.plusDays(26)),
      completedAt = Some(now.plusDays(26).plusMinutes(5)),
      isInvoice = Some(false),
      paymentStatus = Some(PaymentStatus.Paid),
    )
    .create
  Factory
    .orderItem(
      francescoOrder2,
      totalPriceAmount = Some(3),
      quantity = Some(4),
      costAmount = Some(1),
      paymentStatus = Some(PaymentStatus.Paid),
    )
    .create
  Factory
    .orderItem(
      francescoOrder2,
      totalPriceAmount = Some(3),
      quantity = Some(5055),
      costAmount = Some(0.1),
      paymentStatus = Some(PaymentStatus.Paid),
    )
    .create

  val globalMarco = Factory.globalCustomer(firstName = Some("Marco"), lastName = Some("Campana")).create
  val marco = Factory.customerMerchant(merchant, globalMarco).create
  Factory.customerLocation(globalMarco, london, Some(3), Some(10)).create

  val marcoOrder1 = Factory
    .order(
      merchant,
      customer = Some(marco),
      location = Some(london),
      source = Some(Source.Register),
      `type` = Some(OrderType.DineIn),
      paymentType = Some(OrderPaymentType.CreditCard),
      totalAmount = Some(1000),
      subtotalAmount = Some(800),
      discountAmount = Some(0),
      receivedAt = Some(now),
      completedAt = Some(now.plusHours(1)),
      isInvoice = Some(false),
      paymentStatus = Some(PaymentStatus.PartiallyRefunded),
    )
    .create
  Factory
    .orderItem(
      marcoOrder1,
      totalPriceAmount = Some(1000),
      quantity = Some(51.5),
      costAmount = Some(4),
      paymentStatus = Some(PaymentStatus.Paid),
    )
    .create

  new AdminReportService(db).triggerUpdateReports(filters = adminReportFilters).await

}
