package io.paytouch.core.services

import io.paytouch.core.conversions.OrderTaxRateConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.{ OrderRecord, OrderTaxRateUpdate }
import io.paytouch.core.entities.{ OrderTaxRate, UserContext }
import io.paytouch.core.validators.RecoveredOrderUpsertion

import scala.concurrent._

class OrderTaxRateService(implicit val ec: ExecutionContext, val daos: Daos) extends OrderTaxRateConversions {

  protected val dao = daos.orderTaxRateDao

  def findOrderTaxRatesPerOrder(
      orders: Seq[OrderRecord],
    )(implicit
      user: UserContext,
    ): Future[Map[OrderRecord, Seq[OrderTaxRate]]] =
    dao
      .findOrderTaxRatesPerOrderIds(orders.map(_.id))
      .map(_.mapKeysToRecords(orders).transform((_, v) => toSeqEntity(v)))

  def recoverOrderTaxRateUpdates(
      upsertion: RecoveredOrderUpsertion,
    )(implicit
      user: UserContext,
    ): Future[Seq[OrderTaxRateUpdate]] =
    Future.successful {
      upsertion.taxRates.map { taxRateUpsertion =>
        OrderTaxRateUpdate(
          id = None,
          merchantId = Some(user.merchantId),
          orderId = Some(upsertion.orderId),
          taxRateId = taxRateUpsertion.taxRateId,
          name = Some(taxRateUpsertion.name),
          value = Some(taxRateUpsertion.value),
          totalAmount = Some(taxRateUpsertion.totalAmount),
        )
      }
    }

}
