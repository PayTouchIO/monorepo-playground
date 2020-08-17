package io.paytouch.core.entities.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake
import io.paytouch.core.utils.RichString._

sealed trait ContextSource extends EnumEntrySnake

case object ContextSource extends Enum[ContextSource] {

  case object PtDashboard extends ContextSource
  case object PtRegister extends ContextSource
  case object PtTickets extends ContextSource
  case object PtAdmin extends ContextSource
  case object PtOrdering extends ContextSource
  case object PtDelivery extends ContextSource

  val values = findValues
}
