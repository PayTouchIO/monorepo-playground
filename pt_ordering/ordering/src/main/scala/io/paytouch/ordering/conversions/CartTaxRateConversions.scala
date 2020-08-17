package io.paytouch.ordering.conversions

import io.paytouch.ordering.data.model.CartTaxRateRecord
import io.paytouch.ordering.entities.{ AppContext, MonetaryAmount, CartTaxRate => CartTaxRateEntity }

trait CartTaxRateConversions {

  protected def fromRecordToEntity(record: CartTaxRateRecord)(implicit app: AppContext): CartTaxRateEntity =
    CartTaxRateEntity(
      id = record.id,
      taxRateId = record.taxRateId,
      name = record.name,
      `value` = record.`value`,
      total = MonetaryAmount(record.totalAmount),
    )
}
