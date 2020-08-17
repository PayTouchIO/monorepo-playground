package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait LoyaltyPointsHistoryRelatedType extends EnumEntrySnake

case object LoyaltyPointsHistoryRelatedType extends Enum[LoyaltyPointsHistoryRelatedType] {

  case object Order extends LoyaltyPointsHistoryRelatedType
  case object PaymentTransaction extends LoyaltyPointsHistoryRelatedType
  case object RewardRedemption extends LoyaltyPointsHistoryRelatedType

  val values = findValues
}
