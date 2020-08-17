package io.paytouch.ordering.conversions

import java.util.{ Currency, UUID }

import cats.implicits._

import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.clients.paytouch.core.entities.enums._
import io.paytouch.ordering.data.model.{ CartRecord, JetdirectConfig }
import io.paytouch.ordering.jetdirect.JetdirectEncodings
import io.paytouch.ordering.entities.{ Cart, PaymentProcessorData }
import io.paytouch.ordering.entities.enums.PaymentProcessor
import io.paytouch.ordering.entities.jetdirect.CallbackPayload
import io.paytouch.ordering.utils.UtcTime

trait JetdirectConversions extends JetdirectEncodings {

  protected def fromCartRecordToPaymentProcessorData(
      cartRecord: CartRecord,
    )(implicit
      jetDirectConfig: JetdirectConfig,
    ): PaymentProcessorData = {
    val jetDirectReference = cartRecord.id.toString
    val hashCode = calculateJetdirectHashCode(jetDirectReference, cartRecord.totalAmount.toString)
    PaymentProcessorData.empty.copy(reference = Some(jetDirectReference), hashCodeValue = Some(hashCode))
  }

  protected def addPaymentTransactionToOrderUpsertion(payload: CallbackPayload) =
    (cart: Cart, orderUpsertion: OrderUpsertion) => {
      val transaction =
        PaymentTransactionUpsertion(
          id = UUID.randomUUID,
          `type` = TransactionType.Payment,
          paymentProcessorV2 = PaymentProcessor.Jetdirect,
          paymentType = TransactionPaymentType.CreditCard,
          paymentDetails = GenericPaymentDetails(
            amount = payload.amount,
            currency = Currency.getInstance("USD"),
            transactionResult = CardTransactionResultType.fromPaymentProcessorCallbackStatus(payload.status).some,
            transactionStatus = CardTransactionStatusType.Committed.some,
            tipAmount = cart.tip.amount,
            authCode = payload.ccToken,
            maskPan = payload.expandedCardNum,
            last4Digits = payload.cardNum,
            cardHash = payload.ccToken,
            cardReference = payload.uniqueid,
            cardType = payload.card,
            terminalName = Some("jetDirect"),
            transactionReference =
              payload.transId, // This value is the transaction id assigned by Jetdirect for this payment attempt
            gatewayTransactionReference =
              payload.uniqueid, // This value is a unique index reference that directly references the specific transaction at JetPay. This id can be used in place of a card number or Safe Token when doing VOID and CREDIT transaction requests via XML.
          ),
          paidAt = UtcTime.now,
          fees = Seq(
            PaymentTransactionFeeUpsertion(
              id = UUID.randomUUID,
              name = "Price Adj",
              `type` = PaymentTransactionFeeType.JetpayFee,
              amount = payload.feeAmount.getOrElse(0),
            ),
            PaymentTransactionFeeUpsertion(
              id = UUID.randomUUID,
              name = "Cash Discount",
              `type` = PaymentTransactionFeeType.JetpayCashDiscount,
              amount = 0,
            ),
          ),
        )

      val items = orderUpsertion.items.map(_.copy(paymentStatus = Some(PaymentStatus.Paid)))

      val onlineOrderAttribute = orderUpsertion
        .onlineOrderAttribute
        .copy(
          acceptanceStatus = AcceptanceStatus.Pending,
        )

      orderUpsertion.copy(
        paymentType = Some(OrderPaymentType.CreditCard),
        paymentTransactions = Seq(transaction),
        paymentStatus = PaymentStatus.Paid,
        items = items,
        onlineOrderAttribute = onlineOrderAttribute,
      )
    }

}
