package io.paytouch.core.services.ordertransitions

import cats.implicits._
import cats.data._
import com.typesafe.scalalogging.LazyLogging
import java.time.{ ZoneId, ZonedDateTime }
import java.util.UUID

import io.paytouch.implicits._
import io.paytouch.core.RichZoneDateTime
import io.paytouch.core.data.model
import io.paytouch.core.services
import io.paytouch.core.utils.UtcTime

object Computations {
  import PaymentTransactionSubmitted._

  trait ComputePaymentStatus {

    /**
      * - currently in Register it's not possible to end up with a partially paid status as the split flow forces to complete the split
      * - order: total = subtotal + tax + tip + delivery fee
      * - transaction amount contains tips
      * - transaction with tips update tip and total
      */
    def computePaymentStatus(
        order: model.OrderRecord,
        items: Seq[model.OrderItemRecord],
        transactions: Seq[model.PaymentTransactionRecord],
        transactionUpsertion: services.OrderService.PaymentTransactionUpsertion,
        onlineOrderAttribute: Option[model.OnlineOrderAttributeRecord],
      ): Errors.ErrorsAndResult[
      (model.OrderRecord, Seq[model.OrderItemRecord], Option[model.OnlineOrderAttributeRecord]),
    ] = {
      def unchangedReturn(error: Errors.Error) =
        Errors.ErrorsAndResult(Seq(error), (order, items, onlineOrderAttribute))
      if (transactionUpsertion.`type`.isPayment)
        order.paymentStatus match {
          case None | Some(model.enums.PaymentStatus.Pending) | Some(model.enums.PaymentStatus.PartiallyPaid) =>
            val transactionToConsider = filterApprovedPaymentTransaction(
              mergeUpsertionAndTransactions(transactionUpsertion, transactions),
            )
            val totalPaid: BigDecimal = transactionToConsider
              .flatMap(_.paymentDetails.map(_.amount.getOrElse[BigDecimal](0)))
              .sum
            val targetStatus: Errors.ErrorsAndResult[model.enums.PaymentStatus] =
              (order.totalAmount.getOrElse[BigDecimal](0) - totalPaid) match {
                case t if t > 0  => Errors.ErrorsAndResult.noErrors(model.enums.PaymentStatus.PartiallyPaid)
                case t if t == 0 => Errors.ErrorsAndResult.noErrors(model.enums.PaymentStatus.Paid)
                case t if t < 0 =>
                  Errors.ErrorsAndResult(
                    Seq(OverpaidOrder(transactionUpsertion.id, order.id, t.abs)),
                    model.enums.PaymentStatus.Paid,
                  )
              }

            val updatedOnlineOrderAttribute = onlineOrderAttribute.map { oos =>
              val onlineOrderAttributeTargetStatus: model.enums.AcceptanceStatus = targetStatus.data match {
                case model.enums.PaymentStatus.Paid => model.enums.AcceptanceStatus.Pending
                case _                              => oos.acceptanceStatus
              }
              oos.copy(acceptanceStatus = onlineOrderAttributeTargetStatus)
            }
            Errors.ErrorsAndResult(
              targetStatus.errors,
              (
                order.copy(paymentStatus = targetStatus.data.some),
                items.map(_.copy(paymentStatus = targetStatus.data.some)),
                updatedOnlineOrderAttribute,
              ),
            )
          case Some(paymentStatus) =>
            unchangedReturn(UnexpectedNewTransactionForOrderInStatus(transactionUpsertion.id, order.id, paymentStatus))
        }
      else unchangedReturn(UnsupportedPaymentStatusTransition(order.id))
    }
  }

  trait ComputeStatus { self: LazyLogging =>
    def computeStatus(
        order: model.OrderRecord,
        tickets: Seq[model.TicketRecord],
        isAutocompleteEnabled: Boolean,
        locationZoneId: ZoneId,
      ): Errors.ErrorsAndResult[model.OrderRecord] = {
      logger.debug(
        s"[AUTOCOMPLETION] ORDER ID ${order.id} TICKETS ${tickets.map(t => t.id -> t.status).mkString(", ")}",
      )

      val shouldMarkOrderAsCompleted: Boolean = {
        val areAllTicketsCompleted =
          tickets.forall(_.hasCompleted)

        isAutocompleteEnabled &&
        areAllTicketsCompleted &&
        order.paymentStatus.exists(_.isPositive) &&
        !order.status.contains(model.enums.OrderStatus.Completed)
      }

      lazy val shouldMarkOrderAsInProgress: Boolean = {
        val areSomeTicketsInProgress =
          tickets.exists(_.isNewOrInProgress)

        areSomeTicketsInProgress &&
        !order.status.contains(model.enums.OrderStatus.InProgress)
      }

      if (shouldMarkOrderAsCompleted) {
        logger.debug(s"[AUTOCOMPLETION] SETTING ORDER ${order.id} AS COMPLETE")

        val completedAt = UtcTime.now
        Errors
          .ErrorsAndResult
          .noErrors(
            order.copy(
              status = model.enums.OrderStatus.Completed.some,
              completedAt = completedAt.some,
              completedAtTz = completedAt.toLocationTimezone(locationZoneId).some,
            ),
          )
      }
      else if (shouldMarkOrderAsInProgress) {
        logger.debug(s"[AUTOCOMPLETION] SETTING ORDER ${order.id} AS IN PROGRESS")
        Errors.ErrorsAndResult.noErrors(order.copy(status = model.enums.OrderStatus.InProgress.some))
      }
      else
        Errors.ErrorsAndResult.noErrors(order)
    }
  }

  trait ComputePaymentType extends Utils.PaymentTransactions {
    def computePaymentType(
        order: model.OrderRecord,
        transactions: Seq[model.PaymentTransactionRecord],
        transactionUpsertion: services.OrderService.PaymentTransactionUpsertion,
      ): Errors.ErrorsAndResult[model.OrderRecord] = {
      val involvedPaymentTypes: Set[model.enums.TransactionPaymentType] =
        filterApprovedPaymentTransaction(mergeUpsertionAndTransactions(transactionUpsertion, transactions))
          .flatMap(_.paymentType)
          .toSet
      val orderPaymentType: Option[model.enums.OrderPaymentType] = involvedPaymentTypes.toList match {
        case Nil                      => None
        case singlePaymentType :: Nil => singlePaymentType.toOrderPaymentType.some
        case _                        => model.enums.OrderPaymentType.Split.some
      }
      Errors.ErrorsAndResult.noErrors(order.copy(paymentType = orderPaymentType))
    }
  }

  trait ComputeUpdatedAmounts extends Utils.PaymentTransactions {
    def computeUpdatedAmounts(
        order: model.OrderRecord,
        transactions: Seq[model.PaymentTransactionRecord],
        transactionUpsertion: services.OrderService.PaymentTransactionUpsertion,
      ): Errors.ErrorsAndResult[model.OrderRecord] = {
      val tipAmount: BigDecimal =
        filterApprovedPaymentTransaction(mergeUpsertionAndTransactions(transactionUpsertion, transactions))
          .map(_.paymentDetails.map(_.tipAmount).getOrElse[BigDecimal](0))
          .sum
      val totalAmount: BigDecimal =
        order.subtotalAmount.getOrElse[BigDecimal](0) +
          order.deliveryFeeAmount.getOrElse[BigDecimal](0) +
          order.taxAmount.getOrElse[BigDecimal](0) +
          tipAmount
      Errors.ErrorsAndResult.noErrors(order.copy(tipAmount = tipAmount.some, totalAmount = totalAmount.some))
    }
  }

  object Utils {
    trait PaymentTransactions {
      def filterApprovedPaymentTransaction(
          transactions: Seq[model.PaymentTransactionRecord],
        ): Seq[model.PaymentTransactionRecord] =
        transactions.filter { transaction =>
          val isCard: Boolean = transaction.paymentType.exists(_.isCard)
          val isApproved: Boolean = transaction
            .paymentDetails
            .exists(
              _.transactionResult
                .contains(model.enums.CardTransactionResultType.Approved),
            )

          val isPayment: Boolean = transaction.`type`.contains(model.enums.TransactionType.Payment)
          isPayment && (!isCard || (isCard && isApproved))
        }

      def mergeUpsertionAndTransactions(
          upsertion: services.OrderService.PaymentTransactionUpsertion,
          transactions: Seq[model.PaymentTransactionRecord],
        ): Seq[model.PaymentTransactionRecord] =
        transactions ++ Seq(toPaymentTransactionRecord(upsertion))

      def toPaymentTransactionRecord(
          upsertion: services.OrderService.PaymentTransactionUpsertion,
        ): model.PaymentTransactionRecord =
        model.PaymentTransactionRecord(
          id = upsertion.id,
          merchantId = UUID.randomUUID,
          orderId = UUID.randomUUID,
          customerId = none,
          `type` = upsertion.`type`.some,
          refundedPaymentTransactionId = none,
          paymentType = upsertion.paymentType.some,
          paymentDetails = upsertion.paymentDetails.some,
          version = upsertion.version,
          paidAt = upsertion.paidAt.some,
          paymentProcessor = upsertion.paymentProcessor,
          createdAt = UtcTime.now,
          updatedAt = UtcTime.now,
        )
    }
    object PaymentTransactions extends PaymentTransactions
  }
}
