package io.paytouch.ordering.clients.paytouch.core.entities.enums

import enumeratum._

import io.paytouch.ordering.entities.enums.EnumEntrySnake

sealed abstract class AcceptanceStatus extends EnumEntrySnake

case object AcceptanceStatus extends Enum[AcceptanceStatus] {
  case object Open extends AcceptanceStatus
  case object Pending extends AcceptanceStatus

  val values = findValues
}
