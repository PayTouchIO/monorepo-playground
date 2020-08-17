package io.paytouch.core.entities.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait LoginSource extends EnumEntrySnake {
  def toContextSource: ContextSource =
    this match {
      case LoginSource.PtDashboard => ContextSource.PtDashboard
      case LoginSource.PtRegister  => ContextSource.PtRegister
      case LoginSource.PtTickets   => ContextSource.PtTickets
      case LoginSource.PtAdmin     => ContextSource.PtAdmin
    }
}

case object LoginSource extends Enum[LoginSource] {

  case object PtDashboard extends LoginSource
  case object PtRegister extends LoginSource
  case object PtTickets extends LoginSource
  case object PtAdmin extends LoginSource

  val values = findValues
}
