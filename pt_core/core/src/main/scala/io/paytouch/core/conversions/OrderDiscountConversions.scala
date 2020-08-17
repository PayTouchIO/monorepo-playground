package io.paytouch.core.conversions

import io.paytouch.core.data.model.OrderDiscountRecord
import io.paytouch.core.data.model.enums.DiscountType.Percentage
import io.paytouch.core.entities.{ OrderDiscount, UserContext }

trait OrderDiscountConversions extends EntityConversion[OrderDiscountRecord, OrderDiscount] {

  def fromRecordToEntity(record: OrderDiscountRecord)(implicit user: UserContext): OrderDiscount =
    OrderDiscount(
      id = record.id,
      orderId = record.orderId,
      discountId = record.discountId,
      title = record.title,
      `type` = record.`type`,
      amount = record.amount,
      totalAmount = record.totalAmount,
      currency = if (record.`type`.hasCurrency) Some(user.currency) else None,
    )
}
