package io.paytouch.core.services

import io.paytouch.core.conversions.OrderBundleConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.{ OrderBundleUpdate, OrderRecord }
import io.paytouch.core.entities.{ OrderBundle, UserContext }
import io.paytouch.core.validators.RecoveredOrderUpsertion

import scala.concurrent._

class OrderBundleService(implicit val ec: ExecutionContext, val daos: Daos) extends OrderBundleConversions {

  val dao = daos.orderBundleDao

  def recoverOrderBundleUpdates(
      upsertion: RecoveredOrderUpsertion,
    )(implicit
      user: UserContext,
    ): Future[Seq[OrderBundleUpdate]] =
    Future.successful {
      val orderId = upsertion.orderId
      upsertion.bundles.map { bundleUpsertion =>
        OrderBundleUpdate(
          id = Some(bundleUpsertion.id),
          merchantId = Some(user.merchantId),
          orderId = Some(orderId),
          bundleOrderItemId = Some(bundleUpsertion.bundleOrderItemId),
          orderBundleSets = Some(bundleUpsertion.orderBundleSets),
        )
      }
    }

  def findAllByOrders(orders: Seq[OrderRecord]): Future[Map[OrderRecord, Seq[OrderBundle]]] =
    dao
      .findByOrderIds(orders.map(_.id))
      .map(
        _.groupBy(_.orderId).transform((_, v) => v.map(fromRecordToEntity)).mapKeysToRecords(orders),
      )
}
