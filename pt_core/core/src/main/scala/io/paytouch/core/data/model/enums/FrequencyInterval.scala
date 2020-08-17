package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait FrequencyInterval extends EnumEntrySnake

case object FrequencyInterval extends Enum[FrequencyInterval] {

  case object Day extends FrequencyInterval
  case object Week extends FrequencyInterval
  case object Month extends FrequencyInterval

  val values = findValues
}
