package io.paytouch.core.calculations

import io.paytouch.core.entities.{ MonetaryAmount, UserContext }

object CalculationUtils {

  implicit class RichMonetaryAmount(val ms: Seq[MonetaryAmount]) extends AnyVal {
    def sumNonZero(implicit user: UserContext) =
      ms.filterNot(_.amount == BigDecimal(0)).fold(MonetaryAmount(0, user.currency))(_ + _)
  }

}

trait Calculations extends OrderItemsCalculations with ProductStocksCalculations
