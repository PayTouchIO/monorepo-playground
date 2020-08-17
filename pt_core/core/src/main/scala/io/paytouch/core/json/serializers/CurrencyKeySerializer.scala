package io.paytouch.core.json.serializers

import java.util.Currency

import io.paytouch.core.utils.Formatters._
import org.json4s.CustomKeySerializer

case object CurrencyKeySerializer
    extends CustomKeySerializer[Currency](formats =>
      (
        { case x: String if isValidCurrency(x) => Currency.getInstance(x.toUpperCase) },
        {
          case c: Currency => c.getCurrencyCode.toUpperCase
        },
      ),
    )
