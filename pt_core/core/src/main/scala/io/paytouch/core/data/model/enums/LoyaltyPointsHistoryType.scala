package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed abstract class LoyaltyPointsHistoryType(val relatedType: Option[LoyaltyPointsHistoryRelatedType])
    extends EnumEntrySnake

case object LoyaltyPointsHistoryType extends Enum[LoyaltyPointsHistoryType] {

  case object SignUpBonus extends LoyaltyPointsHistoryType(None)
  case object Spend extends LoyaltyPointsHistoryType(Some(LoyaltyPointsHistoryRelatedType.Order)) // Deprecated
  case object SpendTransaction
      extends LoyaltyPointsHistoryType(Some(LoyaltyPointsHistoryRelatedType.PaymentTransaction))
  case object SpendRefund extends LoyaltyPointsHistoryType(Some(LoyaltyPointsHistoryRelatedType.PaymentTransaction))
  case object Visit extends LoyaltyPointsHistoryType(Some(LoyaltyPointsHistoryRelatedType.Order))
  case object VisitCancel extends LoyaltyPointsHistoryType(Some(LoyaltyPointsHistoryRelatedType.Order))
  case object RewardRedemption extends LoyaltyPointsHistoryType(Some(LoyaltyPointsHistoryRelatedType.RewardRedemption))
  case object RewardCancel extends LoyaltyPointsHistoryType(Some(LoyaltyPointsHistoryRelatedType.RewardRedemption))

  val values = findValues
}
