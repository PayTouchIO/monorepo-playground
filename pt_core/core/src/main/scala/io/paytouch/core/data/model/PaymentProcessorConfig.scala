package io.paytouch.core.data.model

import io.paytouch._
import io.paytouch.core._
import io.paytouch.core.data.model.enums.PaymentProcessor

sealed abstract class PaymentProcessorConfig(val paymentProcessor: PaymentProcessor) extends SerializableProduct

object PaymentProcessorConfig {
  type Creditcall = CreditcallConfig
  lazy val Creditcall = CreditcallConfig

  type Jetpay = JetpayConfig
  lazy val Jetpay = JetpayConfig

  type Paytouch = PaytouchConfig
  lazy val Paytouch = PaytouchConfig

  type Stripe = StripeConfig
  lazy val Stripe = StripeConfig

  type Worldpay = WorldpayConfig
  lazy val Worldpay = WorldpayConfig

  // Renaming these breaks Json4s
  final case class CreditcallConfig(terminalId: String, transactionKey: String)
      extends PaymentProcessorConfig(PaymentProcessor.Creditcall)

  final case class JetpayConfig(merchantId: String, refundStandardAdjustmentFee: Boolean)
      extends PaymentProcessorConfig(PaymentProcessor.Jetpay)

  final case class PaytouchConfig() extends PaymentProcessorConfig(PaymentProcessor.Paytouch)

  final case class StripeConfig(
      accessToken: String,
      refreshToken: String,
      liveMode: Boolean,
      accountId: String,
      publishableKey: String,
    ) extends PaymentProcessorConfig(PaymentProcessor.Stripe) {
    def isEmpty: Boolean =
      this == Stripe.empty

    def nonEmpty: Boolean =
      !isEmpty
  }

  object StripeConfig {
    val empty: Stripe =
      Stripe(
        accessToken = "",
        refreshToken = "",
        liveMode = false,
        accountId = "",
        publishableKey = "",
      )
  }

  final case class WorldpayConfig(
      accountId: String,
      acceptorId: String,
      accountToken: String,
      terminalId: String,
    ) extends PaymentProcessorConfig(PaymentProcessor.Worldpay)
}
