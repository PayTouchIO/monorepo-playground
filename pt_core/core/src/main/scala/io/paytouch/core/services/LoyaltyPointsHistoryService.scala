package io.paytouch.core.services

import java.util.UUID

import io.paytouch.core.RichMap
import io.paytouch.core.conversions.LoyaltyPointsHistoryConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.enums._
import io.paytouch.core.data.model._
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.LoyaltyProgramType

import scala.concurrent._

class LoyaltyPointsHistoryService(
    loyaltyMembershipService: => LoyaltyMembershipService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends LoyaltyPointsHistoryConversions {

  protected val dao = daos.loyaltyPointsHistoryDao
  val loyaltyProgramDao = daos.loyaltyProgramDao
  val loyaltyProgramLocationDao = daos.loyaltyProgramLocationDao

  def convertToUpdates(
      loyaltyMembership: LoyaltyMembershipRecord,
      loyaltyProgram: LoyaltyProgramRecord,
      orderPointsData: OrderPointsData,
    ): Future[Seq[LoyaltyPointsHistoryUpdate]] =
    Future.successful {
      if (shouldBeUpdated(loyaltyMembership, loyaltyProgram, orderPointsData)) {
        val updates = loyaltyProgram.`type` match {
          case LoyaltyProgramType.Spend => convertToUpdatesForSpend(loyaltyMembership, loyaltyProgram, orderPointsData)
          case LoyaltyProgramType.Frequency =>
            convertToUpdatesForFrequency(loyaltyMembership, loyaltyProgram, orderPointsData)
        }
        updates.filterNot(_.points.contains(0))
      }
      else Seq.empty
    }

  private def convertToUpdatesForSpend(
      loyaltyMembership: LoyaltyMembershipRecord,
      loyaltyProgram: LoyaltyProgramRecord,
      orderPointsData: OrderPointsData,
    ): Seq[LoyaltyPointsHistoryUpdate] =
    orderPointsData.paymentTransactions.flatMap { pt =>
      pt.`type` match {
        case TransactionType.Payment =>
          Some(
            toLoyaltyPointHistoryUpdate(
              loyaltyMembership,
              orderId = Some(orderPointsData.id),
              objectId = pt.id,
              `type` = LoyaltyPointsHistoryType.SpendTransaction,
              points = pointsForSpend(loyaltyProgram, orderPointsData.eligibleAmountForTransaction(pt)),
            ),
          )
        case tt if tt.isNull =>
          Some(
            toLoyaltyPointHistoryUpdate(
              loyaltyMembership,
              orderId = Some(orderPointsData.id),
              objectId = pt.id,
              `type` = LoyaltyPointsHistoryType.SpendRefund,
              points = -pointsForSpend(loyaltyProgram, orderPointsData.eligibleAmountForTransaction(pt)),
            ),
          )
        case _ => None
      }
    }

  private def convertToUpdatesForFrequency(
      loyaltyMembership: LoyaltyMembershipRecord,
      loyaltyProgram: LoyaltyProgramRecord,
      orderPointsData: OrderPointsData,
    ): Seq[LoyaltyPointsHistoryUpdate] =
    orderPointsData.paymentStatus match {
      case PaymentStatus.Paid =>
        val visitUpdate = toLoyaltyPointHistoryUpdate(
          loyaltyMembership,
          orderId = Some(orderPointsData.id),
          objectId = orderPointsData.id,
          `type` = LoyaltyPointsHistoryType.Visit,
          points = loyaltyProgram.points,
        )
        Seq(visitUpdate)
      case pt if pt.isReturn =>
        val visitUpdate = toLoyaltyPointHistoryUpdate(
          loyaltyMembership,
          orderId = Some(orderPointsData.id),
          objectId = orderPointsData.id,
          `type` = LoyaltyPointsHistoryType.Visit,
          points = loyaltyProgram.points,
        )
        val visitCancelUpdate = toLoyaltyPointHistoryUpdate(
          loyaltyMembership,
          orderId = Some(orderPointsData.id),
          objectId = orderPointsData.id,
          `type` = LoyaltyPointsHistoryType.VisitCancel,
          points = -loyaltyProgram.points,
        )
        Seq(visitUpdate, visitCancelUpdate)
      case _ => Seq.empty
    }

  def findByOrders(
      orders: Seq[OrderRecord],
      transactionsPerOrder: Map[UUID, Seq[PaymentTransaction]],
      orderItemsPerOrder: Map[UUID, Seq[OrderItemRecord]],
    )(implicit
      user: UserContext,
    ): Future[Map[OrderRecord, LoyaltyPoints]] =
    for {
      loyaltyProgram <- loyaltyProgramDao.findOneActiveLoyaltyProgram(user.merchantId, None)
      pointsPerOrder <- dao.getPointsBalanceByOrderIds(orders.map(_.id)).map(_.mapKeysToRecords(orders))
      loyaltyMembershipsPerOrder <- getLoyaltyMembershipsByOrders(orders, loyaltyProgram)
    } yield toLoyaltyPoints(
      orders,
      pointsPerOrder,
      loyaltyMembershipsPerOrder,
      loyaltyProgram,
      transactionsPerOrder,
      orderItemsPerOrder,
    )

  private def getLoyaltyMembershipsByOrders(
      orders: Seq[OrderRecord],
      loyaltyProgram: Option[LoyaltyProgramRecord],
    )(implicit
      user: UserContext,
    ): Future[Map[OrderRecord, LoyaltyMembership]] = {
    val customerIds = orders.flatMap(_.customerId)
    val filter = loyaltyMembershipService.defaultFilter.copy(loyaltyProgramId = loyaltyProgram.map(_.id))
    loyaltyMembershipService.findAllByCustomerIds(customerIds, filter).map { loyaltyMemberships =>
      orders
        .filter(_.customerId.isDefined)
        .flatMap { order =>
          val customerId = order.customerId.get
          val loyaltyMembership = loyaltyMemberships.find(_.customerId == customerId)
          loyaltyMembership.map(order -> _)
        }
        .toMap
    }
  }
}
