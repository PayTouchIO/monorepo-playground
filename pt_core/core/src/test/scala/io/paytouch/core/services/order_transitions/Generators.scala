package io.paytouch.core.services.ordertransitions

import cats.implicits._
import org.scalacheck._

import io.paytouch.implicits._
import io.paytouch.core.data.model
import io.paytouch.core.entities
import io.paytouch.core.services
import io.paytouch.core.utils.{ FixtureRandomGenerators, UtcTime }

object Generators extends FixtureRandomGenerators {
  def rounded(double: Double): BigDecimal = BigDecimal(double).setScale(2, BigDecimal.RoundingMode.HALF_UP)
  def rounded(bd: BigDecimal): BigDecimal = rounded(bd.toDouble)

  object Order {
    def genBase(genOrder: Gen[model.OrderRecord], genTransactionsSize: Gen[Int] = Gen.choose(0, 25)): Gen[
      (
          model.OrderRecord,
          Seq[model.PaymentTransactionRecord],
          io.paytouch.core.services.OrderService.PaymentTransactionUpsertion,
      ),
    ] =
      for {
        order <- genOrder
        transactionsSize <- genTransactionsSize.map(_ + 1) // adds one that will be transformed to Upsertion
        paymentTransactions <- Gen.containerOfN[Seq, model.PaymentTransactionRecord](
          transactionsSize,
          PaymentTransactions.genApprovedCardOrElsePayment,
        )
        totalAmountWithTip: Double =
          paymentTransactions.flatMap(_.paymentDetails.flatMap(_.amount.map(_.doubleValue))).sum
        tipAmount: Double = paymentTransactions.flatMap(_.paymentDetails.map(_.tipAmount.doubleValue)).sum
        totalAmountWithoutTip: Double = totalAmountWithTip - tipAmount
        subtotalAmount <- Gen.choose(0, totalAmountWithoutTip)
        taxAmount <- Gen.choose(0, (totalAmountWithoutTip - subtotalAmount))
        deliveryFeeAmount = totalAmountWithoutTip - subtotalAmount - taxAmount
        updatedOrder = order.copy(
          subtotalAmount = rounded(subtotalAmount).some,
          taxAmount = rounded(taxAmount).some,
          deliveryFeeAmount = rounded(deliveryFeeAmount).some,
          totalAmount = rounded(totalAmountWithTip).some,
          tipAmount = rounded(tipAmount).some,
        )
      } yield (updatedOrder, paymentTransactions.tail, toPaymentTransactionUpsertion(paymentTransactions.head))

    private def toPaymentTransactionUpsertion(
        transaction: model.PaymentTransactionRecord,
      ): services.OrderService.PaymentTransactionUpsertion =
      services
        .OrderService
        .PaymentTransactionUpsertion(
          id = transaction.id,
          `type` = transaction.`type`.get,
          paymentType = transaction.paymentType.get,
          paymentDetails = transaction.paymentDetails.get,
          paidAt = transaction.paidAt.getOrElse(UtcTime.now),
          version = transaction.version,
          paymentProcessor = transaction.paymentProcessor,
        )
  }
  object PaymentTransactions {
    @scala.annotation.nowarn("msg=Auto-application")
    val genBase: Gen[model.PaymentTransactionRecord] =
      for {
        paymentTransaction <- Arbitrary.arbitrary[model.PaymentTransactionRecord]
        paymentType <- Gen.some(genTransactionPaymentType)
        paymentDetails <- genPaymentDetails.map(pd =>
          pd.copy(
            amount = pd.amount.map(rounded),
            tipAmount = rounded(pd.tipAmount),
          ),
        )
      } yield paymentTransaction.copy(paymentType = paymentType, paymentDetails = paymentDetails.some)

    val genApprovedCardOrElsePayment: Gen[model.PaymentTransactionRecord] =
      genApprovedCardOrElsePaymentWithPaymentTypes(genTransactionPaymentType)

    def genApprovedCardOrElsePaymentWithPaymentTypes(
        _genTransactionPaymentType: Gen[model.enums.TransactionPaymentType],
      ): Gen[model.PaymentTransactionRecord] =
      for {
        paymentTransaction <- genBase
        paymentType <- _genTransactionPaymentType
        updatedPaymentDetails = paymentType match {
          case c if c.isCard =>
            paymentTransaction
              .paymentDetails
              .map(_.copy(transactionResult = model.enums.CardTransactionResultType.Approved.some))
          case _ => paymentTransaction.paymentDetails
        }
      } yield paymentTransaction.copy(
        `type` = model.enums.TransactionType.Payment.some,
        paymentType = paymentType.some,
        paymentDetails = updatedPaymentDetails,
      )

    val genNotApprovedCardOrElseAndNotPayment: Gen[model.PaymentTransactionRecord] =
      for {
        paymentTransaction <- genBase
        updatedPaymentDetails = paymentTransaction.paymentType match {
          case Some(c) if c.isCard =>
            paymentTransaction
              .paymentDetails
              .map(_.copy(transactionResult = model.enums.CardTransactionResultType.Declined.some))
          case _ => paymentTransaction.paymentDetails
        }
        invalidTransactionType <- Gen.oneOf(model.enums.TransactionType.values.filterNot(_.isPayment))
      } yield paymentTransaction.copy(`type` = invalidTransactionType.some, paymentDetails = updatedPaymentDetails)
  }

  object PaymentUpsertion {
    @scala.annotation.nowarn("msg=Auto-application")
    val genBase: Gen[services.OrderService.PaymentTransactionUpsertion] =
      for {
        upsertion <- Arbitrary.arbitrary[services.OrderService.PaymentTransactionUpsertion]
        paymentDetails <- genPaymentDetails
      } yield upsertion.copy(paymentDetails = paymentDetails)

    val genApprovedCardOrElsePayment: Gen[services.OrderService.PaymentTransactionUpsertion] =
      genApprovedCardOrElsePaymentWithPaymentTypes(genTransactionPaymentType)

    def genApprovedCardOrElsePaymentWithPaymentTypes(
        _genTransactionPaymentType: Gen[model.enums.TransactionPaymentType],
      ) =
      for {
        paymentTransaction <- genBase
        paymentType <- _genTransactionPaymentType
        updatedPaymentDetails = paymentType match {
          case c if c.isCard =>
            paymentTransaction
              .paymentDetails
              .copy(transactionResult = model.enums.CardTransactionResultType.Approved.some)
          case _ => paymentTransaction.paymentDetails
        }
      } yield paymentTransaction.copy(
        `type` = model.enums.TransactionType.Payment,
        paymentType = paymentType,
        paymentDetails = updatedPaymentDetails,
      )
  }

}
