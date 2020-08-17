package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait CardType extends EnumEntrySnake

case object CardType extends Enum[CardType] {

  case object Visa extends CardType
  case object MasterCard extends CardType
  case object Maestro extends CardType
  case object Amex extends CardType
  case object Jcb extends CardType
  case object Diners extends CardType
  case object Discover extends CardType
  case object CarteBleue extends CardType
  case object CarteBlanc extends CardType
  case object Voyager extends CardType
  case object Wex extends CardType
  case object ChinaUnionPay extends CardType
  case object Style extends CardType
  case object ValueLink extends CardType
  case object Interac extends CardType
  case object Laser extends CardType
  case object Other extends CardType

  val values = findValues
}
