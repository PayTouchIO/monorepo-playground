package io.paytouch.core.json.serializers

import java.util.Currency

import io.paytouch.core.utils.Formatters._
import org.json4s.CustomSerializer
import org.json4s.JsonAST.JString

case object CurrencySerializer
    extends CustomSerializer[Currency](formats =>
      (
        {
          case JString(x) if isValidCurrency(x) => Currency.getInstance(x.toUpperCase)
        },
        {
          case currency: Currency => JString(currency.getCurrencyCode.toUpperCase)
        },
      ),
    )
