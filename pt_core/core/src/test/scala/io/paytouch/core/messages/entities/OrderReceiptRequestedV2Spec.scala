package io.paytouch.core.messages.entities

import io.paytouch.core.data.model.PaymentTransactionRecord
import io.paytouch.core.data.model.enums.TransactionType
import io.paytouch.core.expansions.{ MerchantExpansions, OrderExpansions }
import io.paytouch.core.services.ServiceDaoSpec
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class OrderReceiptRequestedV2Spec extends ServiceDaoSpec {

  abstract class OrderReceiptRequestedV2SpecContext extends ServiceDaoSpecContext with Fixtures {

    def assertMsg(msg: OrderReceiptRequestedV2, transactions: Seq[PaymentTransactionRecord]) = {
      val msgTransactionIds = msg.payload.data.order.paymentTransactions.getOrElse(Seq.empty).map(_.id)
      msgTransactionIds should containTheSameElementsAs(transactions.map(_.id))
    }

  }

  "OrderReceiptRequestedV2" should {
    "filter by a specific payment transaction" in new OrderReceiptRequestedV2SpecContext {
      val msg = OrderReceiptRequestedV2(
        orderEntity,
        Some(voidTransaction.id),
        email,
        merchantEntity,
        locationReceiptEntity,
        None,
        None,
      )
      assertMsg(msg, Seq(voidTransaction))
    }

    "filter void and refund transactions in order" in new OrderReceiptRequestedV2SpecContext {
      val msg = OrderReceiptRequestedV2(orderEntity, None, email, merchantEntity, locationReceiptEntity, None, None)
      assertMsg(msg, Seq(paymentTransactionA, paymentTransactionB))
    }
  }

  trait Fixtures { self: ServiceDaoSpecContext =>

    val globalCustomer = Factory.globalCustomer().create
    val customer = Factory.customerMerchant(merchant, globalCustomer).create
    Factory.locationReceipt(rome).create

    val order = Factory.order(merchant, location = Some(rome), customer = Some(customer)).create

    val orderItemA = Factory.orderItem(order).create
    val orderItemB = Factory.orderItem(order).create
    val orderItems = Seq(orderItemA, orderItemB)

    val voidTransaction = Factory.paymentTransaction(order, orderItems, `type` = Some(TransactionType.Void)).create
    val paymentTransactionA =
      Factory.paymentTransaction(order, Seq(orderItemA), `type` = Some(TransactionType.Payment)).create
    val paymentTransactionB =
      Factory.paymentTransaction(order, Seq(orderItemB), `type` = Some(TransactionType.Payment)).create
    val refundTransaction =
      Factory
        .paymentTransaction(
          order,
          Seq(orderItemB),
          `type` = Some(TransactionType.Refund),
          refundedPaymentTransaction = Some(paymentTransactionA),
        )
        .create

    val orderEntity = orderService.enrich(order, orderService.defaultFilters)(OrderExpansions.withFullOrderItems).await
    val merchantEntity = merchantService.findById(merchant.id)(MerchantExpansions.none).await.get
    val locationReceiptEntity = locationReceiptService.findByLocationId(rome.id).await.get
  }
}
