package io.paytouch.ordering.clients.stripe.entities

final case class StripeErrorResponse(error: StripeError)

final case class StripeError(
    `type`: String = "Unknown",
    message: String = "Unknown",
    code: String = "Unknown",
    // Internal exception in case of parsing failure
    ex: Option[Throwable] = None,
  )
