package io.paytouch.core.resources.stripe

import java.util.UUID

import io.paytouch.core.data.model.enums.PaymentProcessor
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, _ }

abstract class StripeFSpec extends FSpec {
  abstract class StripeFSpecContext extends FSpecContext with DefaultFixtures {
    override lazy val merchant = Factory.merchant(paymentProcessor = Some(PaymentProcessor.Stripe)).create
  }
}
