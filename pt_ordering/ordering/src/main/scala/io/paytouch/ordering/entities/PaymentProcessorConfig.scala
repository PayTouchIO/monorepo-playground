package io.paytouch.ordering.entities

import io.paytouch.ordering.data.model.{
  PaymentProcessorConfig => PaymentProcessorConfigModel,
  EkashuConfig => EkashuConfigModel,
  JetdirectConfig => JetdirectConfigModel,
  WorldpayConfig => WorldpayConfigModel,
  StripeConfig => StripeConfigModel,
  PaytouchConfig => PaytouchConfigModel,
}

//
// Entity PaymentProcessorConfig contains the public fields of the correspodning model PaymentProcessorConfig.
// These are exposed to storefront customers, so MUST only contain fields that
// are meant to be public!
//
sealed abstract class PaymentProcessorConfig extends scala.Product

final case class EkashuConfig private (sellerId: String, sellerKey: String) extends PaymentProcessorConfig

final case class JetdirectConfig private (terminalId: String, key: String) extends PaymentProcessorConfig

final case class WorldpayConfig private (
    accountId: String,
    terminalId: String,
    acceptorId: String,
  ) extends PaymentProcessorConfig

// No fields need to be exposed for Stripe. Everything is returned in PaymentProcessorData when calling carts.checkout.
final case class StripeConfig private () extends PaymentProcessorConfig
final case class PaytouchConfig private () extends PaymentProcessorConfig

sealed abstract class PaymentProcessorConfigUpsertion

final case class EkashuConfigUpsertion private (
    sellerId: String,
    sellerKey: String,
    hashKey: String,
  ) extends PaymentProcessorConfigUpsertion

final case class JetdirectConfigUpsertion private (
    merchantId: String,
    terminalId: String,
    key: String,
    securityToken: String,
  ) extends PaymentProcessorConfigUpsertion

final case class WorldpayConfigUpsertion private (
    accountId: String,
    terminalId: String,
    acceptorId: String,
    accountToken: String,
  ) extends PaymentProcessorConfigUpsertion

final case class StripeConfigUpsertion private (accountId: String, publishableKey: String)
    extends PaymentProcessorConfigUpsertion

final case class PaytouchConfigUpsertion private () extends PaymentProcessorConfigUpsertion
