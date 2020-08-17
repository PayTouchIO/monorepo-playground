package io.paytouch.core.services.ordertransitions

import java.time.{ ZoneId, ZonedDateTime }

import cats.implicits._

import com.typesafe.scalalogging.LazyLogging

import org.scalacheck._
import org.scalacheck.Arbitrary.arbitrary

import io.paytouch.core.data.model
import io.paytouch.core.entities
import io.paytouch.core.services
import io.paytouch.core.utils.PaytouchSpec

@scala.annotation.nowarn("msg=Auto-application")
class ComputationsComputePaymentTypeSpec extends PaytouchSpec {
  // downgrade to non-implicit
  override val arbitraryTransactionPaymentType: Arbitrary[model.enums.TransactionPaymentType] = Arbitrary(
    genTransactionPaymentType,
  )

  "Computations.ComputePaymentType" should {
    val subject = new Computations.ComputePaymentType {}

    "if order has no previous transactions" in {
      "set equal incoming payment transaction type" in {
        prop {
          (
              order: model.OrderRecord,
              transactionUpsertion: services.OrderService.PaymentTransactionUpsertion,
          ) =>
            val errorAndResult = subject.computePaymentType(order, Seq.empty, transactionUpsertion)
            errorAndResult.errors must beEmpty
            errorAndResult.data.paymentType ==== transactionUpsertion.paymentType.toOrderPaymentType.some
        }.setGen2(Generators.PaymentUpsertion.genApprovedCardOrElsePayment)
      }
    }

    "if order previous transactions" in {
      "if all transactions are of the same type" in {
        "set equal to unique payment transaction type " in {
          val genUpsertionWithSamePaymentType = Generators
            .PaymentUpsertion
            .genApprovedCardOrElsePayment
            .map(_.copy(paymentType = model.enums.TransactionPaymentType.Cash))
          val genTransactionsWithSamePaymentType = Gen.nonEmptyContainerOf[Seq, model.PaymentTransactionRecord](
            Generators
              .PaymentTransactions
              .genApprovedCardOrElsePayment
              .map(_.copy(paymentType = model.enums.TransactionPaymentType.Cash.some)),
          )

          prop {
            (
                order: model.OrderRecord,
                transactions: Seq[model.PaymentTransactionRecord],
                transactionUpsertion: services.OrderService.PaymentTransactionUpsertion,
            ) =>
              val errorAndResult = subject.computePaymentType(order, transactions, transactionUpsertion)
              errorAndResult.errors must beEmpty
              errorAndResult.data.paymentType ==== transactionUpsertion.paymentType.toOrderPaymentType.some
          }.setGen2(genTransactionsWithSamePaymentType).setGen3(genUpsertionWithSamePaymentType)
        }
      }
      "if all transactions are of many different type" in {
        def genNonEmptyPaymentTransactions(
            genPaymentTypes: Gen[model.enums.TransactionPaymentType],
          ): Gen[Seq[model.PaymentTransactionRecord]] =
          Gen.nonEmptyContainerOf[Seq, model.PaymentTransactionRecord](
            Generators.PaymentTransactions.genApprovedCardOrElsePaymentWithPaymentTypes(genPaymentTypes),
          )
        implicit val arbTransactionAndUpsertions
            : Arbitrary[(Seq[model.PaymentTransactionRecord], services.OrderService.PaymentTransactionUpsertion)] =
          Arbitrary(
            for {
              upsertion <- Generators.PaymentUpsertion.genApprovedCardOrElsePayment
              paymentTypesDifferentFromUpsertion =
                Gen.oneOf(model.enums.TransactionPaymentType.values.filterNot(_ == upsertion.paymentType))
              paymentTransactions <- genNonEmptyPaymentTransactions(paymentTypesDifferentFromUpsertion)
            } yield (paymentTransactions, upsertion),
          )

        "set equal to Split" in {
          prop {
            (
                order: model.OrderRecord,
                transactionsAndUpsertion: (
                    Seq[model.PaymentTransactionRecord],
                    services.OrderService.PaymentTransactionUpsertion,
                ),
            ) =>
              val (transactions, transactionUpsertion) = transactionsAndUpsertion
              val errorAndResult = subject.computePaymentType(order, transactions, transactionUpsertion)
              errorAndResult.errors must beEmpty
              errorAndResult.data.paymentType ==== model.enums.OrderPaymentType.Split.some
          }
        }
      }
    }
  }
}
