package io.paytouch.core.services

import java.util.UUID

import io.paytouch.core.conversions.OrderDiscountConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.OrderDiscountUpdate
import io.paytouch.core.entities.{ OrderDiscount, UserContext }
import io.paytouch.core.validators.RecoveredOrderUpsertion

import scala.concurrent._

class OrderDiscountService(implicit val ec: ExecutionContext, val daos: Daos) extends OrderDiscountConversions {

  type Entity = OrderDiscount

  protected val dao = daos.orderDiscountDao

  def findByOrderIds(orderIds: Seq[UUID])(implicit user: UserContext): Future[Map[UUID, Seq[Entity]]] =
    dao.findByOrderIds(orderIds).map(records => toSeqEntity(records).groupBy(_.orderId))

  def convertToOrderDiscountUpdates(
      upsertion: RecoveredOrderUpsertion,
    )(implicit
      user: UserContext,
    ): Future[Seq[OrderDiscountUpdate]] =
    Future.successful {
      val orderId = upsertion.orderId
      upsertion.discounts.map { discountUpsertion =>
        OrderDiscountUpdate(
          id = discountUpsertion.id,
          merchantId = Some(user.merchantId),
          orderId = Some(orderId),
          discountId = discountUpsertion.discountId,
          title = discountUpsertion.title,
          `type` = Some(discountUpsertion.`type`),
          amount = Some(discountUpsertion.amount),
          totalAmount = discountUpsertion.totalAmount,
        )
      }
    }
}
