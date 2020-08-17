package io.paytouch.core.services

import io.paytouch.core.conversions.OrderItemTaxRateConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.OrderItemRecord
import io.paytouch.core.entities.{ OrderItemTaxRate, UserContext }

import scala.concurrent._

class OrderItemTaxRateService(implicit val ec: ExecutionContext, val daos: Daos) extends OrderItemTaxRateConversions {

  protected val dao = daos.orderItemTaxRateDao

  def findOrderItemTaxRatesPerOrderItem(
      orderItems: Seq[OrderItemRecord],
    )(implicit
      user: UserContext,
    ): Future[Map[OrderItemRecord, Seq[OrderItemTaxRate]]] =
    dao
      .findOrderItemTaxRatesPerOrderItemIds(orderItems.map(_.id))
      .map(_.mapKeysToRecords(orderItems).transform((_, v) => toSeqEntity(v)))

}
