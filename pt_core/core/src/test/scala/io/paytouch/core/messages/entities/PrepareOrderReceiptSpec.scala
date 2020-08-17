package io.paytouch.core.messages.entities

import io.paytouch.core.data.model.PaymentTransactionRecord
import io.paytouch.core.data.model.enums.TransactionType
import io.paytouch.core.expansions.OrderExpansions
import io.paytouch.core.services.ServiceDaoSpec
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class PrepareOrderReceiptSpec extends ServiceDaoSpec {

  abstract class PrepareOrderReceiptSpecContext extends ServiceDaoSpecContext with Fixtures {

    def assertMsg(
        msg: PrepareOrderReceipt,
        filter: Option[PaymentTransactionRecord],
        transactions: Seq[PaymentTransactionRecord],
      ) = {
      msg.payload.paymentTransactionId ==== filter.map(_.id)
      val msgTransactionIds = msg.payload.data.paymentTransactions.getOrElse(Seq.empty).map(_.id)
      msgTransactionIds should containTheSameElementsAs(transactions.map(_.id))
    }

  }

  "PrepareOrderReceipt" should {
    "filter by a specific payment transaction" in new PrepareOrderReceiptSpecContext {
      val msg = PrepareOrderReceipt(orderEntity, Some(voidTransaction.id), email)
      assertMsg(msg, Some(voidTransaction), Seq(voidTransaction))
    }

    "leave the order unfiltered if no specific payment transaction is wanted" in new PrepareOrderReceiptSpecContext {
      val msg = PrepareOrderReceipt(orderEntity, None, email)
      assertMsg(msg, None, Seq(voidTransaction, paymentTransactionA, paymentTransactionB, refundTransaction))
    }
  }

  trait Fixtures { self: ServiceDaoSpecContext =>

    val globalCustomer = Factory.globalCustomer().create
    val customer = Factory.customerMerchant(merchant, globalCustomer).create

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
  }
}
