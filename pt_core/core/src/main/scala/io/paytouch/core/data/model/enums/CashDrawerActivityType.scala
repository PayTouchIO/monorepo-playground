package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait CashDrawerActivityType extends EnumEntrySnake

case object CashDrawerActivityType extends Enum[CashDrawerActivityType] {

  case object Create extends CashDrawerActivityType
  case object Sale extends CashDrawerActivityType
  case object Refund extends CashDrawerActivityType
  case object PayIn extends CashDrawerActivityType
  case object PayOut extends CashDrawerActivityType
  case object TipIn extends CashDrawerActivityType
  case object TipOut extends CashDrawerActivityType
  case object NoSale extends CashDrawerActivityType
  case object ValuesOverride extends CashDrawerActivityType
  case object StartCash extends CashDrawerActivityType
  case object EndCash extends CashDrawerActivityType

  val values = findValues
}
