package io.paytouch.ordering.clients.stripe.entities

import java.util.{ Currency, UUID }
import enumeratum._

import io.paytouch.ordering.entities.enums.EnumEntrySnake
import io.paytouch.ordering.clients.paytouch.core.entities.enums.CardType

final case class PaymentIntentCharges(data: Seq[Charge])

sealed trait PaymentIntentStatus extends EnumEntrySnake

case object PaymentIntentStatus extends Enum[PaymentIntentStatus] {
  case object RequiresPaymentMethod extends PaymentIntentStatus
  case object RequiresConfirmation extends PaymentIntentStatus
  case object RequiresAction extends PaymentIntentStatus
  case object Processing extends PaymentIntentStatus
  case object RequiresCapture extends PaymentIntentStatus
  case object Canceled extends PaymentIntentStatus
  case object Succeeded extends PaymentIntentStatus

  val values = findValues
}

final case class PaymentIntent(
    id: String,
    amount: BigInt, // amount in cents
    currency: Currency,
    charges: PaymentIntentCharges,
    status: PaymentIntentStatus,
    clientSecret: Option[String] = None,
    metadata: Map[String, String],
  ) extends StripeEntity {

  // Payment intents can only have one charge, but Stripe returns it as an
  // array. It is an option as when the payment intent is created there is no
  // charge, but after receiving webhooks to say a payment has been done the
  // charge will be present.
  lazy val charge: Option[Charge] = charges.data.headOption

  lazy val cartId: Option[UUID] = metadata.get("cartId").map(UUID.fromString(_))
  lazy val orderId: Option[UUID] = metadata.get("orderId").map(UUID.fromString(_))
}
