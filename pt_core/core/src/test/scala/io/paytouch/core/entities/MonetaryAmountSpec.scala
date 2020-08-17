package io.paytouch.core.entities

import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.utils.PaytouchSpec
import org.specs2.specification.{ Scope => SpecScope }

class MonetaryAmountSpec extends PaytouchSpec {

  abstract class MonetaryAmountSpecContext extends SpecScope

  "MonetaryAmount" should {

    "subtract monetary amounts" in new MonetaryAmountSpecContext {
      (10 USD) - (1.9 USD) ==== (8.1 USD)
      (13453 USD) - (2000.99 USD) ==== (11452.01 USD)
    }

    "add monetary amounts" in new MonetaryAmountSpecContext {
      (10 USD) + (1.9 USD) ==== (11.9 USD)
      (13453 USD) + (2000.99 USD) ==== (15453.99 USD)
    }

    "multiply monetary amounts" in new MonetaryAmountSpecContext {
      (10 USD) * (2 USD) ==== (20 USD)
      (1000 USD) * (-1.99 USD) ==== (-1990 USD)
    }

    "divide monetary amounts" in new MonetaryAmountSpecContext {
      (10 USD) / (2 USD) ==== (5 USD)
      (33333.33 USD) / (3 USD) ==== (11111.11 USD)
    }

    "compare monetary amounts" in new MonetaryAmountSpecContext {
      (10 USD) > (1.9 USD) should beTrue
      (2000.99 USD) > (13453 USD) should beFalse

      (10 USD) < (1.9 USD) should beFalse
      (2000.99 USD) < (13453 USD) should beTrue
    }

    "negate a monetary amount" in new MonetaryAmountSpecContext {
      -(10 USD) ==== (-10 USD)
      -(-13453 USD) ==== (13453 USD)
    }

    "reject operation between monetary amounts with different currencies" in new MonetaryAmountSpecContext {
      (10 USD) + (5 EUR) + (3 GBP) should throwA[IllegalArgumentException]
      (10 USD) - (5 GBP) - (3 USD) should throwA[IllegalArgumentException]
      (10 USD) * (5 EUR) * (3 GBP) should throwA[IllegalArgumentException]
      (10 USD) / (5 EUR) / (3 GBP) should throwA[IllegalArgumentException]
    }
  }
}
