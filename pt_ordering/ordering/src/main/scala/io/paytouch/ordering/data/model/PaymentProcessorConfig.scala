package io.paytouch.ordering.data.model

import io.paytouch.ordering.entities.{
  PaymentProcessorConfig => PaymentProcessorConfigEntity,
  EkashuConfig => EkashuConfigEntity,
  JetdirectConfig => JetDirectConfigEntity,
  WorldpayConfig => WorldpayConfigEntity,
  StripeConfig => StripeConfigEntity,
  PaytouchConfig => PaytouchConfigEntity,
}

sealed abstract class PaymentProcessorConfig {
  type Entity <: PaymentProcessorConfigEntity
  def toEntity: Entity
}

final case class EkashuConfig(
    sellerId: String,
    sellerKey: String,
    hashKey: String,
  ) extends PaymentProcessorConfig {
  type Entity = EkashuConfigEntity
  def toEntity = EkashuConfigEntity(sellerId, sellerKey.take(8))
}

final case class JetdirectConfig(
    merchantId: String,
    terminalId: String,
    key: String,
    securityToken: String,
  ) extends PaymentProcessorConfig {
  type Entity = JetDirectConfigEntity
  def toEntity: JetDirectConfigEntity = JetDirectConfigEntity(terminalId, key)
}

final case class WorldpayConfig(
    accountId: String,
    terminalId: String,
    acceptorId: String,
    accountToken: String,
  ) extends PaymentProcessorConfig {
  type Entity = WorldpayConfigEntity
  def toEntity: WorldpayConfigEntity =
    WorldpayConfigEntity(accountId = accountId, terminalId = terminalId, acceptorId = acceptorId)
}

final case class StripeConfig(accountId: String, publishableKey: String) extends PaymentProcessorConfig {
  type Entity = StripeConfigEntity
  def toEntity: StripeConfigEntity =
    StripeConfigEntity()
}

final case class PaytouchConfig() extends PaymentProcessorConfig {
  type Entity = PaytouchConfigEntity

  def toEntity: PaytouchConfigEntity =
    PaytouchConfigEntity()
}
