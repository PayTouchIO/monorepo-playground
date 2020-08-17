package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait PaySchedule extends EnumEntrySnake

case object PaySchedule extends Enum[PaySchedule] {

  case object Weekly extends PaySchedule
  case object BiWeekly extends PaySchedule
  case object Monthly extends PaySchedule

  val values = findValues
}
