package io.paytouch.core.services.ordertransitions

import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import java.time.{ ZoneId, ZonedDateTime }
import java.util.UUID

import io.paytouch.implicits._
import io.paytouch.core.RichZoneDateTime
import io.paytouch.core.data.model
import io.paytouch.core.services
import io.paytouch.core.utils.UtcTime
import Errors._

trait PaymentTransactionSubmitted
    extends LazyLogging
       with Computations.ComputePaymentStatus
       with Computations.ComputePaymentType
       with Computations.ComputeStatus
       with Computations.ComputeUpdatedAmounts {
  import PaymentTransactionSubmitted._

  def apply(
      order: model.OrderRecord,
      items: Seq[model.OrderItemRecord],
      transactions: Seq[model.PaymentTransactionRecord],
      tickets: Seq[model.TicketRecord],
      transactionUpsertion: services.OrderService.PaymentTransactionUpsertion,
      autoCompleteEnabled: Boolean,
      locationZoneId: ZoneId,
      onlineOrderAttribute: Option[model.OnlineOrderAttributeRecord],
    ): ErrorsAndResult[model.upsertions.OrderUpsertion] = {
    // always store payment transaction
    // trigger status transition and log stuff
    val resultUpdatedAmounts =
      computeUpdatedAmounts(order, transactions, transactionUpsertion)
    val resultPaymentStatus =
      computePaymentStatus(resultUpdatedAmounts.data, items, transactions, transactionUpsertion, onlineOrderAttribute)
    val resultStatus =
      computeStatus(resultPaymentStatus.data._1, tickets, autoCompleteEnabled, locationZoneId)
    val resultPaymentType =
      computePaymentType(resultStatus.data, transactions, transactionUpsertion)

    val newTransactionUpdate: model.PaymentTransactionUpdate =
      transactionUpsertion.toPaymentTransactionUpdate(
        merchantId = order.merchantId,
        orderId = order.id,
      )

    val errors = Seq(resultUpdatedAmounts, resultPaymentStatus, resultStatus, resultPaymentType)
      .map(_.errors)
      .foldLeft(Seq.empty[Errors.Error])(_ ++ _)

    val upsertion = model
      .upsertions
      .OrderUpsertion
      .empty(resultPaymentType.data.deriveUpdateFromPreviousState(order).getOrElse(model.OrderUpdate.empty(order.id)))
      .copy(
        canDeleteOrderItems = false, // just to be sure
        paymentTransactions = Seq(newTransactionUpdate),
        orderItems = items
          .zip(resultPaymentStatus.data._2)
          .map {
            case (item, updatedItem) =>
              model
                .upsertions
                .OrderItemUpsertion
                .empty(
                  updatedItem.deriveUpdateFromPreviousState(item),
                )
          },
        onlineOrderAttribute = (resultPaymentStatus.data._3, onlineOrderAttribute).mapN {
          (updatedOnlineAttribute, originalOnlineAttribute) =>
            updatedOnlineAttribute.deriveUpdateFromPreviousState(originalOnlineAttribute)
        },
      )

    Errors.ErrorsAndResult(errors, upsertion)
  }
}

object PaymentTransactionSubmitted extends PaymentTransactionSubmitted {
  case class UnexpectedNewTransactionForOrderInStatus(
      transactionUpsertionId: UUID,
      orderId: UUID,
      orderPaymentStatus: model.enums.PaymentStatus,
    ) extends Error {
    val message =
      s"Unexpected new transaction ${transactionUpsertionId} of type=payment for order ${orderId} in `payment_status=${orderPaymentStatus}`"
  }

  case class UnsupportedPaymentStatusTransition(
      orderId: UUID,
    ) extends Error {
    val message =
      s"Unsupported payment_status transitions with `transaction.payments_type` != `payment` for order ${orderId}!?"
  }

  case class OverpaidOrder(
      val transactionUpsertionId: UUID,
      val orderId: UUID,
      val amount: BigDecimal,
    ) extends Error {
    val message =
      s"Transaction ${transactionUpsertionId} over pays for order ${orderId} by ${amount}"
  }
}
