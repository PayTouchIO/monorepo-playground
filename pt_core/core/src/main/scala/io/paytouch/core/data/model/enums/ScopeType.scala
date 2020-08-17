package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait ScopeType extends EnumEntrySnake

case object ScopeType extends Enum[ScopeType] {

  case object Merchant extends ScopeType
  case object Location extends ScopeType
  case object LocationDaily extends ScopeType

  val values = findValues
}
