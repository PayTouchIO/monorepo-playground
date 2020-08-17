package io.paytouch.core.entities.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait TicketStatus extends EnumEntrySnake {
  def isNotNew: Boolean =
    this != TicketStatus.New

  def isCompleted: Boolean =
    this == TicketStatus.Completed

  def isNewOrInProgress: Boolean =
    TicketStatus.isNewOrInProgress(this)
}

case object TicketStatus extends Enum[TicketStatus] {
  case object New extends TicketStatus
  case object InProgress extends TicketStatus
  case object Completed extends TicketStatus
  case object Canceled extends TicketStatus

  val values = findValues

  val isNewOrInProgress: Set[TicketStatus] =
    Set(
      New,
      InProgress,
    )
}
