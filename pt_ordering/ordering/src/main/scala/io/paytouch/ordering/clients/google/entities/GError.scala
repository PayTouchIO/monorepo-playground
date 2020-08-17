package io.paytouch.ordering.clients.google.entities

final case class GError(
    errorMessage: String = "Unknown",
    status: String = "Unknown",
    ex: Option[Throwable] = None,
  )
