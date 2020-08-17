package io.paytouch.ordering.resources.jetdirect

import io.paytouch.ordering.clients.paytouch.core.entities.enums.CardType
import io.paytouch.ordering.clients.paytouch.core.entities.enums.CardType._
import io.paytouch.ordering.conversions.EkashuConversions
import io.paytouch.ordering.utils.PaytouchSpec

class JetdirectCardSchemeParserSpec extends PaytouchSpec {

  val valuesExpectationMap = Map(
    "VS" -> Visa,
    "MC" -> MasterCard,
  )
  valuesExpectationMap.foreach {
    case (input, expectation) =>
      "cardSchemeParsing" should {
        s"return the expected value for $input" in {
          CardType.withJetdirectName(input) ==== expectation
        }
      }
  }
}
