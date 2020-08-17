package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait RewardRedemptionStatus extends EnumEntrySnake

case object RewardRedemptionStatus extends Enum[RewardRedemptionStatus] {

  case object Reserved extends RewardRedemptionStatus
  case object Redeemed extends RewardRedemptionStatus
  case object Canceled extends RewardRedemptionStatus

  val values = findValues
}
