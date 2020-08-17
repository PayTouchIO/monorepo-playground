package io.paytouch.core.clients.stripe.entities

import java.util.Currency

import io.paytouch.core.entities.ExposedEntity
import io.paytouch.core.entities.enums.ExposedName
import org.json4s.JsonAST.JObject

final case class Refund(
    id: String,
    amount: BigInt,
    currency: Currency,
    status: String,
    // Register requires the full charge object to be returned, as we don't need
    // to do anything with it, we cheat and use JObject here
    charge: JObject,
    paymentIntent: String,
  ) extends ExposedEntity {
  val classShortName = ExposedName.StripeRefund
}
