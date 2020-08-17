package io.paytouch.core.conversions

import io.paytouch.core.data.model.{ OrderItemTaxRateRecord, OrderItemTaxRateUpdate }
import io.paytouch.core.entities.{ UserContext, OrderItemTaxRate => OrderItemTaxRateEntity }
import io.paytouch.core.validators.RecoveredOrderItemUpsertion

trait OrderItemTaxRateConversions extends EntityConversion[OrderItemTaxRateRecord, OrderItemTaxRateEntity] {
  def fromRecordToEntity(record: OrderItemTaxRateRecord)(implicit user: UserContext): OrderItemTaxRateEntity =
    OrderItemTaxRateEntity(
      id = record.id,
      taxRateId = record.taxRateId,
      name = record.name,
      value = record.value,
      totalAmount = record.totalAmount,
      applyToPrice = record.applyToPrice,
      active = record.active,
    )

  def convertToOrderItemTaxRateUpdates(
      upsertion: RecoveredOrderItemUpsertion,
    )(implicit
      user: UserContext,
    ): Seq[OrderItemTaxRateUpdate] =
    upsertion.taxRates.map { taxRateUpsertion =>
      OrderItemTaxRateUpdate(
        id = Some(taxRateUpsertion.id),
        merchantId = Some(user.merchantId),
        orderItemId = Some(upsertion.id),
        taxRateId = taxRateUpsertion.taxRateId,
        name = Some(taxRateUpsertion.name),
        value = Some(taxRateUpsertion.value),
        totalAmount = taxRateUpsertion.totalAmount,
        applyToPrice = Some(taxRateUpsertion.applyToPrice),
        active = Some(taxRateUpsertion.active),
      )
    }
}
