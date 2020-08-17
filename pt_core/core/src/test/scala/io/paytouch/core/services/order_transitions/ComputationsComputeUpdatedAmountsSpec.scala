package io.paytouch.core.services.ordertransitions

import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import java.time.{ ZoneId, ZonedDateTime }

import io.paytouch.core.data.model
import io.paytouch.core.entities
import io.paytouch.core.services
import io.paytouch.core.utils.PaytouchSpec
import org.scalacheck._
import Arbitrary.arbitrary

@scala.annotation.nowarn("msg=Auto-application")
class ComputationsComputeUpdatedAmountsSpec extends PaytouchSpec {
  "Computations.ComputeUpdatedAmounts" should {
    val subject = new Computations.ComputeUpdatedAmounts {}
    import Computations.Utils.PaymentTransactions._

    "update amounts based on tips from payment transactions and upsertion" in {
      prop {
        (
            order: model.OrderRecord,
            transactions: Seq[model.PaymentTransactionRecord],
            transactionUpsertion: services.OrderService.PaymentTransactionUpsertion,
        ) =>
          val mergedTransactions = mergeUpsertionAndTransactions(transactionUpsertion, transactions)

          val errorAndResult = subject.computeUpdatedAmounts(order, transactions, transactionUpsertion)
          errorAndResult.errors must beEmpty
          // TODO: this is not ideal because we are duplicating business logic code
          // the right plan is generating order/transactions/upsertions in a way where totals add up so this prop can be written in terms of previous state of record
          // expectedTipAmount = order.tipAmount + transactionUpsertion.tipApmount
          // expectedTotalAmount = order.totalAmount + transactionUpsertion.tipApmount
          val expectedTipAmount: BigDecimal =
            mergedTransactions.map(_.paymentDetails.map(_.tipAmount).getOrElse[BigDecimal](0)).sum
          val expectedTotalAmount: BigDecimal =
            order.subtotalAmount.getOrElse[BigDecimal](0) +
              order.deliveryFeeAmount.getOrElse[BigDecimal](0) +
              order.taxAmount.getOrElse[BigDecimal](0) +
              expectedTipAmount
          val result = errorAndResult.data
          result.tipAmount ==== expectedTipAmount.some
          result.totalAmount ==== expectedTotalAmount.some
      }.setGen2(
        Gen.nonEmptyContainerOf[Seq, model.PaymentTransactionRecord](
          Generators.PaymentTransactions.genApprovedCardOrElsePayment,
        ),
      ).setGen3(Generators.PaymentUpsertion.genApprovedCardOrElsePayment)
    }
  }
}
