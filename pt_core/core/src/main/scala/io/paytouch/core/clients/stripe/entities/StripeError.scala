package io.paytouch.core.clients.stripe.entities

final case class StripeError(
    `type`: String = "Unknown",
    message: String = "Unknown",
    code: String = "Unknown",
    declineCode: Option[String] = None,
    // Internal exception in case of parsing failure
    ex: Option[Throwable] = None,
  )
