package io.paytouch.core.conversions

import io.paytouch.core.data.model.OrderTaxRateRecord
import io.paytouch.core.entities.{ UserContext, OrderTaxRate => OrderTaxRateEntity }

trait OrderTaxRateConversions extends EntityConversion[OrderTaxRateRecord, OrderTaxRateEntity] {
  def fromRecordToEntity(record: OrderTaxRateRecord)(implicit user: UserContext): OrderTaxRateEntity =
    OrderTaxRateEntity(
      id = record.id,
      taxRateId = record.taxRateId,
      name = record.name,
      value = record.value,
      totalAmount = record.totalAmount,
    )
}
