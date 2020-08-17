package io.paytouch.core.services.ordertransitions

import java.time.{ ZoneId, ZonedDateTime }

import cats.implicits._

import org.scalacheck._
import org.scalacheck.Arbitrary.arbitrary

import io.paytouch.core.data.model
import io.paytouch.core.entities
import io.paytouch.core.services
import io.paytouch.core.utils._

@scala.annotation.nowarn("msg=Auto-application")
class PaymentTransactionSubmittedSpec extends PaytouchSpec {
  "PaymentTransactionSubmitted" should {
    "call all computations in order" in {
      import PaymentTransactionSubmitted._
      case class ErrorSpy(val message: String) extends Errors.Error
      val computePaymentStatusErrorSpy = ErrorSpy("computePaymentStatus")
      val computeStatusErrorSpy = ErrorSpy("computeStatus")
      val computePaymentTypeErrorSpy = ErrorSpy("computePaymentType")
      val computeTipAmountErrorSpy = ErrorSpy("computeTipAmount")

      def toExpectedTransactionUpdate(
          transactionUpsertion: services.OrderService.PaymentTransactionUpsertion,
          order: model.OrderRecord,
        ) =
        model.PaymentTransactionUpdate(
          id = transactionUpsertion.id.some,
          merchantId = order.merchantId.some,
          orderId = order.id.some,
          customerId = None,
          `type` = transactionUpsertion.`type`.some,
          refundedPaymentTransactionId = None,
          paymentType = transactionUpsertion.paymentType.some,
          paymentDetails = transactionUpsertion.paymentDetails.some,
          version = transactionUpsertion.version.some,
          paidAt = transactionUpsertion.paidAt.some,
          paymentProcessor = transactionUpsertion.paymentProcessor.some,
        )

      val subject = new PaymentTransactionSubmitted {
        override def computePaymentStatus(
            order: model.OrderRecord,
            items: Seq[model.OrderItemRecord],
            transactions: Seq[model.PaymentTransactionRecord],
            transactionUpsertion: services.OrderService.PaymentTransactionUpsertion,
            onlineOrderAttribute: Option[model.OnlineOrderAttributeRecord],
          ): Errors.ErrorsAndResult[
          (model.OrderRecord, Seq[model.OrderItemRecord], Option[model.OnlineOrderAttributeRecord]),
        ] = {
          val pickValueToTriggerAnUpdate = model.enums.PaymentStatus.values.find(_.some != order.paymentStatus)
          Errors.ErrorsAndResult(
            Seq(computePaymentStatusErrorSpy),
            (order.copy(paymentStatus = pickValueToTriggerAnUpdate), items, onlineOrderAttribute),
          )
        }

        override def computeStatus(
            order: model.OrderRecord,
            tickets: Seq[model.TicketRecord],
            isAutocompleteEnabled: Boolean,
            locationZoneId: ZoneId,
          ): Errors.ErrorsAndResult[model.OrderRecord] = {
          val pickValueToTriggerAnUpdate = model.enums.OrderStatus.values.find(_.some != order.status)
          Errors.ErrorsAndResult(Seq(computeStatusErrorSpy), order.copy(status = pickValueToTriggerAnUpdate))
        }

        override def computePaymentType(
            order: model.OrderRecord,
            transactions: Seq[model.PaymentTransactionRecord],
            transactionUpsertion: services.OrderService.PaymentTransactionUpsertion,
          ): Errors.ErrorsAndResult[model.OrderRecord] = {
          val pickValueToTriggerAnUpdate = model.enums.OrderPaymentType.values.find(_.some != order.paymentType)
          Errors.ErrorsAndResult(Seq(computePaymentTypeErrorSpy), order.copy(paymentType = pickValueToTriggerAnUpdate))
        }

        override def computeUpdatedAmounts(
            order: model.OrderRecord,
            transactions: Seq[model.PaymentTransactionRecord],
            transactionUpsertion: services.OrderService.PaymentTransactionUpsertion,
          ): Errors.ErrorsAndResult[model.OrderRecord] = {
          val pickValueToTriggerAnUpdateTip = order.tipAmount.getOrElse[BigDecimal](0) + genBigDecimal.instance
          val pickValueToTriggerAnUpdateTotal = order.totalAmount.getOrElse[BigDecimal](0) + genBigDecimal.instance
          Errors.ErrorsAndResult(
            Seq(computeTipAmountErrorSpy),
            order.copy(
              tipAmount = pickValueToTriggerAnUpdateTip.some,
              totalAmount = pickValueToTriggerAnUpdateTotal.some,
            ),
          )
        }
      }

      prop {
        (
            order: model.OrderRecord,
            items: Seq[model.OrderItemRecord],
            transactions: Seq[model.PaymentTransactionRecord],
            tickets: Seq[model.TicketRecord],
            transactionUpsertion: services.OrderService.PaymentTransactionUpsertion,
            autoCompleteEnabled: Boolean,
            locationZoneId: ZoneId,
        ) =>
          val errorAndResult =
            subject(
              order,
              items,
              transactions,
              tickets,
              transactionUpsertion,
              autoCompleteEnabled,
              locationZoneId,
              None,
            )

          errorAndResult.errors ==== Seq(
            computeTipAmountErrorSpy,
            computePaymentStatusErrorSpy,
            computeStatusErrorSpy,
            computePaymentTypeErrorSpy,
          )
          val result = errorAndResult.data
          result.order.paymentStatus must beSome
          result.order.status must beSome
          result.order.paymentType must beSome
          result.order.tipAmount must beSome
          result.order.totalAmount must beSome
          result.paymentTransactions ==== Seq(toExpectedTransactionUpdate(transactionUpsertion, order))
      }
    }
  }
}
