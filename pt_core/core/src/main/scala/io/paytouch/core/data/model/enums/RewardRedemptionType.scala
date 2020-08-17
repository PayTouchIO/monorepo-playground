package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait RewardRedemptionType extends EnumEntrySnake

case object RewardRedemptionType extends Enum[RewardRedemptionType] {

  case object OrderDiscount extends RewardRedemptionType
  case object OrderItem extends RewardRedemptionType

  val values = findValues
}
