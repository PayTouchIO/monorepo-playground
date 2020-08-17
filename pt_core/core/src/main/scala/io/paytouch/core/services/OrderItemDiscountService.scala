package io.paytouch.core.services

import io.paytouch.core.conversions.OrderItemDiscountConversions
import io.paytouch.core.data.daos.{ Daos, OrderItemDiscountDao }
import io.paytouch.core.data.model.{ OrderItemDiscountRecord, OrderItemDiscountUpdate }
import io.paytouch.core.entities.{ OrderItemDiscount, UserContext }
import io.paytouch.core.services.features.OrderItemRelationService
import io.paytouch.core.validators.RecoveredOrderItemUpsertion

import scala.concurrent.ExecutionContext

class OrderItemDiscountService(implicit val ec: ExecutionContext, val daos: Daos)
    extends OrderItemRelationService
       with OrderItemDiscountConversions {

  type Dao = OrderItemDiscountDao
  type Entity = OrderItemDiscount
  type Record = OrderItemDiscountRecord

  protected val dao = daos.orderItemDiscountDao

  def convertToOrderItemDiscountUpdates(
      upsertion: RecoveredOrderItemUpsertion,
    )(implicit
      user: UserContext,
    ): Seq[OrderItemDiscountUpdate] = {
    val orderItemId = upsertion.id
    upsertion.discounts.map { discountUpsertion =>
      OrderItemDiscountUpdate(
        id = discountUpsertion.id,
        merchantId = Some(user.merchantId),
        orderItemId = Some(orderItemId),
        discountId = discountUpsertion.discountId,
        title = discountUpsertion.title,
        `type` = Some(discountUpsertion.`type`),
        amount = Some(discountUpsertion.amount),
        totalAmount = discountUpsertion.totalAmount,
      )
    }
  }

}
