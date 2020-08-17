package io.paytouch.ordering.calculations

import scala.math.BigDecimal.RoundingMode

import io.paytouch.ordering.entities.MonetaryAmount

object RoundingUtils {
  implicit class RichOptBigDecimal(val optBd: Option[BigDecimal]) extends AnyVal {
    def asRounded: Option[BigDecimal] =
      optBd.map(_.asRounded)

    def nonNegative: Option[BigDecimal] =
      optBd.map(_.nonNegative)
  }

  implicit class RichBigDecimal(val bd: BigDecimal) extends AnyVal {
    def asRounded: BigDecimal =
      bd.setScale(
        scale = 2,
        mode = RoundingMode.HALF_UP,
      )

    def nonNegative: BigDecimal =
      bd.max(0)
  }

  implicit class RichMonetaryAmount(val m: MonetaryAmount) extends AnyVal {
    def asRounded: MonetaryAmount =
      m.copy(amount = m.amount.asRounded)
  }
}
