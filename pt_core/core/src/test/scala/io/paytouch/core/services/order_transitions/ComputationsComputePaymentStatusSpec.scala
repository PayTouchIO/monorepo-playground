package io.paytouch.core.services.ordertransitions

import java.time.{ ZoneId, ZonedDateTime }

import cats.implicits._

import org.scalacheck._
import org.scalacheck.Arbitrary.arbitrary

import io.paytouch.core.data.model
import io.paytouch.core.entities
import io.paytouch.core.services.OrderService
import io.paytouch.core.utils.PaytouchSpec

@scala.annotation.nowarn("msg=Auto-application")
class ComputationsComputePaymentStatusSpec extends PaytouchSpec {
  "Computations.ComputePaymentStatus" should {
    val subject = new Computations.ComputePaymentStatus {}

    val neverBeforeFullyPaidOrderPaymentStatuses: List[Option[model.enums.PaymentStatus]] = List(
      None,
      model.enums.PaymentStatus.Pending.some,
      model.enums.PaymentStatus.PartiallyPaid.some,
    )
    val genNeverCompletelyPaidOrder: Gen[model.OrderRecord] = {
      for {
        order <- arbitrary[model.OrderRecord]
        paymentStatus <- Gen.oneOf(neverBeforeFullyPaidOrderPaymentStatuses)
      } yield order.copy(paymentStatus = paymentStatus)
    }
    val genPaidOrOtherNonPendingPaymentStatuesOrder: Gen[model.OrderRecord] = {
      for {
        order <- arbitrary[model.OrderRecord]
        paymentStatus <- Gen.oneOf(
          model.enums.PaymentStatus.values.filterNot(ps => neverBeforeFullyPaidOrderPaymentStatuses.contains(ps.some)),
        )
      } yield order.copy(paymentStatus = paymentStatus.some)
    }

    "if payment transaction upsertion is a payment" in {
      "if no previous transactions" in {
        val genTransactionsSize: Gen[Int] = Gen.const(0)
        "if order was never completely paid before (payment status in none/pending/partially paid" in {
          "if payment transaction total is equal to order total" in {
            "mark order and items as paid" in {
              prop {
                (
                    data: (
                        model.OrderRecord,
                        Seq[model.PaymentTransactionRecord],
                        OrderService.PaymentTransactionUpsertion,
                    ),
                    items: Seq[model.OrderItemRecord],
                    transactions: Seq[model.PaymentTransactionRecord],
                ) =>
                  val (order, transactions, transactionUpsertion) = data
                  val errorAndResult = subject.computePaymentStatus(
                    order,
                    items,
                    transactions,
                    transactionUpsertion,
                    onlineOrderAttribute = None,
                  )
                  errorAndResult.errors should beEmpty
                  errorAndResult.data ==== (
                    order.copy(paymentStatus = model.enums.PaymentStatus.Paid.some),
                    items.map(_.copy(paymentStatus = model.enums.PaymentStatus.Paid.some)),
                    None
                  )
              }.setGen1(
                Generators.Order.genBase(genNeverCompletelyPaidOrder, genTransactionsSize = genTransactionsSize),
              )
            }
          }
          "if payment transaction total is less than order total" in {
            "mark order and items as partially paid" in {
              prop {
                (
                    data: (
                        model.OrderRecord,
                        Seq[model.PaymentTransactionRecord],
                        OrderService.PaymentTransactionUpsertion,
                    ),
                    items: Seq[model.OrderItemRecord],
                ) =>
                  val (order, transactions, transactionUpsertion) = data
                  val errorAndResult = subject.computePaymentStatus(
                    order,
                    items,
                    transactions,
                    transactionUpsertion.copy(paymentDetails =
                      transactionUpsertion
                        .paymentDetails
                        .copy(amount = transactionUpsertion.paymentDetails.amount.map(_ - 5)),
                    ),
                    onlineOrderAttribute = None,
                  )
                  errorAndResult.errors should beEmpty
                  errorAndResult.data ==== (
                    order.copy(paymentStatus = model.enums.PaymentStatus.PartiallyPaid.some),
                    items.map(_.copy(paymentStatus = model.enums.PaymentStatus.PartiallyPaid.some)),
                    None,
                  )
              }.setGen1(
                Generators.Order.genBase(genNeverCompletelyPaidOrder, genTransactionsSize = genTransactionsSize),
              )
            }
          }
          "if payment transaction total is more than order total" in {
            "mark order and items as paid" in {
              prop {
                (
                    data: (
                        model.OrderRecord,
                        Seq[model.PaymentTransactionRecord],
                        OrderService.PaymentTransactionUpsertion,
                    ),
                    items: Seq[model.OrderItemRecord],
                ) =>
                  val (order, transactions, transactionUpsertion) = data
                  val errorAndResult = subject.computePaymentStatus(
                    order,
                    items,
                    transactions,
                    transactionUpsertion.copy(paymentDetails =
                      transactionUpsertion
                        .paymentDetails
                        .copy(amount = transactionUpsertion.paymentDetails.amount.map(_ + 5)),
                    ),
                    onlineOrderAttribute = None,
                  )
                  errorAndResult.errors ==== Seq(
                    PaymentTransactionSubmitted.OverpaidOrder(transactionUpsertion.id, order.id, 5),
                  )
                  errorAndResult.data ==== (
                    order.copy(paymentStatus = model.enums.PaymentStatus.Paid.some),
                    items.map(_.copy(paymentStatus = model.enums.PaymentStatus.Paid.some)),
                    None,
                  )
              }.setGen1(
                Generators.Order.genBase(genNeverCompletelyPaidOrder, genTransactionsSize = genTransactionsSize),
              )
            }
          }
        }

        "if order payment status is not in none/pending/partially paid" in {
          "store the transaction but should not change anything" in {
            prop {
              (
                  data: (
                      model.OrderRecord,
                      Seq[model.PaymentTransactionRecord],
                      OrderService.PaymentTransactionUpsertion,
                  ),
                  items: Seq[model.OrderItemRecord],
              ) =>
                val (order, transactions, transactionUpsertion) = data
                val errorAndResult = subject.computePaymentStatus(
                  order,
                  items,
                  Seq.empty,
                  transactionUpsertion,
                  onlineOrderAttribute = None,
                )

                errorAndResult.errors should contain(
                  PaymentTransactionSubmitted
                    .UnexpectedNewTransactionForOrderInStatus(
                      transactionUpsertion.id,
                      order.id,
                      order.paymentStatus.get,
                    ),
                )

                errorAndResult.data ==== (order, items, None)
            }.setGen1(
              Generators
                .Order
                .genBase(genPaidOrOtherNonPendingPaymentStatuesOrder, genTransactionsSize = genTransactionsSize),
            )
          }
        }
      }

      "if there are previous transactions" in {
        val genTransactionsSize = Gen.choose(1, 25)
        "if order was never completely paid before (payment status in none/pending/partially paid" in {
          "if payment transaction total is equal to order total" in {
            "mark order and items as paidz" in {
              prop {
                (
                    data: (
                        model.OrderRecord,
                        Seq[model.PaymentTransactionRecord],
                        OrderService.PaymentTransactionUpsertion,
                    ),
                    items: Seq[model.OrderItemRecord],
                ) =>
                  val (order, transactions, transactionUpsertion) = data
                  val errorAndResult = subject.computePaymentStatus(
                    order,
                    items,
                    transactions,
                    transactionUpsertion,
                    onlineOrderAttribute = None,
                  )
                  errorAndResult.errors should beEmpty
                  errorAndResult.data ==== (
                    order.copy(paymentStatus = model.enums.PaymentStatus.Paid.some),
                    items.map(_.copy(paymentStatus = model.enums.PaymentStatus.Paid.some)),
                    None
                  )
              }.setGen1(
                Generators.Order.genBase(genNeverCompletelyPaidOrder, genTransactionsSize = genTransactionsSize),
              )
            }
          }
          "if payment transaction total is less than order total" in {
            "mark order and items as partially paid" in {
              prop {
                (
                    data: (
                        model.OrderRecord,
                        Seq[model.PaymentTransactionRecord],
                        OrderService.PaymentTransactionUpsertion,
                    ),
                    items: Seq[model.OrderItemRecord],
                ) =>
                  val (order, transactions, transactionUpsertion) = data
                  val reducedTransactionsToForcePartialStatus = transactions.tail
                  val errorAndResult = subject.computePaymentStatus(
                    order,
                    items,
                    reducedTransactionsToForcePartialStatus,
                    transactionUpsertion,
                    onlineOrderAttribute = None,
                  )
                  errorAndResult.errors should beEmpty
                  errorAndResult.data ==== (
                    order.copy(paymentStatus = model.enums.PaymentStatus.PartiallyPaid.some),
                    items.map(_.copy(paymentStatus = model.enums.PaymentStatus.PartiallyPaid.some)),
                    None
                  )
              }.setGen1(
                Generators.Order.genBase(genNeverCompletelyPaidOrder, genTransactionsSize = genTransactionsSize),
              )
            }
          }
          "if payment transaction total is more than order total" in {
            "mark order and items as paid" in
              prop {
                (
                    data: (
                        model.OrderRecord,
                        Seq[model.PaymentTransactionRecord],
                        OrderService.PaymentTransactionUpsertion,
                    ),
                    items: Seq[model.OrderItemRecord],
                ) =>
                  val (order, transactions, transactionUpsertion) = data
                  val errorAndResult = subject.computePaymentStatus(
                    order,
                    items,
                    transactions,
                    transactionUpsertion.copy(paymentDetails =
                      transactionUpsertion
                        .paymentDetails
                        .copy(amount = transactionUpsertion.paymentDetails.amount.map(_ + 5)),
                    ),
                    onlineOrderAttribute = None,
                  )
                  errorAndResult.errors ==== Seq(
                    PaymentTransactionSubmitted.OverpaidOrder(transactionUpsertion.id, order.id, 5),
                  )
                  errorAndResult.data ==== (
                    order.copy(paymentStatus = model.enums.PaymentStatus.Paid.some),
                    items.map(_.copy(paymentStatus = model.enums.PaymentStatus.Paid.some)),
                    None
                  )
              }.setGen1(
                Generators.Order.genBase(genNeverCompletelyPaidOrder, genTransactionsSize = genTransactionsSize),
              )
          }
        }
      }

      "if payment transaction upsertion is NOT a payment" in {
        "store the transaction but should not change anything" in {
          prop {
            (
                data: (
                    model.OrderRecord,
                    Seq[model.PaymentTransactionRecord],
                    OrderService.PaymentTransactionUpsertion,
                ),
                transactionType: model.enums.TransactionType,
                items: Seq[model.OrderItemRecord],
            ) =>
              val (order, transactions, transactionUpsertion) = data
              val errorAndResult = subject.computePaymentStatus(
                order,
                items,
                Seq.empty,
                transactionUpsertion.copy(`type` = transactionType),
                onlineOrderAttribute = None,
              )

              errorAndResult.errors should contain(
                PaymentTransactionSubmitted
                  .UnsupportedPaymentStatusTransition(order.id),
              )

              errorAndResult.data ==== (order, items, None)
          }.setGen1(Generators.Order.genBase(genPaidOrOtherNonPendingPaymentStatuesOrder))
            .setGen2(Gen.oneOf(model.enums.TransactionType.values.filterNot(_.isPayment)))
        }
      }
    }
  }
}
