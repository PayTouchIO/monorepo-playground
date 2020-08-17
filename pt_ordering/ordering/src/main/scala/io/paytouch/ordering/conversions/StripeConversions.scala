package io.paytouch.ordering.conversions

import java.util.{ Currency, UUID }

import scala.concurrent.Future

import akka.http.scaladsl.model.Uri

import cats.implicits._

import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.clients.paytouch.core.entities.enums._
import io.paytouch.ordering.clients.stripe.entities._
import io.paytouch.ordering.clients.stripe.StripeClientConfig
import io.paytouch.ordering.data.model._
import io.paytouch.ordering.entities.{ Cart, PaymentProcessorData }
import io.paytouch.ordering.entities.enums.{ CartStatus, PaymentProcessor }
import io.paytouch.ordering.utils.UtcTime

trait StripeConversions {
  val config: StripeClientConfig

  final protected def toPaymentProcessorData(
      merchantConfig: StripeConfig,
      paymentIntent: PaymentIntent,
    ): PaymentProcessorData =
    // The clientSecret is a token the Stripe Javascript client uses to process
    // the payment. It's secret in that it shouldn't be stored or published, not
    // secret as in we should not expose it to the customer.
    PaymentProcessorData
      .empty
      .copy(
        stripePublishableKey = Some(merchantConfig.publishableKey),
        stripePaymentIntentSecret = Some(paymentIntent.clientSecret.get),
      )

  final protected def paymentTransactionUpsertion(
      paymentIntent: PaymentIntent,
      tipAmount: BigDecimal,
    ): PaymentTransactionUpsertion = {
    val charge = paymentIntent.charge.get
    val paymentMethodDetails = charge.paymentMethodDetails.card.get

    PaymentTransactionUpsertion(
      id = UUID.randomUUID,
      `type` = TransactionType.Payment,
      paymentProcessorV2 = PaymentProcessor.Stripe,
      paymentType = TransactionPaymentType.CreditCard,
      paidAt = UtcTime.now,
      paymentDetails = GenericPaymentDetails(
        amount = charge.total.amount,
        tipAmount = tipAmount,
        currency = charge.total.currency,
        transactionResult = charge.outcome.transactionResult.some,
        transactionStatus = CardTransactionStatusType.Committed.some,
        cardType = paymentMethodDetails.cardType,
        cardHash = Some(paymentMethodDetails.fingerprint),
        last4Digits = Some(paymentMethodDetails.last4),
        cardHolderName = charge.billingDetails.name,
        transactionReference = Some(charge.id),
        gatewayTransactionReference = Some(paymentIntent.id),
        entryMode = "Manual".some,
      ),
    )
  }

  // All payment processors do something like this, we should refactor it out
  // into common code, and add more functionality such as:
  //
  //   - Support for failed payment transactions (currently we don't sync those to core for any processor)
  //   - Partial payments
  //
  final protected def addPaymentTransactionToOrderUpsertion(
      paymentIntent: PaymentIntent,
    )(
      cart: Cart,
      orderUpsertion: OrderUpsertion,
    ): OrderUpsertion = {
    val charge = paymentIntent.charge.get
    charge.outcome.transactionResult match {
      case CardTransactionResultType.Approved =>
        addPaidPaymentTransactionToOrderUpsertion(paymentIntent, charge, cart, orderUpsertion)
      case _ =>
        addUnpaidPaymentTransactionToOrderUpsertion(paymentIntent, charge, cart, orderUpsertion)
    }
  }

  private def addPaidPaymentTransactionToOrderUpsertion(
      paymentIntent: PaymentIntent,
      charge: Charge,
      cart: Cart,
      orderUpsertion: OrderUpsertion,
    ): OrderUpsertion =
    orderUpsertion.copy(
      paymentType = Some(OrderPaymentType.CreditCard),
      paymentStatus = PaymentStatus.Paid,
      items = orderUpsertion.items.map(_.copy(paymentStatus = Some(PaymentStatus.Paid))),
      onlineOrderAttribute = orderUpsertion.onlineOrderAttribute.copy(acceptanceStatus = AcceptanceStatus.Pending),
      paymentTransactions = Seq(paymentTransactionUpsertion(paymentIntent, cart.tip.amount)),
    )

  private def addUnpaidPaymentTransactionToOrderUpsertion(
      paymentIntent: PaymentIntent,
      charge: Charge,
      cart: Cart,
      orderUpsertion: OrderUpsertion,
    ): OrderUpsertion = {
    // There is no guarantee on the order of webhooks, so we could receive a
    // declined webhook after the cart/order has been marked as paid. We use the
    // cart status to generate the correct order upsertion payment status. This
    // won't be needed when we move storefront to use the
    // orders.store_payment_transaction endpoint.
    val paymentStatus = cart.status match {
      case CartStatus.New  => PaymentStatus.Pending
      case CartStatus.Paid => PaymentStatus.Paid
    }

    val acceptanceStatus = cart.status match {
      case CartStatus.New  => AcceptanceStatus.Open
      case CartStatus.Paid => AcceptanceStatus.Pending
    }

    orderUpsertion.copy(
      paymentType = Some(OrderPaymentType.CreditCard),
      paymentStatus = paymentStatus,
      items = orderUpsertion.items.map(_.copy(paymentStatus = Some(paymentStatus))),
      onlineOrderAttribute = orderUpsertion.onlineOrderAttribute.copy(acceptanceStatus = acceptanceStatus),
      paymentTransactions = Seq(paymentTransactionUpsertion(paymentIntent, cart.tip.amount)),
    )
  }
}
