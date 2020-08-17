package io.paytouch.ordering.resources.ekashu

import io.paytouch.ordering.clients.paytouch.core.entities.enums.CardType
import io.paytouch.ordering.clients.paytouch.core.entities.enums.CardType._
import io.paytouch.ordering.conversions.EkashuConversions
import io.paytouch.ordering.utils.PaytouchSpec

class EkashuCardSchemeParserSpec extends PaytouchSpec {

  val valuesExpectationMap = Map(
    "AMEX" -> Amex,
    "MASTERCARD" -> MasterCard,
    "VISA" -> Visa,
  )
  valuesExpectationMap.foreach {
    case (input, expectation) =>
      "cardSchemeParsing" should {
        s"return the expected value for $input" in {
          CardType.withEkashuName(input) ==== expectation
        }
      }
  }
}
