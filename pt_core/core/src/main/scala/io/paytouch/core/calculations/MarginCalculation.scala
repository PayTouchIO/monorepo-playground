package io.paytouch.core.calculations

import scala.math.BigDecimal.RoundingMode
import scala.util.Try

trait MarginCalculation {

  def computeMargin(priceAmount: BigDecimal, costAmount: BigDecimal): BigDecimal =
    Try(((1 - (costAmount / priceAmount)) * 100).setScale(2, RoundingMode.HALF_UP)) getOrElse 0
}
