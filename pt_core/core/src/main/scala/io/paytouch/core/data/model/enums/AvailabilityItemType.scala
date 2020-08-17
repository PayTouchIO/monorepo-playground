package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait AvailabilityItemType extends EnumEntrySnake

case object AvailabilityItemType extends Enum[AvailabilityItemType] {
  case object Catalog extends AvailabilityItemType
  case object Category extends AvailabilityItemType
  case object CategoryLocation extends AvailabilityItemType
  case object Discount extends AvailabilityItemType
  case object Location extends AvailabilityItemType

  val values = findValues
}
