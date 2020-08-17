package io.paytouch.core.conversions

import io.paytouch.core.data.model.OrderItemDiscountRecord
import io.paytouch.core.data.model.enums.DiscountType.Percentage
import io.paytouch.core.entities.{ OrderItemDiscount, UserContext }

trait OrderItemDiscountConversions extends EntityConversion[OrderItemDiscountRecord, OrderItemDiscount] {

  def fromRecordToEntity(record: OrderItemDiscountRecord)(implicit user: UserContext): OrderItemDiscount =
    OrderItemDiscount(
      id = record.id,
      orderItemId = record.orderItemId,
      discountId = record.discountId,
      title = record.title,
      `type` = record.`type`,
      amount = record.amount,
      totalAmount = record.totalAmount,
      currency = if (record.`type`.hasCurrency) Some(user.currency) else None,
    )
}
