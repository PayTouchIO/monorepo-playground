package io.paytouch.core.reports.resources.products

import io.paytouch.core.data.daos.ConfiguredTestDatabase
import io.paytouch.core.data.model.enums.{ OrderPaymentType, OrderType, PaymentStatus, Source }
import io.paytouch.core.entities.VariantOptionWithType
import io.paytouch.core.reports.filters.AdminReportFilters
import io.paytouch.core.reports.resources.ReportsDates
import io.paytouch.core.reports.services.AdminReportService
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }
import io.paytouch.utils.FutureHelpers

trait ProductsFSpecFixtures extends ReportsDates with ConfiguredTestDatabase with FutureHelpers {
  val productA = Factory.simpleProduct(merchant, name = Some("My Product A"), locations = locations).create
  val productB = Factory.simpleProduct(merchant, name = Some("My Product B"), locations = locations).create
  val productC = Factory.simpleProduct(merchant, name = Some("My Product C"), locations = locations).create
  val productD = Factory.simpleProduct(merchant, name = Some("My Product D"), locations = locations).create

  val template = Factory.templateProduct(merchant, name = Some("Template Product"), locations = locations).create

  val variantOptionType = Factory.variantOptionType(template).create
  val variantOption1 = Factory.variantOption(template, variantOptionType, "Option 1").create
  val variantOption2 = Factory.variantOption(template, variantOptionType, "Option 2").create

  val variantOption1Entity =
    VariantOptionWithType(
      id = variantOption1.id,
      name = variantOption1.name,
      typeName = variantOptionType.name,
      position = variantOption1.position,
      typePosition = variantOptionType.position,
    )
  val variantOption2Entity =
    VariantOptionWithType(
      id = variantOption2.id,
      name = variantOption2.name,
      typeName = variantOptionType.name,
      position = variantOption2.position,
      typePosition = variantOptionType.position,
    )

  val variantProductA =
    Factory.variantProduct(merchant, template, name = Some("Variant Product A"), locations = locations).create
  Factory.productVariantOption(variantProductA, variantOption1).create
  val variantProductB =
    Factory.variantProduct(merchant, template, name = Some("Variant Product B"), locations = locations).create
  Factory.productVariantOption(variantProductB, variantOption2).create

  val order1 = Factory
    .order(
      merchant,
      location = Some(london),
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
      order1,
      product = Some(productA),
      quantity = Some(2),
      costAmount = Some(3),
      taxAmount = Some(6),
      totalPriceAmount = Some(30),
    )
    .create
  Factory
    .orderItem(
      order1,
      product = Some(productB),
      quantity = Some(200),
      costAmount = Some(5),
      taxAmount = Some(8),
      totalPriceAmount = Some(1500),
    )
    .create

  val order2 = Factory
    .order(
      merchant,
      location = Some(rome),
      source = Some(Source.Storefront),
      `type` = Some(OrderType.DineIn),
      paymentType = Some(OrderPaymentType.DebitCard),
      totalAmount = Some(5),
      subtotalAmount = Some(4),
      discountAmount = Some(0.5),
      receivedAt = Some(now),
      completedAt = Some(now.plusMinutes(5)),
      isInvoice = Some(false),
      paymentStatus = Some(PaymentStatus.Paid),
    )
    .create
  Factory
    .orderItem(
      order2,
      product = Some(productC),
      quantity = Some(3),
      costAmount = Some(1),
      taxAmount = Some(3),
      totalPriceAmount = Some(5),
    )
    .create
  Factory
    .orderItem(
      order2,
      product = Some(productA),
      quantity = Some(1000),
      costAmount = Some(0),
      taxAmount = Some(8),
      totalPriceAmount = Some(15),
    )
    .create

  val order3 = Factory
    .order(
      merchant,
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
      paymentStatus = Some(PaymentStatus.PartiallyPaid),
    )
    .create
  Factory
    .orderItem(
      order3,
      product = Some(productB),
      quantity = Some(5),
      costAmount = Some(0.1),
      taxAmount = Some(0.1),
      totalPriceAmount = Some(2),
    )
    .create

  val order4 = Factory
    .order(
      merchant,
      location = Some(rome),
      source = Some(Source.Register),
      `type` = Some(OrderType.DineIn),
      paymentType = Some(OrderPaymentType.CreditCard),
      totalAmount = Some(0),
      subtotalAmount = Some(0),
      discountAmount = Some(0),
      receivedAt = Some(now),
      completedAt = Some(now.plusHours(1)),
      isInvoice = Some(false),
      paymentStatus = Some(PaymentStatus.PartiallyRefunded),
    )
    .create
  Factory
    .orderItem(
      order4,
      product = Some(productD),
      quantity = Some(5),
      costAmount = Some(1),
      taxAmount = Some(8),
      totalPriceAmount = Some(0),
    )
    .create
  Factory
    .orderItem(
      order4,
      product = Some(variantProductA),
      quantity = Some(5),
      costAmount = Some(1),
      taxAmount = Some(1),
      totalPriceAmount = Some(0),
    )
    .create

  val invoice = Factory
    .order(
      merchant,
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

  val nonPaidOrder = Factory
    .order(
      merchant,
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

  new AdminReportService(db).triggerUpdateReports(filters = adminReportFilters).await

}
