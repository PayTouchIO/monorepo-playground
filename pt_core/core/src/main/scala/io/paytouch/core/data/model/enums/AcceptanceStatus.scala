package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait AcceptanceStatus extends EnumEntrySnake

case object AcceptanceStatus extends Enum[AcceptanceStatus] {
  case object Open extends AcceptanceStatus
  case object Pending extends AcceptanceStatus
  case object Accepted extends AcceptanceStatus
  case object Rejected extends AcceptanceStatus

  val values = findValues
}
