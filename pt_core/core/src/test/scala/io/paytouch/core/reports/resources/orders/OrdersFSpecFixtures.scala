package io.paytouch.core.reports.resources.orders

import io.paytouch.core.data.daos.ConfiguredTestDatabase
import io.paytouch.core.data.model.enums._
import io.paytouch.core.entities.OnlineOrderAttribute
import io.paytouch.core.entities.PaymentDetails
import io.paytouch.core.reports.resources.ReportsDates
import io.paytouch.core.reports.services.AdminReportService
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }
import io.paytouch.utils.FutureHelpers

trait OrdersFSpecFixtures extends ReportsDates with ConfiguredTestDatabase with FutureHelpers {
  val employeeCarlo = Factory.user(merchant, firstName = Some("Carlo"), lastName = Some("Mallone")).create
  val employeeGabriele = Factory.user(merchant, firstName = Some("Gabriele"), lastName = Some("Salvini")).create

  val globalDaniela = Factory.globalCustomer(firstName = Some("Daniela"), lastName = Some("Sfregola")).create
  val globalFrancesco = Factory.globalCustomer(firstName = Some("Francesco"), lastName = Some("Levorato")).create
  val globalMarco = Factory.globalCustomer(firstName = Some("Marco"), lastName = Some("Campana")).create

  val category1 = Factory.systemCategory(defaultMenuCatalog, name = Some("Category 1"), locations = locations).create
  val category2 = Factory.systemCategory(defaultMenuCatalog, name = Some("Category 2"), locations = locations).create

  val simpleProduct1 =
    Factory
      .simpleProduct(
        merchant,
        name = Some("Product 1"),
        upc = Some("456-upc"),
        sku = Some("sku-789"),
        categories = Seq(category1),
        locations = locations,
      )
      .create

  val templateProduct =
    Factory
      .templateProduct(merchant, categories = Seq(category2), locations = locations)
      .create

  val variantProduct2 =
    Factory
      .variantProduct(
        merchant,
        templateProduct,
        name = Some("Product 2"),
        upc = Some("789-upc"),
        sku = Some("sku-456"),
        locations = locations,
      )
      .create

  val variantProduct3 =
    Factory
      .variantProduct(
        merchant,
        templateProduct,
        name = Some("Product 3"),
        upc = Some("123-upc"),
        sku = Some("sku-123"),
        locations = locations,
        deletedAt = Some(now),
      ) // will still be included because it falls in the from/to range
      .create

//  ORDERS
//                    id                  |  name  | total_amount | tax_amount | tip_amount |   payment_status
//  --------------------------------------+--------+--------------+------------+------------+--------------------
//   5c090867-bc15-4863-8582-0daa22da8134 | London |         5.00 |       2.70 |       5.00 | paid
//   e1c57c3d-2e15-42da-9661-0f16929f99c6 | Rome   |        50.00 |       1.30 |       3.00 | partially_refunded
//   221168c9-1fc2-4ddc-9a52-77c71ede2a57 | Rome   |         1.00 |       3.80 |       1.00 | partially_paid

//  ORDER ITEMS
//                order_id               | payment_status | tax_amount | discount_amount
//  -------------------------------------+----------------+------------+-----------------
//  e1c57c3d-2e15-42da-9661-0f16929f99c6 | paid           |       1.30 |            5.70
//  e1c57c3d-2e15-42da-9661-0f16929f99c6 | refunded       |       0.00 |            0.00
//  5c090867-bc15-4863-8582-0daa22da8134 | paid           |       2.70 |            1.50
//  5c090867-bc15-4863-8582-0daa22da8134 | paid           |       1.00 |            0.00
//  221168c9-1fc2-4ddc-9a52-77c71ede2a57 | paid           |       0.00 |            0.00

  val order1 =
    Factory
      .order(
        merchant,
        user = Some(employeeCarlo),
        location = Some(rome),
        globalCustomer = Some(globalDaniela),
        source = Some(Source.Register),
        `type` = Some(OrderType.DineIn),
        paymentType = Some(OrderPaymentType.CreditCard),
        totalAmount = Some(51),
        subtotalAmount = Some(8),
        discountAmount = Some(5.7),
        ticketDiscountAmount = Some(2.3),
        taxAmount = Some(1.3),
        tipAmount = Some(3),
        receivedAt = Some(nowBetweenFromAndFromInRomeTimezone),
        completedAt = Some(nowBetweenFromAndFromInRomeTimezone.plusHours(1)),
        isInvoice = Some(false),
        paymentStatus = Some(PaymentStatus.PartiallyRefunded),
      )
      .create

  Factory
    .orderItem(
      order1,
      product = Some(simpleProduct1),
      quantity = Some(2),
      discountAmount = Some(5.7),
      taxAmount = Some(1.3),
      totalPriceAmount = Some(10),
      calculatedPriceAmount = Some(10),
      costAmount = Some(4),
      paymentStatus = Some(PaymentStatus.Paid),
    )
    .create

  val order1Item1 =
    Factory
      .orderItem(
        order1,
        product = Some(simpleProduct1),
        quantity = Some(3),
        discountAmount = Some(0),
        taxAmount = Some(0),
        totalPriceAmount = Some(40),
        calculatedPriceAmount = Some(40),
        costAmount = Some(16),
        paymentStatus = Some(PaymentStatus.Refunded),
      )
      .create

  Factory.orderFeedback(order1, globalDaniela, rating = Some(5)).create

  Factory
    .paymentTransaction(
      order1,
      paymentDetails = Some(PaymentDetails(amount = Some(1), paidInAmount = Some(1), tipAmount = 1)),
      paymentType = Some(TransactionPaymentType.CreditCard),
      `type` = Some(TransactionType.Payment),
    )
    .create

  Factory
    .paymentTransaction(
      order1,
      orderItems = Seq(order1Item1),
      paymentDetails = Some(
        PaymentDetails(
          amount = Some(40),
          paidOutAmount = Some(40),
          transactionResult = Some(CardTransactionResultType.Approved),
          transactionStatus = Some(CardTransactionStatusType.Committed),
        ),
      ),
      paymentType = Some(TransactionPaymentType.CreditCard),
      `type` = Some(TransactionType.Refund),
    )
    .create

  Factory
    .paymentTransaction(
      order1,
      paymentDetails = Some(
        PaymentDetails(
          amount = Some(1000),
          paidInAmount = Some(1000),
          tipAmount = 1,
          transactionResult = Some(CardTransactionResultType.Declined),
          transactionStatus = Some(CardTransactionStatusType.Uncommitted),
        ),
      ),
      paymentType = Some(TransactionPaymentType.CreditCard),
      `type` = Some(TransactionType.Payment),
    )
    .create

  Factory
    .paymentTransaction(
      order1,
      paymentDetails = Some(PaymentDetails(amount = Some(2), paidInAmount = Some(2), tipAmount = 1)),
      paymentType = Some(TransactionPaymentType.GiftCard),
      `type` = Some(TransactionType.Payment),
    )
    .create

  Factory
    .paymentTransaction(
      order1,
      paymentDetails = Some(PaymentDetails(amount = Some(1), paidInAmount = Some(1), tipAmount = 1)),
      paymentType = Some(TransactionPaymentType.GiftCard),
      `type` = Some(TransactionType.Refund),
    )
    .create

  val order2 =
    Factory
      .order(
        merchant,
        user = Some(employeeGabriele),
        location = Some(london),
        globalCustomer = Some(globalFrancesco),
        source = Some(Source.Storefront),
        `type` = Some(OrderType.DineIn),
        paymentType = Some(OrderPaymentType.DebitCard),
        totalAmount = Some(5),
        subtotalAmount = Some(4),
        discountAmount = Some(1.5),
        ticketDiscountAmount = Some(2.3),
        taxAmount = Some(2.7),
        tipAmount = Some(5),
        receivedAt = Some(now),
        completedAt = Some(now.plusMinutes(5)),
        isInvoice = Some(false),
        paymentStatus = Some(PaymentStatus.Paid),
      )
      .create

  val order2Item1 =
    Factory
      .orderItem(
        order2,
        product = Some(simpleProduct1),
        quantity = Some(4.5),
        discountAmount = Some(1.5),
        taxAmount = Some(2.7),
        totalPriceAmount = Some(5),
        calculatedPriceAmount = Some(5),
        costAmount = Some(1),
        paymentStatus = Some(PaymentStatus.Paid),
      )
      .create

  Factory
    .orderItem(
      order2,
      product = Some(variantProduct2),
      quantity = Some(1000),
      discountAmount = Some(0),
      taxAmount = Some(1.0),
      totalPriceAmount = Some(0),
      calculatedPriceAmount = Some(0),
      costAmount = Some(0),
      paymentStatus = Some(PaymentStatus.Paid),
    )
    .create

  val paymentTransactionFee2PaymentAmount = 15
  val paymentTransaction2Payment = Factory
    .paymentTransaction(
      order2,
      paymentDetails = Some(
        PaymentDetails(
          amount = Some(5 + paymentTransactionFee2PaymentAmount),
          paidInAmount = Some(5 + paymentTransactionFee2PaymentAmount),
          tipAmount = 5,
        ),
      ),
      paymentType = Some(TransactionPaymentType.DebitCard),
      `type` = Some(TransactionType.Payment),
    )
    .create

  Factory.paymentTransactionFee(paymentTransaction2Payment, amount = Some(paymentTransactionFee2PaymentAmount)).create

  Factory
    .paymentTransaction(
      order2,
      paymentDetails = Some(
        PaymentDetails(
          amount = Some(1000),
          paidInAmount = Some(1000),
          tipAmount = 1,
          transactionResult = Some(CardTransactionResultType.Declined),
          transactionStatus = Some(CardTransactionStatusType.Uncommitted),
        ),
      ),
      paymentType = Some(TransactionPaymentType.CreditCard),
      `type` = Some(TransactionType.Payment),
    )
    .create

  val order3 =
    Factory
      .order(
        merchant,
        location = Some(rome),
        globalCustomer = Some(globalFrancesco),
        source = Some(Source.Storefront),
        `type` = Some(OrderType.InStore),
        paymentType = Some(OrderPaymentType.Cash),
        totalAmount = Some(1),
        subtotalAmount = Some(0.8),
        discountAmount = Some(0),
        ticketDiscountAmount = Some(2.3),
        taxAmount = Some(3.8),
        tipAmount = Some(1),
        receivedAt = Some(now),
        completedAt = Some(now.plusMinutes(30)),
        isInvoice = Some(false),
        paymentStatus = Some(PaymentStatus.PartiallyPaid),
      )
      .create

  Factory
    .orderItem(
      order3,
      product = Some(simpleProduct1),
      quantity = Some(7),
      discountAmount = Some(0),
      taxAmount = Some(0),
      totalPriceAmount = Some(1),
      calculatedPriceAmount = Some(1),
      costAmount = Some(0.1),
      paymentStatus = Some(PaymentStatus.Paid),
    )
    .create

  Factory
    .orderItem(
      order3,
      product = Some(variantProduct3),
      quantity = Some(5),
      discountAmount = Some(0),
      taxAmount = Some(0),
      totalPriceAmount = Some(0.5),
      calculatedPriceAmount = Some(0.5),
      costAmount = Some(0.1),
      paymentStatus = Some(PaymentStatus.Pending),
    )
    .create

  Factory.orderFeedback(order2, globalFrancesco, rating = Some(3)).create

  Factory
    .paymentTransaction(
      order3,
      paymentDetails = Some(
        PaymentDetails(amount = Some(1), currency = None, paidInAmount = Some(1), paidOutAmount = None, tipAmount = 1),
      ),
      paymentType = Some(TransactionPaymentType.Cash),
      `type` = Some(TransactionType.Payment),
    )
    .create

  Factory
    .paymentTransaction(
      order3,
      paymentDetails = Some(
        PaymentDetails(
          amount = Some(1000),
          paidInAmount = Some(1000),
          tipAmount = 1,
          transactionResult = Some(CardTransactionResultType.Declined),
          transactionStatus = Some(CardTransactionStatusType.Uncommitted),
        ),
      ),
      paymentType = Some(TransactionPaymentType.CreditCard),
      `type` = Some(TransactionType.Payment),
    )
    .create

  val invoice =
    Factory
      .order(
        merchant,
        location = Some(rome),
        source = Some(Source.Storefront),
        `type` = Some(OrderType.InStore),
        paymentType = Some(OrderPaymentType.Cash),
        totalAmount = Some(1),
        subtotalAmount = Some(0.8),
        discountAmount = Some(0),
        taxAmount = Some(3),
        tipAmount = Some(0),
        receivedAt = Some(now),
        completedAt = Some(now.plusMinutes(30)),
        isInvoice = Some(true),
        paymentStatus = Some(PaymentStatus.Paid),
      )
      .create

  val nonPaidOrder =
    Factory
      .order(
        merchant,
        location = Some(rome),
        source = Some(Source.Storefront),
        `type` = Some(OrderType.InStore),
        paymentType = Some(OrderPaymentType.Cash),
        totalAmount = Some(1),
        subtotalAmount = Some(0.8),
        discountAmount = Some(0),
        taxAmount = Some(2.2),
        tipAmount = Some(4),
        receivedAt = Some(now),
        completedAt = Some(now.plusMinutes(30)),
        isInvoice = Some(false),
        paymentStatus = Some(PaymentStatus.Pending),
      )
      .create

  val voidedOrder =
    Factory
      .order(
        merchant,
        location = Some(rome),
        source = Some(Source.Storefront),
        `type` = Some(OrderType.InStore),
        paymentType = Some(OrderPaymentType.Cash),
        totalAmount = Some(2),
        subtotalAmount = Some(0.8),
        discountAmount = Some(0),
        taxAmount = Some(0),
        tipAmount = Some(0),
        receivedAt = Some(now),
        completedAt = Some(now.plusMinutes(30)),
        isInvoice = Some(false),
        paymentStatus = Some(PaymentStatus.Voided),
      )
      .create

  Factory
    .orderItem(
      voidedOrder,
      product = Some(simpleProduct1),
      quantity = Some(1),
      discountAmount = Some(0),
      taxAmount = Some(0),
      totalPriceAmount = Some(1),
      calculatedPriceAmount = Some(1),
      costAmount = Some(1),
      paymentStatus = Some(PaymentStatus.Voided),
    )
    .create

  Factory
    .orderItem(
      voidedOrder,
      product = Some(variantProduct2),
      quantity = Some(1),
      discountAmount = Some(0),
      taxAmount = Some(0),
      totalPriceAmount = Some(1),
      calculatedPriceAmount = Some(1),
      costAmount = Some(1),
      paymentStatus = Some(PaymentStatus.Voided),
    )
    .create

  val paymentTransactionFeeVoidedOrderPaymentAmount = 17

  val paymentTransactionVoidedOrderPayment =
    Factory
      .paymentTransaction(
        voidedOrder,
        paymentDetails = Some(
          PaymentDetails(
            amount = Some(2 + paymentTransactionFeeVoidedOrderPaymentAmount),
            paidInAmount = Some(2 + paymentTransactionFeeVoidedOrderPaymentAmount),
          ),
        ),
        paymentType = Some(TransactionPaymentType.Cash),
        `type` = Some(TransactionType.Payment),
      )
      .create

  Factory
    .paymentTransactionFee(
      paymentTransactionVoidedOrderPayment,
      amount = Some(paymentTransactionFeeVoidedOrderPaymentAmount),
    )
    .create

  val paymentTransactionVoidedOrderVoid =
    Factory
      .paymentTransaction(
        voidedOrder,
        paymentDetails = Some(
          PaymentDetails(
            amount = Some(2 + paymentTransactionFeeVoidedOrderPaymentAmount),
            paidOutAmount = Some(2 + paymentTransactionFeeVoidedOrderPaymentAmount),
          ),
        ),
        paymentType = Some(TransactionPaymentType.Cash),
        `type` = Some(TransactionType.Void),
      )
      .create

  Factory
    .paymentTransactionFee(
      paymentTransactionVoidedOrderVoid,
      amount = Some(-paymentTransactionFeeVoidedOrderPaymentAmount),
    )
    .create

  val openOnlineOrder =
    Factory
      .order(
        merchant,
        user = Some(employeeCarlo),
        paymentStatus = Some(PaymentStatus.PartiallyRefunded),
        onlineOrderAttribute = Some(Factory.onlineOrderAttribute(merchant, Some(AcceptanceStatus.Open)).create),
      )
      .create

  Factory
    .orderItem(
      openOnlineOrder,
    )
    .create

  new AdminReportService(db).triggerUpdateReports(filters = adminReportFilters).await
}

object OrdersFSpecFixtures extends OrdersFSpecFixtures
