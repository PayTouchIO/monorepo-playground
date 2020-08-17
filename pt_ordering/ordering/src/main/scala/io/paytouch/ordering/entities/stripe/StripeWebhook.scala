package io.paytouch.ordering.entities.stripe

import org.json4s.JsonAST.JObject

final case class WebhookData(`object`: JObject)

final case class StripeWebhook(
    id: String,
    `type`: String,
    livemode: Livemode,
    data: WebhookData,
  )
