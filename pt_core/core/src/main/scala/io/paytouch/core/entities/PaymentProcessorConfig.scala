package io.paytouch.core.entities

import io.scalaland.chimney.dsl._

import io.paytouch._
import io.paytouch.core.data._

sealed abstract class PaymentProcessorConfig extends SerializableProduct
object PaymentProcessorConfig extends Function1[model.PaymentProcessorConfig, PaymentProcessorConfig] {
  // the pattern match wouldn't have been necessary if our Json4s layer was less "hacky"
  override implicit def apply(paymentProcessorConfig: model.PaymentProcessorConfig): PaymentProcessorConfig =
    paymentProcessorConfig match {
      case c: model.PaymentProcessorConfig.Creditcall => c.transformInto[Creditcall]
      case c: model.PaymentProcessorConfig.Jetpay     => c.transformInto[Jetpay]
      case c: model.PaymentProcessorConfig.Paytouch   => c.transformInto[Paytouch]
      case c: model.PaymentProcessorConfig.Stripe     => c.transformInto[Stripe]
      case c: model.PaymentProcessorConfig.Worldpay   => c.transformInto[Worldpay]
    }

  type Creditcall = CreditcallConfigEntity
  lazy val Creditcall = CreditcallConfigEntity

  type Jetpay = JetpayConfigEntity
  lazy val Jetpay = JetpayConfigEntity

  type Paytouch = PaytouchConfigEntity
  lazy val Paytouch = PaytouchConfigEntity

  type Stripe = StripeConfigEntity
  lazy val Stripe = StripeConfigEntity

  type Worldpay = WorldpayConfigEntity
  lazy val Worldpay = WorldpayConfigEntity

  // Renaming these breaks Json4s
  final case class CreditcallConfigEntity(terminalId: String, transactionKey: String) extends PaymentProcessorConfig

  final case class JetpayConfigEntity(merchantId: String, refundStandardAdjustmentFee: Boolean)
      extends PaymentProcessorConfig

  final case class StripeConfigEntity(accountId: String, publishableKey: String) extends PaymentProcessorConfig

  final case class PaytouchConfigEntity() extends PaymentProcessorConfig

  final case class WorldpayConfigEntity(
      accountId: String,
      acceptorId: String,
      terminalId: String,
    ) extends PaymentProcessorConfig
}

final case class WorldpayConfigUpsertion(
    accountId: String,
    acceptorId: String,
    accountToken: String,
    terminalId: String,
  ) {
  def toPaymentProcessorConfig: model.PaymentProcessorConfig.Worldpay =
    this.transformInto[model.PaymentProcessorConfig.Worldpay]
}
