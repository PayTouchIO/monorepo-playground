package io.paytouch.ordering.conversions

import java.util.UUID

import cats.implicits._

import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.clients.paytouch.core.entities.enums._
import io.paytouch.ordering.data.model.{ CartRecord, EkashuConfig }
import io.paytouch.ordering.ekashu.EkashuEncodings
import io.paytouch.ordering.entities.{ Cart, PaymentProcessorData }
import io.paytouch.ordering.entities.ekashu.SuccessPayload
import io.paytouch.ordering.entities.enums.PaymentProcessor
import io.paytouch.ordering.utils.UtcTime

trait EkashuConversions extends EkashuEncodings {

  protected def fromCartRecordToPaymentProcessorData(
      cartRecord: CartRecord,
    )(implicit
      ekashuConfig: EkashuConfig,
    ): PaymentProcessorData = {
    val ekashuReference = cartRecord.id.toString
    val hashCode = calculateEkashuHashCode(ekashuReference, cartRecord.totalAmount.toString)
    PaymentProcessorData.empty.copy(reference = Some(ekashuReference), hashCodeValue = Some(hashCode))
  }

  protected def addPaymentTransactionToOrderUpsertion(payload: SuccessPayload) =
    (cart: Cart, orderUpsertion: OrderUpsertion) => {
      val transaction =
        PaymentTransactionUpsertion(
          id = UUID.randomUUID,
          `type` = TransactionType.Payment,
          paymentProcessorV2 = PaymentProcessor.Ekashu,
          paymentType = TransactionPaymentType.CreditCard,
          paymentDetails = GenericPaymentDetails(
            amount = payload.ekashuAmount,
            currency = payload.ekashuCurrency,
            transactionResult = CardTransactionResultType.Approved.some,
            transactionStatus = CardTransactionStatusType.Committed.some,
            tipAmount = cart.tip.amount,
            authCode = payload.ekashuAuthCode,
            maskPan = payload.ekashuMaskedCardNumber,
            cardHash = payload.ekashuCardHash,
            cardReference = payload.ekashuCardReference,
            cardType = payload.ekashuCardScheme,
            terminalName = Some("ekashu"),
            transactionReference = Some(payload.ekashuTransactionID),
          ),
          paidAt = UtcTime.now,
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
