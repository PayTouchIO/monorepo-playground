package io.paytouch.core.data.model.enums

import enumeratum._

import io.paytouch.core.utils.EnumEntrySnake

sealed abstract class RestaurantType extends EnumEntrySnake

case object RestaurantType extends Enum[RestaurantType] {
  case object FineDining extends RestaurantType
  case object CasualDining extends RestaurantType
  case object FastCasual extends RestaurantType
  case object BarNightclub extends RestaurantType
  case object Cafe extends RestaurantType
  case object Enterprise extends RestaurantType
  case object Unassigned extends RestaurantType

  val values =
    findValues

  def Default: RestaurantType =
    Unassigned
}
