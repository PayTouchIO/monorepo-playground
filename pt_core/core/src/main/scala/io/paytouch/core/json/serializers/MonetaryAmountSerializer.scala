package io.paytouch.core.json.serializers

import java.util.Currency

import io.paytouch.core.entities.MonetaryAmount
import org.json4s.JsonAST.JObject
import org.json4s.JsonDSL._
import org.json4s.{ CustomSerializer, Extraction }

case object MonetaryAmountSerializer
    extends CustomSerializer[MonetaryAmount](implicit formats =>
      (
        {
          case obj: JObject =>
            val amount = (obj \ "amount").extract[BigDecimal]
            val currency = (obj \ "currency").extract[Currency]
            MonetaryAmount(amount, currency)
        },
        {
          case m: MonetaryAmount =>
            ("amount" -> Extraction.decompose(m.roundedAmount)) ~ ("currency" -> Extraction.decompose(m.currency))
        },
      ),
    )
