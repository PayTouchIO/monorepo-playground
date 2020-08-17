package io.paytouch.ordering.messages.entities

import java.util.UUID

import io.paytouch.ordering.entities.enums.{ ExposedName, PaymentProcessor }

final case class MerchantChanged(eventName: String, payload: MerchantPayload) extends PtOrderingMsg[MerchantChangedData]

object MerchantChanged {
  val eventName = "merchant_changed"

  def apply(
      merchantId: UUID,
      merchant: MerchantChangedData,
      orderingPaymentPaymentProcessorConfigUpsertion: Option[MerchantPayload.OrderingPaymentProcessorConfigUpsertion],
    ): MerchantChanged =
    MerchantChanged(eventName, MerchantPayload(merchantId, merchant, orderingPaymentPaymentProcessorConfigUpsertion))
}

final case class MerchantPayload(
    `object`: ExposedName,
    merchantId: UUID,
    data: MerchantChangedData,
    orderingPaymentPaymentProcessorConfigUpsertion: Option[MerchantPayload.OrderingPaymentProcessorConfigUpsertion],
  ) extends EntityPayloadLike[MerchantChangedData]

object MerchantPayload {
  def apply(
      merchantId: UUID,
      merchant: MerchantChangedData,
      orderingPaymentPaymentProcessorConfigUpsertion: Option[MerchantPayload.OrderingPaymentProcessorConfigUpsertion],
    ): MerchantPayload =
    MerchantPayload(ExposedName.Merchant, merchantId, merchant, orderingPaymentPaymentProcessorConfigUpsertion)

  sealed abstract class OrderingPaymentProcessorConfigUpsertion

  object OrderingPaymentProcessorConfigUpsertion {
    final case class WorldpayConfigUpsertion(
        accountId: String,
        acceptorId: String,
        accountToken: String,
        terminalId: String,
      ) extends OrderingPaymentProcessorConfigUpsertion

    final case class StripeConfigUpsertion(accountId: String, publishableKey: String)
        extends OrderingPaymentProcessorConfigUpsertion

    final case class PaytouchConfigUpsertion() extends OrderingPaymentProcessorConfigUpsertion
  }
}

// Core sends the whole merchant entity, so we can pick out what we need here.
final case class MerchantChangedData(displayName: String, paymentProcessor: PaymentProcessor)
