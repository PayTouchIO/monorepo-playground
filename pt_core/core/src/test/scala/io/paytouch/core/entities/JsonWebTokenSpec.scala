package io.paytouch.core.entities

import io.paytouch.core.utils.PaytouchSpec
import org.specs2.specification.{ Scope => SpecScope }

class JsonWebTokenSpec extends PaytouchSpec {

  abstract class JsonWebTokenSpecContext extends SpecScope

  "JsonWebToken" should {
    "create a JsonWebToken" in new JsonWebTokenSpecContext with Fixtures {
      JsonWebToken(payload, jwtSecret) ==== jwtToken
    }

    "parse a token value" in new JsonWebTokenSpecContext with Fixtures {
      jwtToken.claims ==== payload
      JsonWebToken("not-a-good-value").claims ==== Map.empty
    }

    "validate a token value" in new JsonWebTokenSpecContext with Fixtures {
      jwtToken.isValid(jwtSecret) should beTrue
      jwtToken.isValid("another-secret") should beFalse
    }
  }

  trait Fixtures {
    val payload = Map("foo" -> "bar")
    val jwtSecret = "foosecret"

    val jwtToken = JsonWebToken(payload, jwtSecret)
  }
}
