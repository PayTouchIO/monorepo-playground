package io.paytouch.core.data.model.enums

import enumeratum._

import io.paytouch.core.utils.EnumEntrySnake

sealed abstract class Source extends EnumEntrySnake
case object Source extends Enum[Source] {
  case object DeliveryProvider extends Source
  case object Register extends Source
  case object Storefront extends Source

  val values = findValues
}
