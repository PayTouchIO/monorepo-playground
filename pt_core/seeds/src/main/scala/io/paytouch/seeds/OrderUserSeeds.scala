package io.paytouch.seeds

import io.paytouch.core.data.model._
import io.paytouch.seeds.SeedsQuantityProvider._

import scala.concurrent._

object OrderUserSeeds extends Seeds {

  lazy val orderUserDao = daos.orderUserDao

  def load(
      orders: Seq[OrderRecord],
      users: Seq[UserRecord],
    )(implicit
      user: UserRecord,
    ): Future[Seq[OrderUserRecord]] = {

    val ordersWithUserId = orders.filter(_.userId.isDefined)
    val ordersWithExtraUsers = ordersWithUserId.random(OrdersWithExtraUsers)

    val orderUsers = ordersWithUserId.map { order =>
      OrderUserUpdate(id = None, merchantId = Some(user.merchantId), orderId = Some(order.id), userId = order.userId)
    }

    val orderExtraUsers = ordersWithExtraUsers.flatMap { order =>
      val employeeIds = users.map(_.id) diff order.userId.toSeq
      employeeIds.randomSample.map { employeeId =>
        OrderUserUpdate(
          id = None,
          merchantId = Some(user.merchantId),
          orderId = Some(order.id),
          userId = Some(employeeId),
        )
      }
    }

    orderUserDao.bulkUpsertByRelIds(orderUsers ++ orderExtraUsers).extractRecords
  }
}
