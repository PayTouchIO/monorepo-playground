package io.paytouch.core.messages.entities

import java.util.UUID

import cats.data._
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging

import io.paytouch.core.entities.Merchant
import io.paytouch.core.entities.enums.ExposedName
import io.paytouch.core.data.model

final case class MerchantChanged(eventName: String, payload: MerchantPayload) extends PtOrderingMsg[Merchant]

object MerchantChanged {

  val eventName = "merchant_changed"

  def apply(merchant: Merchant, modelPaymentPaymentProcessorConfig: model.PaymentProcessorConfig): MerchantChanged =
    MerchantChanged(eventName, MerchantPayload(merchant, modelPaymentPaymentProcessorConfig))
}

final case class MerchantPayload(
    `object`: ExposedName,
    merchantId: UUID,
    data: Merchant,
    orderingPaymentPaymentProcessorConfigUpsertion: Option[MerchantPayload.OrderingPaymentProcessorConfigUpsertion],
  ) extends EntityPayloadLike[Merchant]

object MerchantPayload extends LazyLogging {
  import io.scalaland.chimney.dsl._

  def apply(merchant: Merchant, modelPaymentPaymentProcessorConfig: model.PaymentProcessorConfig): MerchantPayload =
    MerchantPayload(
      merchant.classShortName,
      merchant.id,
      merchant,
      toOrderingPaymentProcessorConfigUpsertion(modelPaymentPaymentProcessorConfig),
    )

  def toOrderingPaymentProcessorConfigUpsertion(
      modelPaymentPaymentProcessorConfig: model.PaymentProcessorConfig,
    ): Option[OrderingPaymentProcessorConfigUpsertion] =
    modelPaymentPaymentProcessorConfig match {
      case c: model.PaymentProcessorConfig.Stripe =>
        c.transformInto[OrderingPaymentProcessorConfigUpsertion.StripeConfigUpsertion].some
      case c: model.PaymentProcessorConfig.Worldpay =>
        c.transformInto[OrderingPaymentProcessorConfigUpsertion.WorldpayConfigUpsertion].some
      case c: model.PaymentProcessorConfig.Paytouch => None
      case c =>
        logger.error(s"MerchantChanged to an unsupported type: ${c.getClass.getName}")
        None
    }

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
  }
}
