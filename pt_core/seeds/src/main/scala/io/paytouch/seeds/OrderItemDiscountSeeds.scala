package io.paytouch.seeds

import io.paytouch.core.data.model._
import io.paytouch.seeds.SeedsQuantityProvider._

import scala.concurrent._

object OrderItemDiscountSeeds extends Seeds {

  lazy val orderItemDiscountDao = daos.orderItemDiscountDao

  def load(
      orderItems: Seq[OrderItemRecord],
      discounts: Seq[DiscountRecord],
    )(implicit
      user: UserRecord,
    ): Future[Seq[OrderItemDiscountRecord]] = {

    val orderItemDiscounts = orderItems.random(ItemsWithDiscount).map { orderItem =>
      val discount = discounts.random
      OrderItemDiscountUpdate(
        id = None,
        merchantId = Some(user.merchantId),
        orderItemId = Some(orderItem.id),
        discountId = Some(discount.id),
        title = Some(discount.title),
        `type` = Some(discount.`type`),
        amount = Some(discount.amount),
        totalAmount = Some(genBigDecimal.instance),
      )
    }

    val orderItemDiscountManuals = orderItems.random(ItemsWithDiscountManual).map { orderItem =>
      OrderItemDiscountUpdate(
        id = None,
        merchantId = Some(user.merchantId),
        orderItemId = Some(orderItem.id),
        discountId = None,
        title = None,
        `type` = Some(genDiscountType.instance),
        amount = Some(genBigDecimal.instance),
        totalAmount = Some(genBigDecimal.instance),
      )
    }

    orderItemDiscountDao.bulkUpsert(orderItemDiscounts ++ orderItemDiscountManuals).extractRecords
  }
}
