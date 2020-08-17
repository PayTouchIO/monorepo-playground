package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.entities.{ CustomerTotals, MerchantContext, MonetaryAmount }

trait CustomerLocationConversions extends GlobalCustomerConversions {

  protected def toCustomerLocation(data: (UUID, BigDecimal, Int))(implicit merchant: MerchantContext) =
    data match {
      case (customerId, totalSpend, totalVisits) =>
        CustomerTotals(customerId, MonetaryAmount(totalSpend, merchant), totalVisits)
    }
}
