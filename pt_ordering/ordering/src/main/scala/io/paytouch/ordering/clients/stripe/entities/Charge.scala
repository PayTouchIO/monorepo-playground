package io.paytouch.ordering.clients.stripe.entities

import java.util.Currency
import io.paytouch.ordering.entities.MonetaryAmount
import io.paytouch.ordering.clients.paytouch.core.entities.enums.CardType
import io.paytouch.ordering.clients.paytouch.core.entities.enums.CardTransactionResultType

final case class Charge(
    id: String, // GenericPaymentDetails transactionReference
    amount: BigInt, // amount in cents GenericPaymentDetails amount
    currency: Currency, // GenericPaymentDetails currency
    billingDetails: BillingDetails,
    outcome: ChargeOutcome,
    paymentMethodDetails: PaymentMethodDetails,
  ) extends StripeEntity {
  lazy val total: MonetaryAmount = MonetaryAmount.fromCents(amount, currency)
}

final case class ChargeOutcome(
    `type`: String, // GenericPaymentDetails transactionResult
  ) {
  lazy val transactionResult: CardTransactionResultType = `type` match {
    case "authorized"    => CardTransactionResultType.Approved
    case "manual_review" => CardTransactionResultType.PartialApproval
    case _               => CardTransactionResultType.Declined
  }
}

final case class BillingDetails(
    name: Option[String], // GenericPaymentDetails cardHolderName
  )

final case class PaymentMethodDetails(card: Option[CardPaymentMethodDetails])

final case class CardPaymentMethodDetails(
    brand: String, // GenericPaymentDetails cardType
    last4: String, // GenericPaymentDetailss last4Digits
    fingerprint: String, // GenericPaymentDetails cardHash
  ) {
  lazy val cardType: Option[CardType] = CardType.withStripeNameOption(brand)
}
