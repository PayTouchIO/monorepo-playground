package io.paytouch.ordering.conversions

import java.util.{ Currency, UUID }

import scala.concurrent.Future

import akka.http.scaladsl.model.Uri

import cats.implicits._

import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.clients.paytouch.core.entities.enums._
import io.paytouch.ordering.data.model.{
  WorldpayPaymentRecord,
  WorldpayPaymentUpdate => WorldpayPaymentUpdateModel,
  CartRecord,
  PaymentIntentRecord,
}
import io.paytouch.ordering.clients.worldpay.entities._
import io.paytouch.ordering.clients.worldpay.WorldpayCheckoutUri
import io.paytouch.ordering.data.model.WorldpayPaymentType
import io.paytouch.ordering.entities.{ Cart, PaymentProcessorData }
import io.paytouch.ordering.entities.enums.PaymentProcessor
import io.paytouch.ordering.entities.worldpay._
import io.paytouch.ordering.utils.UtcTime
import io.paytouch.ordering.withTag

trait WorldpayConversions {
  def checkoutUri: Uri withTag WorldpayCheckoutUri

  final protected def cartToUpsertion(
      cart: CartRecord,
      successReturnUrl: String,
      failureReturnUrl: String,
      response: TransactionSetupResponse,
    ): WorldpayPaymentUpdateModel =
    WorldpayPaymentUpdateModel(
      id = None,
      objectId = Some(cart.id),
      objectType = Some(WorldpayPaymentType.Cart),
      transactionSetupId = Some(response.transactionSetupId),
      successReturnUrl = Some(successReturnUrl),
      failureReturnUrl = Some(failureReturnUrl),
      status = Some(WorldpayPaymentStatus.Submitted),
    )

  protected def paymentIntentToUpsertion(
      paymentIntent: PaymentIntentRecord,
      response: TransactionSetupResponse,
    ): WorldpayPaymentUpdateModel =
    WorldpayPaymentUpdateModel(
      id = None,
      objectId = Some(paymentIntent.id),
      objectType = Some(WorldpayPaymentType.PaymentIntent),
      transactionSetupId = Some(response.transactionSetupId),
      successReturnUrl = Some(paymentIntent.successReturnUrl),
      failureReturnUrl = Some(paymentIntent.failureReturnUrl),
      status = Some(WorldpayPaymentStatus.Submitted),
    )

  protected def recordToPaymentProcessorData(record: WorldpayPaymentRecord): PaymentProcessorData =
    PaymentProcessorData
      .empty
      .copy(
        checkoutUrl = Some(checkoutUrl(record)),
        transactionSetupId = Some(record.transactionSetupId),
      )

  final protected def checkoutUrl(record: WorldpayPaymentRecord): String =
    s"${checkoutUri}?TransactionSetupId=${record.transactionSetupId}"

  final protected def paymentTransactionUpsertion(response: TransactionQueryResponse, tipAmount: BigDecimal) =
    PaymentTransactionUpsertion(
      id = UUID.randomUUID,
      `type` = TransactionType.Payment,
      paymentProcessorV2 = PaymentProcessor.Worldpay,
      paymentType = TransactionPaymentType.CreditCard,
      paidAt = UtcTime.now,
      paymentDetails = GenericPaymentDetails(
        accountId = Some(response.accountId),
        applicationId = Some(response.applicationId),
        authCode = Some(response.approvalNumber),
        amount = response.approvedAmount,
        tipAmount = tipAmount,
        // Worldpay only supports payments in USD
        currency = Currency.getInstance("USD"),
        transactionResult = CardTransactionResultType.Approved.some,
        transactionStatus = CardTransactionStatusType.Committed.some,
        maskPan = Some(response.maskedCardNumber),
        cardType = Some(response.cardType),
        cardHolderName = Some(response.cardHolderName),
        terminalId = Some(response.terminalId),
        transactionReference = Some(response.transactionId),
        gatewayTransactionReference = Some(response.transactionSetupId),
        entryMode = "Manual".some,
        transactionStatusInfo = response.hostResponseCode.some,
      ),
    )

  final protected def addPaymentTransactionToOrderUpsertion(
      response: TransactionQueryResponse,
    )(
      cart: Cart,
      orderUpsertion: OrderUpsertion,
    ): OrderUpsertion =
    orderUpsertion.copy(
      paymentType = Some(OrderPaymentType.CreditCard),
      paymentStatus = PaymentStatus.Paid,
      items = orderUpsertion.items.map(_.copy(paymentStatus = Some(PaymentStatus.Paid))),
      onlineOrderAttribute = orderUpsertion.onlineOrderAttribute.copy(acceptanceStatus = AcceptanceStatus.Pending),
      paymentTransactions = Seq(paymentTransactionUpsertion(response, cart.tip.amount)),
    )
}
