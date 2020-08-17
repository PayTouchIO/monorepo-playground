package io.paytouch.core.data.model.enums

import enumeratum._

sealed abstract class UnitType(name: String) extends EnumEntry {
  override def entryName = name
}

case object UnitType extends Enum[UnitType] {

  case object `Unit` extends UnitType("unit")
  case object Ounce extends UnitType("oz")
  case object Pound extends UnitType("lb")
  case object Milligram extends UnitType("mg")
  case object Gram extends UnitType("g")
  case object Kilogram extends UnitType("kg")
  case object FlOunce extends UnitType("fl")
  case object Pint extends UnitType("pt")
  case object Milliliter extends UnitType("mL")
  case object Liter extends UnitType("L")
  case object Inch extends UnitType("in")
  case object Foot extends UnitType("ft")
  case object Yard extends UnitType("yd")
  case object Millimeter extends UnitType("mm")
  case object Centimeter extends UnitType("cm")
  case object Meter extends UnitType("m")

  val values = findValues
}
