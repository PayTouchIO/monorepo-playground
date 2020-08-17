package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait TransferOrderType extends EnumEntrySnake

case object TransferOrderType extends Enum[TransferOrderType] {

  case object Ongoing extends TransferOrderType
  case object Outgoing extends TransferOrderType

  val values = findValues
}
