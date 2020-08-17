package io.paytouch.ordering.clients.paytouch.core.entities.enums

import enumeratum._
import io.paytouch.ordering.entities.enums.EnumEntrySnake

sealed abstract class SetupType extends EnumEntrySnake

case object SetupType extends Enum[SetupType] {
  case object Dash extends SetupType
  case object Paytouch extends SetupType

  val values = findValues
}
