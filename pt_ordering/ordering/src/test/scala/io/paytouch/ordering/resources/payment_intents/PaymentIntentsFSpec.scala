package io.paytouch.ordering.resources.payment_intents

import java.util.UUID

import io.paytouch.ordering.calculations.RoundingUtils._
import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.clients.paytouch.core.entities.enums._
import io.paytouch.ordering.entities._
import io.paytouch.ordering.entities.enums._
import io.paytouch.ordering.utils.{ CommonArbitraries, FSpec, MultipleLocationFixtures }
import io.paytouch.ordering.stubs.PtCoreStubData

@scala.annotation.nowarn("msg=Auto-application")
abstract class PaymentIntentsFSpec extends FSpec with CommonArbitraries {
  abstract class PaymentIntentsFSpecContext extends FSpecContext with MultipleLocationFixtures {
    val paymentIntentDao = daos.paymentIntentDao

    val merchantContext = Merchant.fromRecord(merchant)
    implicit val coreAuthToken =
      ptCoreClient.generateAuthHeaderForCoreMerchant(merchant.id)

    val taxRates = Seq(
      TaxRate(
        id = UUID.randomUUID,
        name = "City Tax",
        value = 0.08735,
        applyToPrice = true,
        locationOverrides = Map.empty,
      ),
    )

    val item1 = orderItemWithTax(
      randomOrderItem.copy(
        paymentStatus = Some(PaymentStatus.Pending),
      ),
    )
    val item2 = orderItemWithTax(
      randomOrderItem.copy(
        paymentStatus = Some(PaymentStatus.Pending),
      ),
    )

    val order = orderWithItems(
      randomOrder().copy(
        status = OrderStatus.InProgress,
        paymentStatus = PaymentStatus.Pending,
        paymentTransactions = Seq.empty,
      ),
      Seq(
        item1,
        item2,
      ),
    )

    val completedItem1 = orderItemWithTax(
      randomOrderItem.copy(
        paymentStatus = Some(PaymentStatus.Paid),
      ),
    )
    val completedItem2 = orderItemWithTax(
      randomOrderItem.copy(
        paymentStatus = Some(PaymentStatus.Paid),
      ),
    )

    val completedOrder = orderWithItems(
      randomOrder().copy(
        status = OrderStatus.Completed,
        paymentStatus = PaymentStatus.Paid,
        paymentTransactions = Seq(random[PaymentTransaction]),
      ),
      Seq(completedItem1, completedItem2),
    )

    val refundedItem1 = orderItemWithTax(
      randomOrderItem.copy(
        paymentStatus = Some(PaymentStatus.Refunded),
      ),
    )

    val refundedOrder = orderWithItems(
      randomOrder().copy(
        status = OrderStatus.Completed,
        paymentStatus = PaymentStatus.Refunded,
        paymentTransactions = Seq(random[PaymentTransaction]),
      ),
      Seq(refundedItem1),
    )

    PtCoreStubData.recordOrders(Seq(order, completedOrder, refundedOrder))

    def assertCreation(id: UUID, upsertion: PaymentIntentCreation) = {
      val record = paymentIntentDao.findById(id).await.get

      record.merchantId ==== upsertion.merchantId
      record.orderId ==== upsertion.orderId
      record.orderItemIds ==== upsertion.orderItemIds
      record.paymentMethodType ==== upsertion.paymentMethodType
      record.successReturnUrl ==== upsertion.successReturnUrl
      record.successReturnUrl ==== upsertion.successReturnUrl
      record.status ==== PaymentIntentStatus.New

      if (upsertion.metadata.isDefined) {
        val metadata = upsertion.metadata.get
        record.metadata.customer ==== metadata.customer
      }
    }

    def assertCalculations(
        order: Order,
        tipAmount: Option[BigDecimal],
        entity: PaymentIntent,
      ) = {
      val tip: BigDecimal = tipAmount.getOrElse(0)
      val total = order.total.get.amount.asRounded + tip

      entity.subtotal ==== order.subtotal.get.asRounded
      entity.tax ==== order.tax.get.asRounded
      entity.tip.amount ==== tip
      entity.total.amount ==== total
    }

    def orderItemWithTax(item: OrderItem): OrderItem = {
      val taxableAmount = item
        .calculatedPrice
        .map(_.amount)
        .getOrElse(BigDecimal(0)) * item.quantity.getOrElse(1)

      val itemTaxRates = taxRates.map { taxRate =>
        val totalAmount = taxableAmount * taxRate.value

        OrderItemTaxRate(
          id = UUID.randomUUID,
          taxRateId = Some(taxRate.id),
          name = taxRate.name,
          value = taxRate.value,
          totalAmount = Some(totalAmount),
          applyToPrice = taxRate.applyToPrice,
          active = true,
        )
      }

      val taxAmount =
        itemTaxRates.map(_.totalAmount.getOrElse(BigDecimal(0))).sum
      val totalAmount = taxableAmount + taxAmount

      item.copy(
        totalPrice = Some(MonetaryAmount(totalAmount, USD)),
        tax = Some(MonetaryAmount(taxAmount, USD)),
        taxRates = itemTaxRates,
      )
    }

    def orderWithItems(order: Order, items: Seq[OrderItem]): Order = {
      val totalAmount =
        items.map(_.totalPrice.map(_.amount).getOrElse(BigDecimal(0))).sum
      val taxAmount =
        items.map(_.tax.map(_.amount).getOrElse(BigDecimal(0))).sum
      val subtotalAmount = totalAmount - taxAmount

      val orderTaxRates = taxRates.map { taxRate =>
        val totalAmount =
          items
            .map(_.taxRates.filter(_.taxRateId == Some(taxRate.id)))
            .flatten
            .map(_.totalAmount.getOrElse(BigDecimal(0)))
            .sum

        OrderTaxRate(
          id = UUID.randomUUID,
          taxRateId = Some(taxRate.id),
          name = taxRate.name,
          value = taxRate.value,
          totalAmount = totalAmount,
        )
      }

      order.copy(
        items = items,
        taxRates = orderTaxRates,
        subtotal = Some(MonetaryAmount(subtotalAmount, USD)),
        tax = Some(MonetaryAmount(taxAmount, USD)),
        total = Some(MonetaryAmount(totalAmount, USD)),
      )

    }
  }
}
