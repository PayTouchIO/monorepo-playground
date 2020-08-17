package io.paytouch.core.services

import java.util.UUID

import io.paytouch.core.conversions.OrderUserConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.{ OrderUserRecord, OrderUserUpdate }
import io.paytouch.core.entities.{ UserContext, UserInfo }
import io.paytouch.core.validators.RecoveredOrderUpsertion

import scala.concurrent._

class OrderUserService(val userService: UserService)(implicit val ec: ExecutionContext, val daos: Daos)
    extends OrderUserConversions {

  protected val dao = daos.orderUserDao

  def findAllUsersByOrderIds(orderIds: Seq[UUID]): Future[Map[UUID, Seq[UserInfo]]] =
    for {
      orderUsers <- dao.findByOrderIds(orderIds)
      userInfo <- userService.getUserInfoByIds(orderUsers.map(_.userId).distinct)
    } yield groupUserInfoByOrderId(orderUsers, userInfo)

  private def groupUserInfoByOrderId(orderUsers: Seq[OrderUserRecord], userInfos: Seq[UserInfo]) =
    orderUsers
      .groupBy(_.orderId)
      .transform((_, v) => userInfos.filter(u => v.map(_.userId).contains(u.id)))

  def recoverAssignedOrderUserUpdates(
      upsertion: RecoveredOrderUpsertion,
    )(implicit
      user: UserContext,
    ): Future[Option[Seq[OrderUserUpdate]]] =
    Future.successful {
      val orderId = upsertion.orderId
      upsertion.assignedUserIds.map { assignedUserIds =>
        assignedUserIds.map(assignedUserId => toUpdate(orderId, assignedUserId))
      }
    }

  def recoverCreatorOrderUserUpdates(
      upsertion: RecoveredOrderUpsertion,
    )(implicit
      user: UserContext,
    ): Future[Option[OrderUserUpdate]] =
    Future.successful {
      val orderId = upsertion.orderId
      upsertion.creatorUserId.map(userId => toUpdate(orderId, userId))
    }

}
