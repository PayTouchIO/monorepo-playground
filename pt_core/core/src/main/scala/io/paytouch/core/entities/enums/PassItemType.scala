package io.paytouch.core.entities.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed abstract class PassItemType extends EnumEntrySnake

case object PassItemType extends Enum[PassItemType] {

  case object GiftCard extends PassItemType
  case object LoyaltyMembership extends PassItemType

  val values = findValues
}
