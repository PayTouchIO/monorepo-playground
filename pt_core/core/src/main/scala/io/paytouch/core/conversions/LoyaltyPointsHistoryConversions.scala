package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model.enums.LoyaltyPointsHistoryType
import io.paytouch.core.data.model._
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.LoyaltyProgramType

import scala.math.floor

trait LoyaltyPointsHistoryConversions {

  protected def toLoyaltyPointHistoryUpdate(
      loyaltyMembership: LoyaltyMembershipRecord,
      orderId: Option[UUID],
      objectId: UUID,
      `type`: LoyaltyPointsHistoryType,
      points: Int,
    ): LoyaltyPointsHistoryUpdate =
    toLoyaltyPointHistoryUpdate(loyaltyMembership.id, loyaltyMembership.merchantId, orderId, objectId, `type`, points)

  def toLoyaltyPointHistoryUpdate(
      loyaltyMembershipId: UUID,
      merchantId: UUID,
      orderId: Option[UUID],
      objectId: UUID,
      `type`: LoyaltyPointsHistoryType,
      points: Int,
    ): LoyaltyPointsHistoryUpdate =
    LoyaltyPointsHistoryUpdate(
      id = Some(UUID.randomUUID),
      merchantId = Some(merchantId),
      loyaltyMembershipId = Some(loyaltyMembershipId),
      `type` = Some(`type`),
      points = Some(points),
      orderId = orderId,
      objectId = Some(objectId),
      objectType = `type`.relatedType,
    )

  protected def pointsForSpend(loyaltyProgram: LoyaltyProgramRecord, amount: BigDecimal): Int = {
    require(loyaltyProgram.`type` == LoyaltyProgramType.Spend)

    val multiplier = amount / loyaltyProgram.spendAmountForPoints.getOrElse(1)
    floor(multiplier.toDouble).toInt * loyaltyProgram.points
  }

  protected def shouldBeUpdated(
      loyaltyMembership: LoyaltyMembershipRecord,
      loyaltyProgram: LoyaltyProgramRecord,
      orderPointsData: OrderPointsData,
    ): Boolean = {
    val minimumPurchaseAmount = loyaltyProgram.minimumPurchaseAmount.getOrElse[BigDecimal](0)
    val amountGreaterThanMinimum = orderPointsData.totalAmountForPoints >= minimumPurchaseAmount
    loyaltyMembership.isEnrolled && loyaltyProgram.active && amountGreaterThanMinimum
  }

  protected def toLoyaltyPoints(
      orders: Seq[OrderRecord],
      pointsPerOrder: Map[OrderRecord, Int],
      loyaltyMembershipsPerOrder: Map[OrderRecord, LoyaltyMembership],
      loyaltyProgram: Option[LoyaltyProgramRecord],
      paymentTransactionsPerOrder: Map[UUID, Seq[PaymentTransaction]],
      orderItemsPerOrder: Map[UUID, Seq[OrderItemRecord]],
    ) =
    orders.map { order =>
      lazy val paymentTransactions = paymentTransactionsPerOrder.getOrElse(order.id, Seq.empty)
      lazy val orderItems = orderItemsPerOrder.getOrElse(order.id, Seq.empty)
      val points = loyaltyMembershipsPerOrder.get(order) match {
        case Some(lm) if lm.enrolled => LoyaltyPoints.actual(pointsPerOrder.getOrElse(order, 0))
        case _ =>
          LoyaltyPoints.potential(
            calculatePotentialLoyaltyPoints(loyaltyProgram, order, paymentTransactions, orderItems),
          )
      }
      order -> points
    }.toMap

  private def calculatePotentialLoyaltyPoints(
      loyaltyProgram: Option[LoyaltyProgramRecord],
      order: OrderRecord,
      paymentTransactions: Seq[PaymentTransaction],
      orderItems: Seq[OrderItemRecord],
    ): Int = {
    val orderPointsData = OrderPointsData.extract(order, paymentTransactions, orderItems)
    val result = for {
      lp <- loyaltyProgram
      opd <- orderPointsData
    } yield pointsForAmount(lp, opd)
    result.getOrElse(0)
  }

  private def pointsForAmount(loyaltyProgram: LoyaltyProgramRecord, orderPointsData: OrderPointsData): Int =
    loyaltyProgram.`type` match {
      case LoyaltyProgramType.Spend     => pointsForSpend(loyaltyProgram, orderPointsData.totalAmountForPoints)
      case LoyaltyProgramType.Frequency => loyaltyProgram.points
    }

}
