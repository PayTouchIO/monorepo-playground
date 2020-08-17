package io.paytouch.core.barcodes.entities.enum

import enumeratum._

sealed trait BarcodeFormat extends EnumEntry

case object BarcodeFormat extends Enum[BarcodeFormat] {

  case object PDF417 extends BarcodeFormat
  case object Code128 extends BarcodeFormat

  val values = findValues
}
