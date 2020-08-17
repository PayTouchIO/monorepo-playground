package io.paytouch.core.entities.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait CustomerSource extends EnumEntrySnake

case object CustomerSource extends Enum[CustomerSource] {

  case object PtRegister extends CustomerSource
  case object PtDashboard extends CustomerSource
  case object PtStorefront extends CustomerSource
  case object UberEats extends CustomerSource
  case object DoorDash extends CustomerSource
  case object Postmates extends CustomerSource

  val values = findValues

  lazy val hidden: Set[CustomerSource] = Set(UberEats)
  lazy val visible: Set[CustomerSource] = values.filterNot(hidden).toSet
}

sealed abstract class CustomerSourceAlias(val targets: Set[CustomerSource]) extends EnumEntrySnake

case object CustomerSourceAlias extends Enum[CustomerSourceAlias] {

  case object All extends CustomerSourceAlias(targets = CustomerSource.values.toSet)
  case object Visible extends CustomerSourceAlias(targets = CustomerSource.visible)
  case object Hidden extends CustomerSourceAlias(targets = CustomerSource.hidden)

  case object PtRegister extends CustomerSourceAlias(targets = Set(CustomerSource.PtRegister))
  case object PtDashboard extends CustomerSourceAlias(targets = Set(CustomerSource.PtDashboard))
  case object PtStorefront extends CustomerSourceAlias(targets = Set(CustomerSource.PtStorefront))
  case object UberEats extends CustomerSourceAlias(targets = Set(CustomerSource.UberEats))
  case object DoorDash extends CustomerSourceAlias(targets = Set(CustomerSource.DoorDash))
  case object Postmates extends CustomerSourceAlias(targets = Set(CustomerSource.Postmates))

  val values = findValues
}
