package io.paytouch.ordering.resources.jetdirect

import java.util.UUID

import cats.data.Validated.{ Invalid, Valid }
import io.paytouch.ordering.jetdirect.JetdirectParser
import io.paytouch.ordering.utils.PaytouchSpec
import org.specs2.specification.Scope

class JetdirectParserSpec extends PaytouchSpec with JetdirectParser {
  abstract class JetdirectParserContext extends Scope {
    lazy val amount: BigDecimal = 90
    lazy val orderNumber = UUID.randomUUID
    lazy val responseText = "APPROVED"
    lazy val hashCodeResult: String = "valid1234"
    lazy val card = "VS"
    lazy val tipAmount: BigDecimal = 10
    lazy val feeAmount: BigDecimal = 7

    lazy val payload = Map[String, String](
      "amount" -> amount.toString,
      "order_number" -> orderNumber.toString,
      "responseText" -> responseText,
      "jp_return_hash" -> hashCodeResult,
      "card" -> card,
      "tipAmount" -> tipAmount.toString,
      "feeAmount" -> feeAmount.toString,
    )
  }

  "it parses tipAmount" in new JetdirectParserContext {
    val result = parseJetdirectEntity(payload).toOption.get
    result.tipAmount ==== Some(tipAmount)
  }

  "it parses feeAmount" in new JetdirectParserContext {
    val result = parseJetdirectEntity(payload).toOption.get
    result.feeAmount ==== Some(feeAmount)
  }
}
