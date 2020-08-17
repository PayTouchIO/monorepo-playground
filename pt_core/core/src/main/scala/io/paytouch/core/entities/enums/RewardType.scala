package io.paytouch.core.entities.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait RewardType extends EnumEntrySnake

case object RewardType extends Enum[RewardType] {

  case object FreeProduct extends RewardType
  case object GiftCard extends RewardType
  case object DiscountPercentage extends RewardType
  case object DiscountFixedAmount extends RewardType

  val values = findValues
}
