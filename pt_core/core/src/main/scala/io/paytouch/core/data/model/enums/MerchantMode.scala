package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait MerchantMode extends EnumEntrySnake

case object MerchantMode extends Enum[MerchantMode] {

  case object Demo extends MerchantMode
  case object Production extends MerchantMode

  val values = findValues
}
