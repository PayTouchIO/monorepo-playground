package io.paytouch.ordering.entities

final case class PaymentProcessorData(
    reference: Option[String],
    hashCodeValue: Option[String],
    checkoutUrl: Option[String],
    transactionSetupId: Option[String],
    stripePublishableKey: Option[String],
    stripePaymentIntentSecret: Option[String],
  )

object PaymentProcessorData {
  def empty: PaymentProcessorData =
    PaymentProcessorData(
      None,
      None,
      None,
      None,
      None,
      None,
    )
}
