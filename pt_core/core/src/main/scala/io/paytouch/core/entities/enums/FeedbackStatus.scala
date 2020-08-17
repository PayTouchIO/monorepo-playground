package io.paytouch.core.entities.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait FeedbackStatus extends EnumEntrySnake

case object FeedbackStatus extends Enum[FeedbackStatus] {

  case object Read extends FeedbackStatus
  case object Unread extends FeedbackStatus

  val values = findValues
}
